/* === 海岛日记 - 游戏配置 === */

const CONFIG = {
  /* 存档 */
  SAVE_KEY: 'island_v2',
  SAVE_VERSION: 2,

  /* 场景：宽度匹配背景图比例 (2508:1672 ≈ 1.5), 高度900 */
  SCENE_WIDTH: 1350,
  SCENE_HEIGHT: 900,
  DEFAULT_CENTER_X: 0,
  DEFAULT_CENTER_Y: 30,
  DRAG_THRESHOLD: 5,

  /* 视差系数：远景完全跟手 */
  PARALLAX_FAR: 1.0,
  PARALLAX_MID: 1.0,
  PARALLAX_NEAR: 1.0,

  /* 游戏循环 (ms) */
  TICK_INTERVAL: 500,

  /* 菜地 */
  GRID_COLS: 3,
  GRID_ROWS: 3,
  PLOT_SIZE: 48,
  PLOT_GAP: 3,

  /* 生长参数 */
  WATER_DECAY_RATE: 2.5,       // 每秒水分消耗
  GROWTH_SPEED_MULTIPLIER: 1,  // 生长速度倍率
  FERTILIZER_SPEED_MULT: 3,    // 施肥后加速倍率

  /* 昼夜循环 */
  DAY_DURATION: 24 * 60,       // 一游戏天 = 24 现实分钟
};

/* 作物数据 */
const CROPS = {
  cabbage: { name: '小白菜', emoji: '🥬', seedCost: 2,  sellPrice: 3,  growTime: 10, exp: 5,  unlockLv: 1, color: '#7EC850' },
  carrot:  { name: '胡萝卜', emoji: '🥕', seedCost: 5,  sellPrice: 8,  growTime: 20, exp: 10, unlockLv: 1, color: '#E8923C' },
  tomato:  { name: '番茄',   emoji: '🍅', seedCost: 4,  sellPrice: 6,  growTime: 30, exp: 8,  unlockLv: 2, color: '#E54B4B' },
  potato:  { name: '土豆',   emoji: '🥔', seedCost: 3,  sellPrice: 5,  growTime: 40, exp: 7,  unlockLv: 3, color: '#C49560' },
  chili:   { name: '辣椒',   emoji: '🌶️', seedCost: 10, sellPrice: 20, growTime: 60, exp: 25, unlockLv: 4, color: '#D4382E' },
  /* 新增作物 */
  strawberry: { name: '草莓', emoji: '🍓', seedCost: 8,  sellPrice: 15, growTime: 25, exp: 12, unlockLv: 2, color: '#E8546E' },
  corn:       { name: '玉米', emoji: '🌽', seedCost: 6,  sellPrice: 10, growTime: 35, exp: 15, unlockLv: 3, color: '#F0C040' },
  sunflower:  { name: '向日葵', emoji: '🌻', seedCost: 12, sellPrice: 25, growTime: 50, exp: 20, unlockLv: 5, color: '#F4D35E' },
};

/* 等级表 */
const LEVELS = [0, 100, 250, 450, 700, 1200, 2000, 999999];
const LEVEL_NAMES = [
  '新手农夫', '勤劳农夫', '田园农夫',
  '资深农夫', '海岛农夫长', '园艺大师', '传奇岛主'
];

/* 作物生长阶段图标 */
const STAGE_EMOJI = { 0: '', 1: 'seed', 2: '🌱', 3: '🌿', 4: '' };

/* 任务模板 */
const QUEST_TEMPLATES = [
  { type: 'harvest', label: '收获', rewardBase: 30 },
  { type: 'water',   label: '浇水', rewardBase: 15 },
  { type: 'plant',   label: '种植', rewardBase: 20 },
  { type: 'sell',    label: '卖出', rewardBase: 25 },
];

/* 成就列表 */
const ACHIEVEMENTS = [
  { id: 'first_harvest',  name: '初次收获',   desc: '完成第一次收获',    icon: '🧺', reward: 50 },
  { id: 'harvest_10',     name: '丰收新手',   desc: '累计收获10次',      icon: '🌾', reward: 100 },
  { id: 'harvest_50',     name: '勤劳农夫',   desc: '累计收获50次',      icon: '🏆', reward: 300 },
  { id: 'coins_1000',     name: '小有积蓄',   desc: '星币达到1000',     icon: '🪙', reward: 0 },
  { id: 'coins_5000',     name: '海岛富翁',   desc: '星币达到5000',     icon: '💰', reward: 0 },
  { id: 'level_3',        name: '田园农夫',   desc: '达到等级3',        icon: '⭐', reward: 80 },
  { id: 'level_5',        name: '海岛农夫长', desc: '达到等级5',        icon: '🌟', reward: 200 },
  { id: 'all_crops',      name: '作物图鉴',   desc: '解锁所有作物',      icon: '📖', reward: 500 },
  { id: 'water_20',       name: '浇水达人',   desc: '累计浇水20次',      icon: '💧', reward: 60 },
  { id: 'sell_500',       name: '经商初成',   desc: '累计卖出500星币',   icon: '⛵', reward: 150 },
];

/* 可选角色列表 */
const CHARACTERS = [
  { id: 1, name: '小岛主',   src: 'assets/角色1.png', emoji: '👧' },
  { id: 2, name: '小水手',   src: 'assets/角色2.png', emoji: '👦' },
  { id: 3, name: '小园丁',   src: 'assets/角色3.png', emoji: '👩' },
  { id: 4, name: '小渔夫',   src: 'assets/角色4.png', emoji: '🧑' },
  { id: 5, name: '小探险家', src: 'assets/角色5.png', emoji: '👨' },
];

/* NPC 对话池 */
const NPC_DIALOGUES = {
  default: [
    '今天也是美好的一天呢～',
    '海风吹着真舒服呀！',
    '我的菜地需要好好照顾...',
    '你闻到花香了吗？',
  ],
  morning: [
    '早上好！新的一天开始啦！',
    '清晨的露水真美～',
  ],
  evening: [
    '夕阳好美呀...',
    '今天辛苦啦，早点休息吧～',
  ],
  gift: [
    '这些送给你，希望你喜欢！',
    '我找到了这个，给你吧！',
  ],
};
