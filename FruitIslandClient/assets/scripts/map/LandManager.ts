import { _decorator, Color, Component, Graphics, Label, Node, Vec3 } from 'cc';
import { DataManager } from '../data/DataManager';
import { Api } from '../network/Api';
import { http } from '../network/HttpClient';
import { CropConfig, LandVO } from '../types';
import { FarmBlockConfig } from './MapLoader';
import { GridManager } from './GridManager';

const { ccclass } = _decorator;

interface LandCellView {
  land: LandVO;
  label: Label;
}

/** 种子选择面板需要显示的可种植品种。 */
export interface PlantableCropOption {
  cropId: string;
  name: string;
  rarity: string;
  cropLevel: number;
  temporary: boolean;
  validUntil?: string;
}

/**
 * 土地管理器 — Demo2.5
 *
 * 将服务端 LandVO 的 Block 内坐标映射到世界地图中的农田 Block。
 * 当前交互采用“一次点击执行当前动作”，先完成核心玩法闭环：
 *   UNPURCHASED → 购买
 *   EMPTY → 种植草莓
 *   PLANTED(未浇水) → 浇水
 *   PLANTED(生长中) → 查看倒计时
 *   READY → 收获
 */
@ccclass('LandManager')
export class LandManager extends Component {
  private _gridManager: GridManager | null = null;
  private _farmByBlockId: Map<string, FarmBlockConfig> = new Map();
  private _cellViews: Map<number, LandCellView> = new Map();
  private _messageHandler: ((message: string) => void) | null = null;
  private _cropPickerHandler:
    ((options: PlantableCropOption[], onSelect: (cropId: string) => void) => void)
    | null = null;
  private _busy: boolean = false;
  private _countdownAccumulator: number = 0;

  init(gridManager: GridManager, farms: FarmBlockConfig[]): void {
    this._gridManager = gridManager;
    this._farmByBlockId.clear();

    for (const farm of farms) {
      this._farmByBlockId.set(this.toServerBlockId(farm.id), farm);
    }

    this.refresh();
  }

  setMessageHandler(handler: (message: string) => void): void {
    this._messageHandler = handler;
  }

  /** 由 UIManager 注入种子选择面板，避免土地层直接创建屏幕 UI。 */
  setCropPickerHandler(
    handler: (
      options: PlantableCropOption[],
      onSelect: (cropId: string) => void,
    ) => void,
  ): void {
    this._cropPickerHandler = handler;
  }

  /** 使用 DataManager 当前数据重新绘制全部农田格。 */
  refresh(): void {
    for (const child of [...this.node.children]) {
      child.destroy();
    }
    this._cellViews.clear();

    const data = DataManager.getInstance();
    if (!data.isLoaded || !this._gridManager) {
      this.showMessage('离线地图模式：连接服务器后可操作土地');
      return;
    }

    const farmLands = data.lands.filter(land => land.areaType === 'FARM');
    for (const land of farmLands) {
      const farm = this._farmByBlockId.get(land.blockId);
      if (!farm) continue;
      this.createLandCell(land, farm);
    }

    console.log(`[LandManager] 已渲染 ${this._cellViews.size} 块农田`);
    if (this._cellViews.size !== 64) {
      console.warn(
        `[LandManager] 预期 4 个 4×4 农田区域共 64 块，当前服务端返回 ${this._cellViews.size} 块`,
      );
    }
  }

  /**
   * 尝试处理一次世界地图点击。
   * 返回 true 表示点击命中了土地，GameManager 不应再让玩家移动。
   */
  tryInteractAt(worldPos: Vec3): boolean {
    const land = this.findLandAt(worldPos.x, worldPos.y);
    if (!land) return false;

    if (this._busy) {
      this.showMessage('操作处理中，请稍候');
      return true;
    }

    void this.interact(land);
    return true;
  }

  update(dt: number): void {
    this._countdownAccumulator += dt;
    if (this._countdownAccumulator < 1) return;
    this._countdownAccumulator = 0;

    for (const view of this._cellViews.values()) {
      view.label.string = this.getLandLabel(view.land);
    }
  }

