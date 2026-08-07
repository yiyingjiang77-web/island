/**
 * 接口地址集中管理
 *
 * 与后端 Controller 路径一一对应
 */

export const Api = {
  // ==================== 用户中心 ====================

  /** 微信登录 */
  WECHAT_LOGIN: '/auth/wechat/login',

  /** 刷新 Token */
  REFRESH_TOKEN: '/auth/refresh',

  // ==================== 游戏初始化 ====================

  /** 游戏初始化（获取玩家完整数据） */
  GAME_INIT: '/game/init',

  // ==================== 玩家 ====================

  /** 获取玩家信息 */
  PLAYER_INFO: '/game/player/info',

  // ==================== 岛屿 ====================

  /** 获取岛屿信息 */
  ISLAND_INFO: '/game/island/info',

  // ==================== 土地 / 农场 ====================

  /** 获取土地列表 */
  LAND_LIST: '/farm/lands',

  /** 购买土地 */
  LAND_BUY: '/farm/buy',

  /** 种植 */
  PLANT: '/farm/plant',

  /** 浇水 */
  WATER: '/farm/water',

  /** 收获 */
  HARVEST: '/farm/harvest',

  /** 使用金币升级永久拥有的作物 */
  CROP_UPGRADE: '/crop/upgrade',

  // ==================== 建筑 ====================

  /** 获取建筑列表 */
  BUILDING_LIST: '/game/building/list',

  /** 升级建筑 */
  BUILDING_UPGRADE: '/game/building/upgrade',

  // ==================== 背包 ====================

  /** 获取背包 */
  INVENTORY_LIST: '/game/inventory/list',

  // ==================== 商店 ====================

  /** 获取商店物品 */
  SHOP_LIST: '/game/shop/list',

  /** 购买 */
  SHOP_BUY: '/game/shop/buy',

  // ==================== 任务 ====================

  /** 获取任务列表 */
  QUEST_LIST: '/game/quest/list',

  /** 领取任务奖励 */
  QUEST_CLAIM: '/game/quest/claim',

  // ==================== 顾客订单 ====================

  /** 获取顾客订单 */
  CUSTOMER_ORDER_LIST: '/game/customer/order/list',

  /** 提交顾客订单 */
  CUSTOMER_ORDER_SUBMIT: '/game/customer/order/submit',

  // ==================== 生产 ====================

  /** 开始生产 */
  PRODUCTION_START: '/game/production/start',

  /** 领取产物 */
  PRODUCTION_CLAIM: '/game/production/claim',

  // ==================== 饮品店制作台 ====================

  /** 获取制作台配方、材料库存和最大可制作数量 */
  DRINK_SHOP: '/drink-shop',

  /** 制作饮品（身份由 JWT 决定） */
  DRINK_SHOP_CRAFT: '/drink-shop/craft',

  // ==================== 花卉系统 (Demo2.8) ====================

  /** 购买花卉永久种植权（金币或钻石） */
  FLOWER_PURCHASE: '/flower/purchase',

  /** 升级花卉等级 */
  FLOWER_UPGRADE: '/flower/upgrade',

  // ==================== 蜂箱系统 (Demo2.8) ====================

  /** 购买蜂箱（递增价格，最多3个） */
  BEEHIVE_PURCHASE: '/beehive/purchase',

  /** 收取蜂蜜 */
  BEEHIVE_COLLECT: '/beehive/collect',

  /** 查看蜂箱状态（含惰性结算） */
  BEEHIVE_STATUS: '/beehive/status',

  // ==================== 畜牧系统 (Demo2.9) ====================

  /** 查看牛棚与鸡舍汇总状态（触发惰性结算） */
  LIVESTOCK_STATUS: '/livestock/status',

  /** 首次解锁牛棚（岛屿Lv5 + 1000金币） */
  LIVESTOCK_BARN_UNLOCK: '/livestock/barn/unlock',

  /** 升级牛棚 */
  LIVESTOCK_BARN_UPGRADE: '/livestock/barn/upgrade',

  /** 首次解锁鸡舍（岛屿Lv8 + 3000金币） */
  LIVESTOCK_COOP_UNLOCK: '/livestock/coop/unlock',

  /** 升级鸡舍 */
  LIVESTOCK_COOP_UPGRADE: '/livestock/coop/upgrade',

  // ==================== Demo2.10 蛋糕店 ====================

  /** 蛋糕店状态 */
  CAKE_SHOP_STATUS: '/cake-shop/status',

  /** 解锁蛋糕店 */
  CAKE_SHOP_UNLOCK: '/cake-shop/unlock',

  /** 升级蛋糕店 */
  CAKE_SHOP_UPGRADE: '/cake-shop/upgrade',

  /** 可制作蛋糕配方列表 */
  CAKE_SHOP_RECIPES: '/cake-shop/recipes',

  /** 制作蛋糕 */
  CAKE_SHOP_CRAFT: '/cake-shop/craft',

  /** 蛋糕架状态 */
  CAKE_SHOP_RACKS: '/cake-shop/racks',

  /** 上架蛋糕 */
  CAKE_SHOP_RACK_LIST: '/cake-shop/racks',  // + /{slot}/list

  /** 下架蛋糕 */
  CAKE_SHOP_RACK_TAKEDOWN: '/cake-shop/racks',  // + /{slot}/takedown

  /** 收取售罄 */
  CAKE_SHOP_RACK_COLLECT: '/cake-shop/racks',  // + /{slot}/collect

  // ==================== Demo3.0 配方商店 ====================

  /** 配方商店列表（含购买状态） */
  RECIPE_SHOP_LIST: '/recipe-shop/list',

  /** 购买配方 */
  RECIPE_SHOP_BUY: '/recipe-shop/buy',
} as const;
