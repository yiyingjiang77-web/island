/* === 海岛日记 - 主控制器 ===
 * 职责：初始化游戏、驱动游戏循环、处理暂停恢复
 * 这是整个游戏的入口和心脏
 */

(function () {
  'use strict';

  /* 游戏循环状态 */
  let lastTick = 0;         // 上一次 tick 的时间戳
  let paused = false;       // 是否暂停（标签页不可见时）
  let tickAccumulator = 0;  // tick 时间累计器

  /* ===== 初始化 ===== */
  function init() {
    /* 1. 缓存所有 DOM 引用 */
    Renderer.cacheElements();

    /* 2. 加载存档 */
    const loaded = Store.load();
    if (!loaded) {
      // 新玩家：初始化菜地数据
      Store._state.plots = Store.initPlots();
      Store.save();
    }

    /* 3. 加载角色图片 */
    Renderer.updatePlayerChar();

    /* 4. 构建界面 */
    Renderer.buildFarmGrid();     // 生成 3x3 菜地格子
    Renderer.renderFarmGrid();    // 首次渲染作物状态
    Renderer.updateTopBar();      // 更新顶部状态栏
    centerScene();                // 场景居中

    /* 5. 绑定交互 */
    Input.setup();

    /* 6. 老玩家跳过欢迎页 */
    if (loaded) {
      const welcome = document.getElementById('welcome');
      if (welcome) {
        welcome.classList.add('hide');
        welcome.style.display = 'none';
      }
    }

    /* 7. 窗口事件 */
    // 窗口大小变化时重新居中
    window.addEventListener('resize', Utils.debounce(centerScene, 200));
    // 标签页切换时暂停/恢复
    document.addEventListener('visibilitychange', onVisibilityChange);

    /* 8. 启动游戏循环 */
    lastTick = performance.now();
    requestAnimationFrame(gameLoop);

    /* 9. 欢迎提示 */
    if (!loaded) {
      setTimeout(() => Toast.show('🏝️ 欢迎来到海岛日记！'), 500);
    }
  }

  /* ===== 游戏主循环 =====
   * 使用 requestAnimationFrame 驱动，每帧执行
   * 通过累加器控制 tick 频率（默认 500ms/tick）
   */
  function gameLoop(timestamp) {
    if (paused) {
      requestAnimationFrame(gameLoop);
      return;
    }

    // 计算帧间隔，上限 5 秒防止大跳跃（如从后台恢复）
    const dt = Math.min((timestamp - lastTick) / 1000, 5);
    lastTick = timestamp;

    // 更新游戏内时间（用于昼夜循环）
    Store._state.gameTime += dt;

    // 按固定间隔触发 tick
    tickAccumulator += dt * 1000;
    while (tickAccumulator >= CONFIG.TICK_INTERVAL) {
      tickAccumulator -= CONFIG.TICK_INTERVAL;
      onTick();
    }

    // 更新昼夜覆盖层颜色
    Renderer.updateDayNightOverlay(Store._state.gameTime);

    requestAnimationFrame(gameLoop);
  }

  /* ===== 每 tick 执行 ===== */
  function onTick() {
    // 刷新菜地：水分和生长进度基于绝对时间戳实时计算
    Renderer.renderFarmGrid();
  }

  /* ===== 标签页可见性变化 =====
   * 切到后台：暂停循环
   * 切回前台：恢复，利用绝对时间戳自动修正所有状态
   */
  function onVisibilityChange() {
    if (document.hidden) {
      paused = true;
    } else {
      paused = false;
      lastTick = performance.now();
      tickAccumulator = 0;
      Renderer.renderFarmGrid();
      Renderer.updateTopBar();
      Store.save();
    }
  }

  /* ===== 场景回到默认视角 ===== */
  function centerScene() {
    Renderer.updateScenePos(CONFIG.DEFAULT_CENTER_X, CONFIG.DEFAULT_CENTER_Y);
  }

  /* 全局导出（浏览器控制台调试用） */
  window.IslandApp = {
    Store, Renderer, Farm, Panels, Toast, Achievements, NPC,
    CONFIG, CROPS,
  };

  /* 启动：等 DOM 就绪后执行 */
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
