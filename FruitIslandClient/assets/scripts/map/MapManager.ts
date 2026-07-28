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
  private _worldRoot: Node | null = null;
  private _buildingManager: BuildingManager | null = null;
  private _collisionManager: MapCollisionManager | null = null;
  private _initialized: boolean = false;

  /** 农场方块节点（供外部查询） */
  private _farmNodes: Map<string, Node> = new Map();

  // ==================== 初始化 ====================

  init(worldRoot: Node = this.node): void {
    if (this._initialized) return;
    this._initialized = true;
    this._worldRoot = worldRoot;
    console.log('[MapManager] Demo2.4 初始化开始');

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

    console.log('[MapManager] Demo2.4 初始化完成 ✅');
  }

  // ==================== 地形 ====================

  private createTerrain(): void {
    const terrainLayer = this.getOrCreateLayer('TerrainLayer');

    const gm = this._gridManager.config;
    const waterEdge = MapConfig.WATER_EDGE;
    const beachWidth = MapConfig.BEACH_WIDTH;
    const g = terrainLayer.getComponent(Graphics) || terrainLayer.addComponent(Graphics);
    g.clear();

    for (let gx = 0; gx < gm.width; gx++) {
      for (let gy = 0; gy < gm.height; gy++) {
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
          const shade = ((gx * 17 + gy * 31) % 25) - 12;
          g.fillColor = new Color(110 + shade, 160 + shade, 70 + Math.floor(shade / 2));
        }

        const x = gx * gm.gridSize;
        const y = gy * gm.gridSize;
        g.rect(x, y, gm.gridSize, gm.gridSize);
        g.fill();

        g.strokeColor = new Color(0, 0, 0, 15);
        g.lineWidth = 0.5;
        g.rect(x, y, gm.gridSize, gm.gridSize);
        g.stroke();
      }
    }

    console.log(`[MapManager] 地形: ${gm.width}×${gm.height}，使用单 Graphics 节点绘制`);
  }

  // ==================== 建筑 ====================

  private createBuildings(buildings: BuildingConfig[]): void {
    const buildingLayer = this.getOrCreateLayer('BuildingLayer');

    this._buildingManager = buildingLayer.getComponent(BuildingManager)
      || buildingLayer.addComponent(BuildingManager);
    const manager = this._buildingManager;
    manager.init(this._gridManager);
    manager.createBuildings(buildings);
  }

  // ==================== 农田 ====================

  private createFarms(farms: FarmBlockConfig[]): void {
    const landLayer = this.getOrCreateLayer('LandLayer');

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
    const root = this._worldRoot || this.node;
    const colNode = root.getChildByName('CollisionManager')
      || (() => { const n = new Node('CollisionManager'); root.addChild(n); return n; })();

    const collisionManager = colNode.getComponent(MapCollisionManager)
      || colNode.addComponent(MapCollisionManager);
    this._collisionManager = collisionManager;

    if (this._buildingManager) {
      collisionManager.init(this._gridManager, this._buildingManager);
      collisionManager.setTerrain(MapConfig.WATER_EDGE, MapConfig.BEACH_WIDTH);
    }
  }

  private getOrCreateLayer(name: string): Node {
    const root = this._worldRoot || this.node;
    let layer = root.getChildByName(name);
    if (!layer) {
      layer = new Node(name);
      root.addChild(layer);
    }
    return layer;
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
