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

  /** 服务器基础地址 */
  SERVER_URL: 'http://localhost:8080',

  /** 是否启用调试日志 */
  DEBUG: true,
} as const;
