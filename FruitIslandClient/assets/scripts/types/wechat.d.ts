/**
 * 微信小游戏 API 类型声明（最小集）
 *
 * Cocos Creator 构建为微信小游戏后，wx 全局对象可用
 */
declare const wx: {
  login(options: {
    success?: (res: { code: string }) => void;
    fail?: (err: any) => void;
    complete?: () => void;
  }): void;

  getUserInfo(options: {
    success?: (res: { userInfo: { nickName: string; avatarUrl: string } }) => void;
    fail?: (err: any) => void;
  }): void;

  getSetting(options: {
    success?: (res: { authSetting: Record<string, boolean> }) => void;
  }): void;

  setStorageSync(key: string, value: any): void;
  getStorageSync(key: string): any;
  removeStorageSync(key: string): void;
};