  private createLandCell(land: LandVO, farm: FarmBlockConfig): void {
    const gm = this._gridManager!;
    const gs = gm.config.gridSize;
    const gx = farm.gx + land.gridX;
    const gy = farm.gy + land.gridY;
    const center = gm.gridToWorldCenter(gx, gy);

    const cellNode = new Node(`Land_${land.landConfigId}`);
    cellNode.setPosition(center);

    const graphics = cellNode.addComponent(Graphics);
    const color = this.getLandColor(land);
    graphics.fillColor = color;
    graphics.rect(-gs * 0.45, -gs * 0.45, gs * 0.9, gs * 0.9);
    graphics.fill();
    graphics.strokeColor = new Color(255, 255, 255, 150);
    graphics.lineWidth = 2;
    graphics.rect(-gs * 0.45, -gs * 0.45, gs * 0.9, gs * 0.9);
    graphics.stroke();

    const labelNode = new Node('StateLabel');
    labelNode.setPosition(0, 0, 1);
    cellNode.addChild(labelNode);
    const label = labelNode.addComponent(Label);
    label.fontSize = 22;
    label.lineHeight = 25;
    label.color = new Color(255, 255, 255, 255);
    label.string = this.getLandLabel(land);

    this.node.addChild(cellNode);
    this._cellViews.set(land.landConfigId, { land, label });
  }

  private findLandAt(worldX: number, worldY: number): LandVO | null {
    if (!this._gridManager) return null;
    const gs = this._gridManager.config.gridSize;

    for (const view of this._cellViews.values()) {
      const farm = this._farmByBlockId.get(view.land.blockId);
      if (!farm) continue;
      /*
       * 使用世界坐标包围盒而不是整数 Grid 相等判断。
       * 这样 FarmConfig 可使用 35.5 这类半格位置，同时土地内部坐标仍是 0~3。
       */
      const left = (farm.gx + view.land.gridX) * gs;
      const bottom = (farm.gy + view.land.gridY) * gs;
      if (worldX >= left && worldX < left + gs &&
          worldY >= bottom && worldY < bottom + gs) {
        return view.land;
      }
    }
    return null;
  }

  private async interact(land: LandVO, selectedCropId?: string): Promise<void> {
    const state = this.getEffectiveState(land);

    if (state === 'LOCKED') {
      this.showMessage(`需要 Lv.${land.unlockLevel} 才能解锁`);
      return;
    }

    if (state === 'PLANTED' && (land.waterLevel ?? 0) > 0) {
      this.showMessage(`作物生长中，还剩 ${this.getRemainingSeconds(land)} 秒`);
      return;
    }

    /*
     * 空地先展示玩家真正拥有的品种，选择后再提交种植。
     * 永久品种和有效期内的限时稀有品种都会出现在面板中。
     */
    if (state === 'EMPTY' && !selectedCropId) {
      const options = this.getPlantableCrops();
      if (options.length === 0) {
        this.showMessage('还没有可用的永久或限时种植权限');
        return;
      }
      if (this._cropPickerHandler) {
        this._cropPickerHandler(options, cropId => {
          void this.interact(land, cropId);
        });
        this.showMessage('请选择要种植的品种');
        return;
      }
      // 场景未绑定选择 UI 时仍可使用第一个品种，保证核心流程不阻塞。
      return this.interact(land, options[0].cropId);
    }

    this._busy = true;
    try {
      let result;
      if (state === 'UNPURCHASED') {
        result = await http.post(Api.LAND_BUY, {
          landConfigId: land.landConfigId,
        });
      } else if (state === 'EMPTY') {
        if (!selectedCropId) throw new Error('请选择要种植的品种');
        result = await http.post(Api.PLANT, {
          playerLandId: land.playerLandId,
          cropId: selectedCropId,
        });
      } else if (state === 'PLANTED') {
        result = await http.post(Api.WATER, {
          playerLandId: land.playerLandId,
        });
      } else if (state === 'READY') {
        result = await http.post(Api.HARVEST, {
          playerLandId: land.playerLandId,
        });
      } else {
        return;
      }

      if (result.code !== 0) {
        this.showMessage(result.message || '土地操作失败');
        return;
      }

      const actionMessage: Record<string, string> = {
        UNPURCHASED: '土地购买成功',
        EMPTY: '种子已种下，请再点击一次浇水 💧',
        PLANTED: '浇水成功，作物开始发芽 🌱',
        READY: '收获成功，产物已进入背包',
      };
      const successMessage = actionMessage[state] || '操作成功';

      const loaded = await DataManager.getInstance().loadGameData();
      if (loaded) {
        this.refresh();
        this.showMessage(successMessage);
      } else {
        this.showMessage('操作成功，但刷新游戏数据失败');
      }
    } finally {
      this._busy = false;
    }
  }

