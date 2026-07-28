import { _decorator, Component } from 'cc';
import { GridManager, MapGridConfig } from './GridManager';

const { ccclass } = _decorator;

/**
 * 建筑配置（对应 MapConfig.json 中的 buildings[]）
 */
export interface BuildingConfig {
  id: string;
  name: string;
  type: string;
  gx: number;
  gy: number;
  width: number;
  height: number;
  prefab: string;
  unlockLevel: number;
}

/**
 * 地图原始配置（对应 MapConfig.json）
 */
export interface MapRawConfig {
  map: MapGridConfig;
  buildings: BuildingConfig[];
}

/**
 * 农场配置（对应 FarmConfig.json）
 */
export interface FarmBlockConfig {
  id: string;
  gx: number;
  gy: number;
  width: number;
  height: number;
  unlockLevel: number;
}

export interface FarmRawConfig {
  farms: FarmBlockConfig[];
  blockSize: number;
  defaultCropGrowSeconds: number;
}

/**
 * 地图加载器 — Demo2.1
 *
 * 职责：
 * - 加载 MapConfig.json / FarmConfig.json
 * - 将原始 JSON 数据注入 GridManager 和 MapManager
 *
 * 注意：Cocos Creator 中 resources.load 加载 JSON 需要文件在 resources/ 目录
 * Demo2.1 阶段直接用内嵌数据（后续改为异步加载）
 */
@ccclass('MapLoader')
export class MapLoader extends Component {
  private _mapConfig: MapRawConfig | null = null;
  private _farmConfig: FarmRawConfig | null = null;

  /**
   * 加载地图配置
   *
   * Cocos 版：
   *   resources.load('configs/MapConfig', JsonAsset, (err, asset) => { ... })
   *
   * Demo2.1 版：直接使用内嵌数据（与 MapConfig.json 内容一致）
   */
  loadConfigs(): { mapConfig: MapRawConfig; farmConfig: FarmRawConfig } {
    this._mapConfig = this.getMapConfigData();
    this._farmConfig = this.getFarmConfigData();
    return { mapConfig: this._mapConfig, farmConfig: this._farmConfig };
  }

  get mapConfig(): MapRawConfig {
    if (!this._mapConfig) throw new Error('MapConfig 未加载');
    return this._mapConfig;
  }

  get farmConfig(): FarmRawConfig {
    if (!this._farmConfig) throw new Error('FarmConfig 未加载');
    return this._farmConfig;
  }

  get buildings(): BuildingConfig[] {
    return this.mapConfig.buildings;
  }

  get farms(): FarmBlockConfig[] {
    return this.farmConfig.farms;
  }

  // ==================== 内嵌配置（与 JSON 文件内容同步） ====================

  private getMapConfigData(): MapRawConfig {
    return {
      map: { width: 48, height: 48, gridSize: 120, worldSize: 5760 },
      buildings: [
        { id: 'chicken_coop', name: '鸡舍', type: 'animal', gx: 35, gy: 15, width: 6, height: 5, prefab: 'ChickenCoop', unlockLevel: 1 },
        { id: 'cow_barn', name: '牛棚', type: 'animal', gx: 25, gy: 7, width: 8, height: 6, prefab: 'CowBarn', unlockLevel: 1 },
        { id: 'drink_shop', name: '饮品店', type: 'shop', gx: 18, gy: 31, width: 6, height: 5, prefab: 'DrinkShop', unlockLevel: 4 },
        { id: 'cake_shop', name: '蛋糕店', type: 'shop', gx: 15, gy: 22, width: 6, height: 5, prefab: 'CakeShop', unlockLevel: 3 },
        { id: 'exchange_shop', name: '交易所', type: 'shop', gx: 15, gy: 14, width: 6, height: 4, prefab: 'ExchangeShop', unlockLevel: 6 },
        { id: 'dock', name: '码头', type: 'dock', gx: 25, gy: 42, width: 5, height: 4, prefab: 'Dock', unlockLevel: 1 },
        { id: 'bee_house', name: '蜂箱', type: 'decoration', gx: 26, gy: 20, width: 2, height: 2, prefab: 'BeeHouse', unlockLevel: 5 },
      ],
    };
  }

  private getFarmConfigData(): FarmRawConfig {
    return {
      farms: [
        { id: 'farm_a', gx: 31, gy: 30, width: 4, height: 4, unlockLevel: 1 },
        { id: 'farm_b', gx: 36, gy: 30, width: 4, height: 4, unlockLevel: 3 },
        { id: 'farm_c', gx: 31, gy: 35, width: 4, height: 4, unlockLevel: 5 },
        { id: 'farm_d', gx: 36, gy: 35, width: 4, height: 4, unlockLevel: 8 },
      ],
      blockSize: 4,
      defaultCropGrowSeconds: 60,
    };
  }
}
