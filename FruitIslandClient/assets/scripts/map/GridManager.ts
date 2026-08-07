import { _decorator, Component, Node, Vec3 } from 'cc';

const { ccclass } = _decorator;

/**
 * 网格管理器 — Demo2.1
 *
 * 统一的世界坐标系统：
 * - 所有位置以 Grid 坐标为基准
 * - 提供 grid ↔ world 转换
 * - 地图尺寸来自 MapConfig.json
 */

/** 地图基础配置（与 MapConfig.json 对应） */
export interface MapGridConfig {
  width: number;
  height: number;
  gridSize: number;
  worldSize: number;
}

/** 网格坐标 */
export interface GridPos {
  gx: number;
  gy: number;
}

/** 世界坐标 */
export interface WorldPos {
  x: number;
  y: number;
}

@ccclass('GridManager')
export class GridManager {
  private _config: MapGridConfig | null = null;

  /** 初始化（由 MapLoader 调用） */
  init(config: MapGridConfig): void {
    this._config = config;
    console.log(`[GridManager] 初始化: ${config.width}×${config.height} grid, ${config.gridSize}px/tile, world=${config.worldSize}`);
  }

  get config(): MapGridConfig {
    if (!this._config) throw new Error('GridManager 未初始化');
    return this._config;
  }

  // ==================== 坐标转换 ====================

  /** Grid → 世界坐标（方块左上角） */
  gridToWorld(gx: number, gy: number): WorldPos {
    return {
      x: gx * this.config.gridSize,
      y: gy * this.config.gridSize,
    };
  }

  /** Grid → 世界坐标（方块中心） */
  gridToWorldCenter(gx: number, gy: number): Vec3 {
    const gs = this.config.gridSize;
    return new Vec3(gx * gs + gs / 2, gy * gs + gs / 2, 0);
  }

  /** 世界坐标 → Grid */
  worldToGrid(wx: number, wy: number): GridPos {
    const gs = this.config.gridSize;
    return {
      gx: Math.floor(wx / gs),
      gy: Math.floor(wy / gs),
    };
  }

  /** 计算区域的世界包围盒 */
  getWorldBounds(gx: number, gy: number, w: number, h: number): {
    left: number; right: number; bottom: number; top: number;
  } {
    const gs = this.config.gridSize;
    return {
      left: gx * gs,
      right: (gx + w) * gs,
      bottom: gy * gs,
      top: (gy + h) * gs,
    };
  }

  // ==================== 地形 ====================

  /** 判断 Grid 坐标是否在地图内 */
  isInBounds(gx: number, gy: number): boolean {
    return gx >= 0 && gx < this.config.width && gy >= 0 && gy < this.config.height;
  }

  /** 判断世界坐标是否在地图内 */
  isWorldInBounds(wx: number, wy: number): boolean {
    return wx >= 0 && wx < this.config.worldSize && wy >= 0 && wy < this.config.worldSize;
  }
}
