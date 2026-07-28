/**
 * 果香小岛 - TypeScript 类型定义
 *
 * 与后端 Java Entity 一一对应
 */

// ==================== 服务器统一响应 ====================

/** 服务器统一返回格式，对应 com.fruitisland.common.result.Result */
export interface ServerResult<T = any> {
  code: number;
  message: string;
  data: T;
}

// ==================== 用户中心 (user_db) ====================

/** 用户账号，对应 user 表 */
export interface User {
  id: number;
  nickname: string;
  avatar: string;
  status: number;
  createTime: string;
  updateTime: string;
}

/** 登录方式，对应 user_login 表 */
export interface UserLogin {
  id: number;
  userId: number;
  platform: string;
  platformUid: string;
  createTime: string;
}

/** 登录 Token，对应 user_token 表 */
export interface UserToken {
  id: number;
  userId: number;
  token: string;
  expireTime: string;
  createTime: string;
}

// ==================== 游戏数据 (fruit_island_db) ====================

/** 游戏角色，对应 game_player 表 */
export interface GamePlayer {
  id: number;
  userId: number;
  gameId: string;
  nickname: string;
  level: number;
  exp: number;
  gold: number;
  diamond: number;
  avatarId: string;
  createTime: string;
  updateTime: string;
}

/** 岛屿，对应 island 表 */
export interface Island {
  id: number;
  playerId: number;
  islandName: string;
  level: number;
  createTime: string;
  updateTime: string;
}

/** 岛屿区域类型 */
export enum AreaType {
  FARM = 'FARM',
  GARDEN = 'GARDEN',
  PASTURE = 'PASTURE',
  DRINK_SHOP_AREA = 'DRINK_SHOP_AREA',
  ICE_CREAM_AREA = 'ICE_CREAM_AREA',
  DESSERT_AREA = 'DESSERT_AREA',
  EXPLORE_AREA = 'EXPLORE_AREA',
}

/** 岛屿区域，对应 island_area 表 */
export interface IslandArea {
  id: number;
  islandId: number;
  areaType: AreaType;
  areaName: string;
  unlockLevel: number;
  unlockCost: number;
  status: number; // 0锁定 1开启
  positionX: number;
  positionY: number;
  createTime: string;
}

/** 土地状态 */
export enum LandState {
  LOCKED = 'LOCKED',
  UNPURCHASED = 'UNPURCHASED',
  EMPTY = 'EMPTY',
  PLANTED = 'PLANTED',
  READY = 'READY',
}

/** 土地配置（全局静态） */
export interface LandConfig {
  id: number;
  areaType: string;   // FARM | FLOWER
  blockId: string;    // Farm-A, Flower-B 等
  gridX: number;      // Block 内 X (0-3)
  gridY: number;      // Block 内 Y (0-3)
  unlockLevel: number;
  buyPrice: number;
}

/** 土地视图 — 合并配置 + 玩家状态 */
export interface LandVO {
  landConfigId: number;
  areaType: string;
  blockId: string;
  gridX: number;
  gridY: number;
  status: string;       // 动态: LOCKED / UNPURCHASED / EMPTY / PLANTED / READY
  unlockLevel: number;
  buyPrice: number;
  playerLandId?: number; // 已购买才有
  cropId?: string;
  plantTime?: string;
  finishTime?: string;
}

/** 作物种植记录，对应 crop_plant 表 */
export interface CropPlant {
  id: number;
  landId: number;
  cropId: string;
  plantTime: string;
  finishTime: string;
  status: string; // GROWING | READY | HARVESTED
  createTime: string;
}

/** 物品配置，对应 item_config 表 */
export interface ItemConfig {
  id: string;
  name: string;
  type: string; // SEED | CROP | MATERIAL | PRODUCT | DECORATION
  icon: string;
  sellPrice: number;
  createTime: string;
}

/** 背包物品，对应 inventory 表 */
export interface InventoryItem {
  id: number;
  playerId: number;
  itemId: string;
  count: number;
  updateTime: string;
}

/** 建筑类型 */
export enum BuildingType {
  HOUSE = 'HOUSE',
  DRINK_SHOP = 'DRINK_SHOP',
  ICE_CREAM_SHOP = 'ICE_CREAM_SHOP',
  DESSERT_SHOP = 'DESSERT_SHOP',
  BEE_HIVE = 'BEE_HIVE',
  CHICKEN_HOUSE = 'CHICKEN_HOUSE',
  COW_HOUSE = 'COW_HOUSE',
}

/** 建筑，对应 building 表 */
export interface Building {
  id: number;
  areaId: number;
  playerId: number;
  type: BuildingType;
  level: number;
  positionX: number;
  positionY: number;
  rotation: number;
  status: number;
  createTime: string;
}

/** 动物，对应 animal 表 */
export interface Animal {
  id: number;
  playerId: number;
  areaId: number;
  type: string;
  level: number;
  createTime: string;
}

/** 顾客订单，对应 customer_order 表 */
export interface CustomerOrder {
  id: number;
  playerId: number;
  customerId: string;
  itemId: string;
  rewardGold: number;
  status: string; // WAIT | SERVING | FINISH
  createTime: string;
}

// ==================== 登录相关 ====================

/** 微信登录请求 */
export interface WechatLoginReq {
  code: string;
}

/** 登录响应 */
export interface LoginRes {
  token: string;
  userId: number;
  expireTime: string;
}

// ==================== 游戏初始化 ====================

/** /game/init 接口响应 */
export interface GameInitData {
  player: GamePlayer;
  island: Island;
  lands: LandVO[];
  inventory: InventoryItem[];
}

// ==================== 错误码（与服务端 ErrorCode 对应） ====================

export enum ErrorCode {
  SUCCESS = 0,
  // 用户 10xxx
  USER_NOT_FOUND = 10001,
  TOKEN_EXPIRED = 10002,
  // 资源 20xxx
  COIN_NOT_ENOUGH = 20001,
  // 地图/土地 30xxx
  LAND_LOCKED = 30001,
  LAND_NOT_UNLOCKED = 30002,
  LAND_ALREADY_PURCHASED = 30003,
  LAND_NOT_EMPTY = 30004,
  GOLD_NOT_ENOUGH = 30005,
}
