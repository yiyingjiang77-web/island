import { _decorator, Component, Node, Graphics, Color, Vec3 } from 'cc';
import { GridManager } from './GridManager';
import { MapLoader, BuildingConfig, FarmBlockConfig } from './MapLoader';
import { BuildingManager } from './BuildingManager';
import { MapCollisionManager } from './MapCollisionManager';
import { MapConfig } from '../../configs/MapConfig';

const { ccclass } = _decorator;

/**
 * 地图总调度器 — Demo2.1
 *
 * 流程：
 *   MapLoader.loadConfigs() → GridManager.init() → 地形 → 建筑 → 农田 → 碰撞
 *
 * 场景层级：
 *   WorldRoot/TerrainLayer  — 地形
 *   WorldRoot/LandLayer     — 农田
 *   WorldRoot/BuildingLayer — 建筑
 */
@ccclass('MapManager')
export class MapManager extends Component {
  private _gridManager: GridManager = new GridManager();
  private _loader: MapLoader = new MapLoader();
  private _buildingManager: BuildingManager | null = null;
  private _collisionManager: MapCollisionManager | null = null;

  /** 农场方块节点（供外部查询） */
  private _farmNodes: Map<string, Node> = new Map();

  // ==================== 初始化 ====================

  onLoad(): void {
    console.log('[MapManager] Demo2.1 初始化开始');
    this.init();
  }

  init(): void {
    // 1. 加载配置
    const { mapConfig, farmConfig } = this._loader.loadConfigs();

    // 2. 初始化网格系统
    this._gridManager.init(mapConfig.map);

    // 3. 创建地形
    this.createTerrain();

    // 4. 创建建筑
    this.createBuildings(mapConfig.buildings);

    // 5. 创建农田
    this.createFarms(farmConfig.farms);

    // 6. 初始化碰撞系统
    this.initCollision();

    console.log('[MapManager] Demo2.1 初始化完成 ✅');
  }

  // ==================== 地形 ====================

  private createTerrain(): void {
    const terrainLayer = this.node.getChildByName('TerrainLayer');
    if (!terrainLayer) {
      console.warn('[MapManager] TerrainLayer 不存在，跳过地形');
      return;
    }

    const gm = this._gridManager.config;
    const waterEdge = 1, beachWidth = 2;

    for (let gx = 0; gx < gm.width; gx++) {
      for (let gy = 0; gy < gm.height; gy++) {
        const tileNode = new Node(`Tile_${gx}_${gy}`);
        const pos = this._gridManager.gridToWorldCenter(gx, gy);
        tileNode.setPosition(pos);

        const g = tileNode.addComponent(Graphics);

        // 根据位置判断地形
        const isWater =
          gx < waterEdge || gx >= gm.width - waterEdge ||
          gy < waterEdge || gy >= gm.height - waterEdge;
        const grassStart = waterEdge + beachWidth;
        const grassEnd = gm.width - waterEdge - beachWidth - 1;
        const isBeach = !isWater &&
          (gx < grassStart || gx > grassEnd || gy < grassStart || gy > grassEnd);

        if (isWater) {
          g.fillColor = new Color(100, 160, 220);
        } else if (isBeach) {
          g.fillColor = new Color(240, 220, 180);
        } else {
          const shade = Math.floor(Math.random() * 30) - 15;
          g.fillColor = new Color(110 + shade, 160 + shade, 70 + Math.floor(shade / 2));
        }

        g.rect(-gm.gridSize / 2, -gm.gridSize / 2, gm.gridSize, gm.gridSize);
        g.fill();

        g.strokeColor = new Color(0, 0, 0, 15);
        g.lineWidth = 0.5;
        g.rect(-gm.gridSize / 2, -gm.gridSize / 2, gm.gridSize, gm.gridSize);
        g.stroke();

        terrainLayer.addChild(tileNode);
      }
    }

    console.log(`[MapManager] 地形: ${gm.width * gm.height} 块 (${gm.width}×${gm.height})`);
  }

  // ==================== 建筑 ====================

  private createBuildings(buildings: BuildingConfig[]): void {
    let buildingLayer = this.node.getChildByName('BuildingLayer');
    if (!buildingLayer) {
      buildingLayer = new Node('BuildingLayer');
      this.node.addChild(buildingLayer);
    }

    this._buildingManager = buildingLayer.getComponent(BuildingManager)
      || buildingLayer.addComponent(BuildingManager);
    this._buildingManager.init(this._gridManager);
    this._buildingManager.createBuildings(buildings);
  }

  // ==================== 农田 ====================

  private createFarms(farms: FarmBlockConfig[]): void {
    let landLayer = this.node.getChildByName('LandLayer');
    if (!landLayer) {
      landLayer = new Node('LandLayer');
      this.node.addChild(landLayer);
    }

    for (const farm of farms) {
      const farmNode = new Node(`Farm_${farm.id}`);
      const pos = this._gridManager.gridToWorldCenter(
        farm.gx + farm.width / 2 - 0.5,
        farm.gy + farm.height / 2 - 0.5,
      );
      farmNode.setPosition(pos);

      const gs = this._gridManager.config.gridSize;
      const g = farmNode.addComponent(Graphics);

      // 半透明绿色填充
      g.fillColor = new Color(126, 200, 80, 100);
      const hw = (farm.width * gs) / 2;
      const hh = (farm.height * gs) / 2;
      g.rect(-hw, -hh, farm.width * gs, farm.height * gs);
      g.fill();

      // 虚线边框
      g.strokeColor = new Color(180, 220, 140, 200);
      g.lineWidth = 2;
      g.rect(-hw, -hh, farm.width * gs, farm.height * gs);
      g.stroke();

      landLayer.addChild(farmNode);
      this._farmNodes.set(farm.id, farmNode);
    }

    console.log(`[MapManager] 农田: ${farms.length} 块`);
  }

  // ==================== 碰撞 ====================

  private initCollision(): void {
    const colNode = this.node.getChildByName('CollisionManager')
      || (() => { const n = new Node('CollisionManager'); this.node.addChild(n); return n; })();

    this._collisionManager = colNode.getComponent(MapCollisionManager)
      || colNode.addComponent(MapCollisionManager);

    if (this._buildingManager) {
      this._collisionManager.init(this._gridManager, this._buildingManager);
    }
  }

  // ==================== 公共接口 ====================

  get gridManager(): GridManager { return this._gridManager; }
  get buildingManager(): BuildingManager | null { return this._buildingManager; }
  get collisionManager(): MapCollisionManager | null { return this._collisionManager; }

  /** 判断是否可行走 */
  isWalkable(worldX: number, worldY: number): boolean {
    return this._collisionManager ? this._collisionManager.isWalkable(worldX, worldY) : true;
  }

  /** 修正到可行走位置 */
  clampToWalkable(worldX: number, worldY: number): Vec3 {
    return this._collisionManager
      ? this._collisionManager.clampToWalkable(worldX, worldY)
      : new Vec3(worldX, worldY, 0);
  }
}
