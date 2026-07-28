/**
 * 地图配置
 *
 * 所有地图相关常量集中管理
 * 后续可从服务器配置表加载
 */
export const MapConfig = {
  /** 网格行数 */
  GRID_ROWS: 15,

  /** 网格列数 */
  GRID_COLS: 20,

  /** 每格像素 */
  TILE_SIZE: 64,

  /** 地图世界宽度 */
  get MAP_WIDTH(): number {
    return this.GRID_COLS * this.TILE_SIZE;
  },

  /** 地图世界高度 */
  get MAP_HEIGHT(): number {
    return this.GRID_ROWS * this.TILE_SIZE;
  },

  /** 地图左边界（世界坐标） */
  get BOUND_LEFT(): number {
    return -this.MAP_WIDTH / 2;
  },

  /** 地图右边界 */
  get BOUND_RIGHT(): number {
    return this.MAP_WIDTH / 2;
  },

  /** 地图下边界 */
  get BOUND_BOTTOM(): number {
    return -this.MAP_HEIGHT / 2;
  },

  /** 地图上边界 */
  get BOUND_TOP(): number {
    return this.MAP_HEIGHT / 2;
  },

  /** 边缘几行是水（不可行走） */
  WATER_EDGE_ROWS: 1,

  /** 沙滩行数（可行走） */
  BEACH_ROWS: 1,
} as const;