  private getEffectiveState(land: LandVO): string {
    if (land.status === 'PLANTED' &&
        land.finishTime &&
        this.getRemainingSeconds(land) <= 0) {
      return 'READY';
    }
    return land.status;
  }

  private getLandColor(land: LandVO): Color {
    const state = this.getEffectiveState(land);
    if (state === 'LOCKED') return new Color(75, 80, 85, 210);
    if (state === 'UNPURCHASED') return new Color(145, 105, 65, 220);
    if (state === 'EMPTY') return new Color(125, 85, 45, 230);
    if (state === 'READY') return new Color(245, 150, 65, 240);
    if ((land.waterLevel ?? 0) <= 0) return new Color(100, 120, 150, 230);
    return new Color(95, 175, 80, 230);
  }

  private getLandLabel(land: LandVO): string {
    const state = this.getEffectiveState(land);
    if (state === 'LOCKED') return `🔒\nLv.${land.unlockLevel}`;
    if (state === 'UNPURCHASED') return `💰\n${land.buyPrice}`;
    if (state === 'EMPTY') return '＋';
    if (state === 'READY') {
      return `${this.getCropEmoji(land.cropId)}\n×${land.yieldCount ?? 1}`;
    }
    if ((land.waterLevel ?? 0) <= 0) return '💧\n浇水';
    return `🌱\n${this.getRemainingSeconds(land)}s`;
  }

  private getRemainingSeconds(land: LandVO): number {
    if (!land.finishTime) return 0;
    const remainingMs = new Date(land.finishTime).getTime() - Date.now();
    return Math.max(0, Math.ceil(remainingMs / 1000));
  }

  /**
   * 合并永久种植权和当前有效的限时稀有种植权。
   *
   * 同一品种同时存在永久和限时权限时只显示永久权限；种植不消耗数量。
   */
  private getPlantableCrops(): PlantableCropOption[] {
    const data = DataManager.getInstance();
    const now = Date.now();
    const configs = new Map(
      data.cropConfigs
        .filter(config =>
          config.enabled === 1 &&
          config.playerUnlockLevel <= data.player.level)
        .map(config => [config.cropId, config]),
    );
    const options = new Map<string, PlantableCropOption>();

    for (const owned of data.playerCrops) {
      const config = configs.get(owned.cropId);
      if (!config) continue;
      options.set(owned.cropId, {
        cropId: owned.cropId,
        name: config.name,
        rarity: config.rarity,
        cropLevel: owned.cropLevel,
        temporary: false,
      });
    }

    for (const grant of data.cropGrants) {
      const config = configs.get(grant.cropId);
      if (!config || options.has(grant.cropId)) continue;
      const validFrom = new Date(grant.validFrom).getTime();
      const validUntil = new Date(grant.validUntil).getTime();
      if (grant.status !== 'ACTIVE' || validFrom > now || validUntil <= now) continue;
      options.set(grant.cropId, {
        cropId: grant.cropId,
        name: config.name,
        rarity: config.rarity,
        cropLevel: grant.grantCropLevel,
        temporary: true,
        validUntil: grant.validUntil,
      });
    }

    return [...options.values()].sort((a, b) => {
      if (a.temporary !== b.temporary) return a.temporary ? 1 : -1;
      return a.cropId.localeCompare(b.cropId);
    });
  }

  private getCropConfig(cropId?: string): CropConfig | undefined {
    if (!cropId) return undefined;
    return DataManager.getInstance().cropConfigs
      .find(config => config.cropId === cropId);
  }

  private getCropEmoji(cropId?: string): string {
    const icons: Record<string, string> = {
      strawberry: '🍓',
      cabbage: '🥬',
      carrot: '🥕',
      tomato: '🍅',
      potato: '🥔',
      chili: '🌶️',
      corn: '🌽',
      moonberry: '🫐',
    };
    return cropId ? icons[cropId] ?? '🌿' : '🌿';
  }

  private toServerBlockId(farmId: string): string {
    const suffix = farmId.replace('farm_', '').toUpperCase();
    return `Farm-${suffix}`;
  }

  private showMessage(message: string): void {
    console.log(`[LandManager] ${message}`);
    this._messageHandler?.(message);
  }
}
