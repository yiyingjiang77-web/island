import { GridManager, MapGridConfig } from './GridManager';
import { MapConfig } from '../../configs/MapConfig';

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
 * 运行时坐标统一来自 MapConfig.ts，避免 MapManager 再维护一份坐标。
 * MapConfig.json / FarmConfig.json 保留给策划配置和校验工具使用。
 */
export class MapLoader {
  private _mapConfig: MapRawConfig | null = null;
  private _farmConfig: FarmRawConfig | null = null;

  /**
   * 加载地图配置
   *
   * 当前使用同步 TypeScript 配置，保证场景初始化不依赖异步资源加载。
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

  // ==================== 配置转换 ====================

  private getMapConfigData(): MapRawConfig {
    const buildingMeta: Record<string, {
      type: string;
      prefab: string;
      unlockLevel: number;
    }> = {
      chicken_coop: { type: 'animal', prefab: 'ChickenCoop', unlockLevel: 1 },
      cow_barn: { type: 'animal', prefab: 'CowBarn', unlockLevel: 1 },
      drink_shop: { type: 'shop', prefab: 'DrinkShop', unlockLevel: 4 },
      cake_shop: { type: 'shop', prefab: 'CakeShop', unlockLevel: 3 },
      exchange_shop: { type: 'shop', prefab: 'ExchangeShop', unlockLevel: 6 },
      dock: { type: 'dock', prefab: 'Dock', unlockLevel: 1 },
      bee_house: { type: 'decoration', prefab: 'BeeHouse', unlockLevel: 5 },
    };

    const buildings = MapConfig.ZONES
      .filter(zone => buildingMeta[zone.id] !== undefined)
      .map(zone => {
        const meta = buildingMeta[zone.id];
        return {
          id: zone.id,
          name: zone.name,
          type: meta.type,
          gx: zone.gx,
          gy: zone.gy,
          width: zone.w,
          height: zone.h,
          prefab: meta.prefab,
          unlockLevel: meta.unlockLevel,
        };
      });

    return {
      map: {
        width: MapConfig.GRID_COUNT,
        height: MapConfig.GRID_COUNT,
        gridSize: MapConfig.TILE_SIZE,
        worldSize: MapConfig.WORLD_SIZE,
      },
      buildings,
    };
  }

  private getFarmConfigData(): FarmRawConfig {
    const unlockLevels: Record<string, number> = {
      farm_a: 1,
      farm_b: 3,
      farm_c: 5,
      farm_d: 8,
    };

    const farms = MapConfig.ZONES
      .filter(zone => zone.id.startsWith('farm_'))
      .map(zone => ({
        id: zone.id,
        gx: zone.gx,
        gy: zone.gy,
        width: zone.w,
        height: zone.h,
        unlockLevel: unlockLevels[zone.id] ?? 1,
      }));

    return {
      farms,
      blockSize: 4,
      defaultCropGrowSeconds: 60,
    };
  }
}
