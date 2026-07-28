/**
 * 角色/物体尺寸比例标准 — Demo2.1
 *
 * 基于 72×72 Grid 地图（1 Grid = 120px）
 * 45° 俯视角 Q版风格
 *
 * 坐标基准：所有实体以脚底为锚点 (0.5, 0)
 */
export const ScaleConfig = {
  // ==================== 基础 ====================

  /** 1 Grid = 120px */
  GRID_PX: 120,

  // ==================== 玩家 ====================

  PLAYER: {
    /** 宽 (px) */
    WIDTH: 120,
    /** 高 (px) */
    HEIGHT: 210,
    /** 绘制参考尺寸（用于 Graphics 占位绘制） */
    SPRITE_SIZE: 60,
    /** 高度占 Grid 比例 */
    get GRID_RATIO(): number { return this.HEIGHT / 120; },
    /** 普通行走速度 px/s ≈ 1 Grid/s */
    WALK_SPEED: 120,
    /** 跑步速度 */
    RUN_SPEED: 180,
    /** 到达阈值 (px) */
    ARRIVE_THRESHOLD: 8,
  },

  // ==================== NPC 顾客 ====================

  NPC: {
    WIDTH_MIN: 70,
    WIDTH_MAX: 90,
    HEIGHT_MIN: 120,
    HEIGHT_MAX: 160,
    WALK_SPEED: 80,
  },

  // ==================== 动物 ====================

  ANIMAL: {
    CHICKEN:   { w: 70,  h: 70 },
    SQUIRREL:  { w: 60,  h: 60 },
    COW:       { w: 160, h: 120 },
    BEE:       { w: 30,  h: 30 },
    DOG:       { w: 70,  h: 80 },
    CAT:       { w: 60,  h: 70 },
    RABBIT:    { w: 55,  h: 65 },
  },

  // ==================== 装饰 ====================

  DECORATION: {
    TABLE:     { w: 100, h: 100 },
    CHAIR:     { w: 60,  h: 80 },
    FENCE:     { w: 40,  h: 60 },
    TREE_SM:   { w: 100, h: 180 },
    TREE_LG:   { w: 160, h: 280 },
  },

  // ==================== 建筑（参考） ====================
  // 实际尺寸由 MapConfig.json 中 Grid 计算

  /** Grid → px 换算 */
  gridToPx(gridUnits: number): number {
    return gridUnits * this.GRID_PX;
  },

  // ==================== 摄像机 ====================

  CAMERA: {
    /** 横屏可见 Grid 数 */
    VIEW_GRIDS_H: 10,
    /** 竖屏可见 Grid 数 */
    VIEW_GRIDS_V: 16,
    /** 跟随平滑系数 */
    SMOOTH: 6,
  },

  // ==================== 土地 ====================

  LAND: {
    /** 单格土地 (1 Grid) */
    TILE_PX: 120,
    /** 农场 Block (4×4 Grid) */
    FARM_BLOCK_PX: 480,
  },
} as const;
