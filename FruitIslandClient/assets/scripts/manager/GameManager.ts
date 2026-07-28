/**
 * 兼容旧导入路径。
 *
 * 正式游戏入口统一维护在 scripts/game/GameManager.ts，
 * 此文件不再注册第二个 Cocos GameManager 组件。
 */
export { GameManager } from '../game/GameManager';
