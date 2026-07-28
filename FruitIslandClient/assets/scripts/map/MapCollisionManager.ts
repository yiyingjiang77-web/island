import { _decorator, Component, Vec3 } from 'cc';
import { BuildingManager } from './BuildingManager';
import { GridManager } from './GridManager';

const { ccclass } = _decorator;

/**
 * 碰撞管理器 — Demo2.1
 *
 * 职责：
 * - 判断世界坐标是否可通行
 * - 组合多种障碍：水域、沙滩、建筑
 */
@ccclass('MapCollisionManager')
export class MapCollisionManager extends Component {
  private _gridManager: GridManager | null = null;
  private _buildingManager: BuildingManager | null = null;

  /** 水域开始行（不可走） */
  private _waterEdge: number = 1;
  /** 沙滩宽度 */
  private _beachWidth: number = 2;

  init(gridManager: GridManager, buildingManager: BuildingManager): void {
    this._gridManager = gridManager;
    this._buildingManager = buildingManager;
  }

  /** 设置地形参数 */
  setTerrain(waterEdge: number, beachWidth: number): void {
    this._waterEdge = waterEdge;
    this._beachWidth = beachWidth;
  }

  /**
   * 判断世界坐标是否可行走
   *
   * 规则：
   * 1. 必须在地图范围内
   * 2. 不能在水域或沙滩上
   * 3. 不能穿过建筑
   */
  isWalkable(worldX: number, worldY: number): boolean {
    const gm = this._gridManager;
    if (!gm) return true;

    // 1. 边界检查
    if (!gm.isWorldInBounds(worldX, worldY)) return false;

    // 2. 地形检查
    const { gx, gy } = gm.worldToGrid(worldX, worldY);
    const mapW = gm.config.width;
    const grassStart = this._waterEdge + this._beachWidth;
    const grassEnd = mapW - this._waterEdge - this._beachWidth - 1;

    if (gx < grassStart || gx > grassEnd || gy < grassStart || gy > grassEnd) {
      return false;
    }

    // 3. 建筑碰撞检查
    if (this._buildingManager && this._buildingManager.isBlocked(worldX, worldY)) {
      return false;
    }

    return true;
  }

  /**
   * 将世界坐标修正到最近的可走位置
   */
  clampToWalkable(worldX: number, worldY: number): Vec3 {
    const gm = this._gridManager;
    if (!gm) return new Vec3(worldX, worldY, 0);

    const gs = gm.config.gridSize;
    const grassStart = (this._waterEdge + this._beachWidth) * gs;
    const grassEnd = gm.config.worldSize - (this._waterEdge + this._beachWidth) * gs - 1;

    let cx = Math.max(grassStart, Math.min(grassEnd, worldX));
    let cy = Math.max(grassStart, Math.min(grassEnd, worldY));

    // 简单推开建筑：如果被阻挡，尝试往四个方向偏移
    if (this._buildingManager && this._buildingManager.isBlocked(cx, cy)) {
      const offsets = [gs, -gs, gs * 2, -gs * 2, gs * 3, -gs * 3];
      for (const off of offsets) {
        if (!this._buildingManager.isBlocked(cx + off, cy)) { cx += off; break; }
        if (!this._buildingManager.isBlocked(cx, cy + off)) { cy += off; break; }
      }
    }

    return new Vec3(cx, cy, 0);
  }
}
