/**
 * 玩家配置
 *
 * 所有玩家相关常量集中管理
 */
export const PlayerConfig = {
  /** 移动速度（像素/秒） */
  MOVE_SPEED: 250,

  /** 到达阈值（像素） */
  ARRIVE_THRESHOLD: 5,

  /** 初始位置 X */
  SPAWN_X: 0,

  /** 初始位置 Y */
  SPAWN_Y: 0,

  /** 角色显示大小 */
  SPRITE_SIZE: 24,
} as const;
