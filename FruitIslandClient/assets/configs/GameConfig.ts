/**
 * 游戏全局配置
 *
 * 场景名、网络地址、功能开关等
 */
export const GameConfig = {
  /** 启动场景 */
  LAUNCH_SCENE: 'Launch',

  /** 主场景 */
  MAIN_SCENE: 'Main',

  /** 用户中心地址：登录、刷新 Token */
  AUTH_SERVER_URL: 'http://localhost:8081',

  /** 游戏服务地址：游戏初始化、土地、背包等 */
  GAME_SERVER_URL: 'http://localhost:8082',

  /** 是否启用调试日志 */
  DEBUG: true,
} as const;
