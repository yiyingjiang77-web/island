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
  cumulativeExp?: number;
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
  /** 本轮种植时的等级快照。 */
  cropLevel?: number;
  /** 本轮成熟后可收获的数量快照。 */
  yieldCount?: number;
  /** 本轮收获经验快照。 */
  harvestExp?: number;
  /** PERMANENT / TEMPORARY。 */
  accessType?: string;
  plantTime?: string;
  finishTime?: string;
  waterLevel?: number;
}

/** 作物种植记录，对应 crop_plant 表 */
export interface CropPlant {
  id: number;
  landId: number;
  cropId: string;
  cropLevel: number;
  growSecondsSnapshot: number;
  yieldCountSnapshot: number;
  harvestExpSnapshot: number;
  accessType: 'PERMANENT' | 'TEMPORARY';
  accessGrantId?: number;
  plantTime: string;
  finishTime: string;
  status: string; // WAITING_WATER | GROWING | READY | HARVESTED
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

/** 作物基础配置，对应 crop_config 表 */
export interface CropConfig {
  cropId: string;
  name: string;
  rarity: 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';
  rewardEligible: number;
  permanentUnlockEnabled: number;
  upgradeEnabled: number;
  playerUnlockLevel: number;
  maxCropLevel: number;
  enabled: number;
  createTime?: string;
  updateTime?: string;
}

/** 作物某一等级的数值配置，对应 crop_level_config 表 */
export interface CropLevelConfig {
  id: number;
  cropId: string;
  cropLevel: number;
  growSeconds: number;
  yieldCount: number;
  /** 收获该等级作物一次获得的玩家经验。 */
  harvestExp: number;
  /** 从上一等级升级到本等级需要的金币；1 级为 0。 */
  upgradeGold: number;
  createTime?: string;
  updateTime?: string;
}

/** 玩家等级成长配置。 */
export interface PlayerLevelConfig {
  level: number;
  requiredExp: number;
  rewardGold: number;
}

/** 收获接口返回的产物与经验结算。 */
export interface HarvestResult {
  playerLandId: number;
  cropId: string;
  cropLevel: number;
  yieldCount: number;
  expGained: number;
  playerLevel: number;
  playerExp: number;
  nextLevelExp?: number;
  levelsGained: number;
  levelRewardGold: number;
}

/** 作物永久种植权获得渠道，对应 crop_unlock_source 表。 */
export interface CropUnlockSource {
  id: number;
  cropId: string;
  sourceType: 'INITIAL' | 'GOLD_SHOP' | 'DIAMOND_SHOP' | 'LEVEL_REWARD';
  currencyType: 'NONE' | 'GOLD' | 'DIAMOND';
  price: number;
  requiredPlayerLevel: number;
  sourceRefId?: string;
  enabled: number;
}

/** 玩家永久拥有的作物；存在记录即可以无限次种植。 */
export interface PlayerCrop {
  id: number;
  playerId: number;
  cropId: string;
  cropLevel: number;
  unlockSource: string;
  unlockTime: string;
}

/** 玩家当前有效的限时稀有作物权限；不可升级。 */
export interface PlayerCropGrant {
  id: number;
  playerId: number;
  cropId: string;
  grantCropLevel: number;
  grantSource: string;
  sourceRefId?: string;
  validFrom: string;
  validUntil: string;
  status: 'ACTIVE' | 'EXPIRED' | 'REVOKED';
}

/** 背包物品，对应 inventory 表 */
export interface InventoryItem {
  id: number;
  playerId: number;
  itemId: string;
  count: number;
  updateTime: string;
}

// ==================== 花卉系统 (Demo2.8) ====================

/** 花卉基础配置，对应 flower_config 表 */
export interface FlowerConfig {
  flowerId: string;
  name: string;
  currencyType: 'GOLD' | 'DIAMOND';
  seedPrice: number;
  growSeconds: number;
  yieldCount: number;
  harvestExp: number;
  honeyCoefficient: number;
  maxLevel: number;
  enabled: number;
  createTime?: string;
  updateTime?: string;
}

/** 花卉各等级数值配置，对应 flower_level_config 表 */
export interface FlowerLevelConfig {
  id: number;
  flowerId: string;
  flowerLevel: number;
  growSeconds: number;
  yieldCount: number;
  harvestExp: number;
  upgradeGold: number;
  createTime?: string;
  updateTime?: string;
}

/** 玩家永久花卉种植权，对应 player_flower_right 表 */
export interface PlayerFlowerRight {
  id: number;
  playerId: number;
  flowerId: string;
  flowerLevel: number;
  unlockSource: string;
  unlockTime: string;
  createTime?: string;
  updateTime?: string;
}

/** 玩家蜂箱状态，对应 player_beehive 表 */
export interface PlayerBeehive {
  id: number;
  playerId: number;
  beehiveCount: number;
  honeyStored: number;
  lastProduceTime: string;
  lastCollectTime: string;
  createTime?: string;
  updateTime?: string;
}

/** /beehive/collect 返回的收取结果 */
export interface HoneyCollectResult {
  honeyCollected: number;
}

// ==================== 小岛成长 (Demo2.7) ====================

/** 小岛等级奖励状态 */
export interface IslandLevelRewardVO {
  level: number;
  cumulativeExp: number;
  cropId?: string;
  recipeId?: string;
  claimed: boolean;
  materialSourceHint?: string;
  shopCapabilityHint?: string;
}

/** /game/init 中的 islandGrowth 字段 */
export interface IslandGrowthVO {
  cumulativeExp: number;
  currentLevel: number;
  nextLevelThreshold?: number;
  rewards: IslandLevelRewardVO[];
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
  cropConfigs: CropConfig[];
  cropLevelConfigs: CropLevelConfig[];
  cropUnlockSources: CropUnlockSource[];
  playerCrops: PlayerCrop[];
  cropGrants: PlayerCropGrant[];
  playerLevelConfigs: PlayerLevelConfig[];
  islandGrowth?: IslandGrowthVO;
  flowerConfigs?: FlowerConfig[];
  flowerLevelConfigs?: FlowerLevelConfig[];
  playerFlowerRights?: PlayerFlowerRight[];
  playerBeehive?: PlayerBeehive;
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
