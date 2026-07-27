/* === 海岛日记 - 状态管理 === */

const Store = (function () {
  'use strict';

  /* 初始状态 */
  function createInitialState() {
    return {
      player: {
        coins: 500,
        level: 1,
        exp: 0,
      },
      plots: [],
      seeds: { cabbage: 5, carrot: 3, tomato: 0, potato: 0, chili: 0, strawberry: 0, corn: 0, sunflower: 0 },
      veg: { cabbage: 0, carrot: 0, tomato: 0, potato: 0, chili: 0, strawberry: 0, corn: 0, sunflower: 0 },
      quest: { type: 'harvest', veg: 'cabbage', target: 3, current: 0, reward: 30 },
      achievements: {},
      stats: { totalHarvest: 0, totalWater: 0, totalPlanted: 0, totalCoinsEarned: 0 },
      selectedChar: 1,
      settings: { debugMode: false, soundOn: false, hasPickedChar: false },
      scene: { x: 0, y: 0 },
      gameTime: 0,       // 游戏内总秒数
      day: 1,
      version: CONFIG.SAVE_VERSION,
    };
  }

  /* 单一状态对象 */
  let state = createInitialState();
  let listeners = {};

  /* 初始化菜地 */
  function initPlots() {
    const plots = [];
    for (let i = 0; i < CONFIG.GRID_COLS * CONFIG.GRID_ROWS; i++) {
      plots.push({
        idx: i,
        veg: null,
        plantedAt: null,
        lastWateredAt: null,
        fertilized: false,
      });
    }
    return plots;
  }

  /* 获取状态路径 */
  function get(path) {
    return path.split('.').reduce((obj, key) => (obj != null ? obj[key] : undefined), state);
  }

  /* 设置状态路径 */
  function set(path, value) {
    const keys = path.split('.');
    const lastKey = keys.pop();
    const target = keys.reduce((obj, key) => {
      if (obj[key] == null) obj[key] = {};
      return obj[key];
    }, state);
    const old = target[lastKey];
    target[lastKey] = value;

    /* 通知该路径的监听者 */
    Object.keys(listeners).forEach((lp) => {
      if (path.startsWith(lp) || lp.startsWith(path)) {
        listeners[lp].forEach((fn) => fn(value, old, path));
      }
    });

    return value;
  }

  /* 订阅状态变化 */
  function on(path, fn) {
    if (!listeners[path]) listeners[path] = [];
    listeners[path].push(fn);
    return () => {
      listeners[path] = listeners[path].filter((f) => f !== fn);
    };
  }

  /* 派生值：进度百分比 */
  function getProgress(plot) {
    if (!plot.veg || !plot.plantedAt) return 0;
    if (CROPS[plot.veg].growTime === 0) return 1;
    const growSec = CROPS[plot.veg].growTime;
    const multiplier = plot.fertilized ? CONFIG.FERTILIZER_SPEED_MULT : CONFIG.GROWTH_SPEED_MULTIPLIER;
    const elapsed = (Date.now() - plot.plantedAt) / 1000;
    return Math.min(1, elapsed / (growSec * multiplier));
  }

  /* 派生值：水分百分比 */
  function getWater(plot) {
    if (!plot.veg || !plot.lastWateredAt) return 100;
    const elapsed = (Date.now() - plot.lastWateredAt) / 1000;
    const thirstTime = (CROPS[plot.veg].growTime || 10) * 0.4;
    return Math.max(0, Math.min(100, 100 - (elapsed / thirstTime) * 100));
  }

  /* 存档 */
  function save() {
    try {
      const saveData = {
        player: state.player,
        plots: state.plots,
        seeds: state.seeds,
        veg: state.veg,
        quest: state.quest,
        achievements: state.achievements,
        stats: state.stats,
        settings: state.settings,
        selectedChar: state.selectedChar,
        gameTime: state.gameTime,
        day: state.day,
        version: CONFIG.SAVE_VERSION,
      };
      localStorage.setItem(CONFIG.SAVE_KEY, JSON.stringify(saveData));
    } catch (e) {
      /* localStorage 满或不可用 */
    }
  }

  /* 读档 */
  function load() {
    try {
      const raw = localStorage.getItem(CONFIG.SAVE_KEY);
      if (!raw) return false;

      const data = JSON.parse(raw);

      /* 版本迁移 */
      if (!data.version || data.version < 2) {
        migrateSave(data);
      }

      state.player = data.player || createInitialState().player;
      state.plots = data.plots || [];
      state.seeds = data.seeds || createInitialState().seeds;
      state.veg = data.veg || createInitialState().veg;
      state.quest = data.quest || createInitialState().quest;
      state.achievements = data.achievements || {};
      state.stats = data.stats || createInitialState().stats;
      state.settings = data.settings || createInitialState().settings;
      state.selectedChar = data.selectedChar || 1;
      state.gameTime = data.gameTime || 0;
      state.day = data.day || 1;

      if (state.plots.length === 0) {
        state.plots = initPlots();
      }

      return true;
    } catch (e) {
      return false;
    }
  }

  /* 存档版本迁移 */
  function migrateSave(data) {
    /* v0 → v2: 添加新作物字段 */
    if (!data.seeds) data.seeds = createInitialState().seeds;
    if (!data.veg) data.veg = createInitialState().veg;
    data.seeds.strawberry = data.seeds.strawberry || 0;
    data.seeds.corn = data.seeds.corn || 0;
    data.seeds.sunflower = data.seeds.sunflower || 0;
    data.veg.strawberry = data.veg.strawberry || 0;
    data.veg.corn = data.veg.corn || 0;
    data.veg.sunflower = data.veg.sunflower || 0;
    if (!data.stats) data.stats = createInitialState().stats;
    if (!data.achievements) data.achievements = {};

    /* old plots 迁移到 timestamp 模式 */
    if (data.plots) {
      const now = Date.now();
      data.plots.forEach((p) => {
        if (p.plantedAt == null && p.veg) {
          /* 旧存档用 progress，估算 plantedAt */
          const growSec = CROPS[p.veg] ? CROPS[p.veg].growTime : 10;
          p.plantedAt = now - (p.progress || 0) * growSec * 1000;
          p.lastWateredAt = now - ((100 - (p.water || 100)) / CONFIG.WATER_DECAY_RATE) * 1000;
        }
        if (p.fertilized == null) p.fertilized = false;
      });
    }
    data.version = 2;
  }

  /* 重置 */
  function reset() {
    state = createInitialState();
    state.plots = initPlots();
    try { localStorage.removeItem(CONFIG.SAVE_KEY); } catch (e) {}
  }

  /* 获取派生 plot 数据 */
  function getPlotState(idx) {
    const plot = state.plots[idx];
    if (!plot) return null;
    const progress = getProgress(plot);
    const water = getWater(plot);
    return { ...plot, progress, water };
  }

  return {
    get,
    set,
    on,
    save,
    load,
    reset,
    initPlots,
    getPlotState,
    getProgress,
    getWater,
    /* 直接访问 state（用于批量读取） */
    _state: state,
  };
})();
