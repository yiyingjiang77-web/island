/* === 海岛日记 - 输入处理 ===
 * 职责：拖拽场景、点击交互、角色行走、菜单操作
 * 所有用户输入的统一入口
 */

const Input = (function () {
  'use strict';

  /* ===== 初始化所有输入 ===== */
  function setup() {
    setupDrag();       // 场景拖拽 + 点击事件
    setupBottomBar();  // 右下角浮动菜单
    setupWelcome();    // 欢迎页 → 角色选择
    setupBoatClick();  // 小船点击
  }

  /* ===== 场景拖拽与点击 ===== */
  function setupDrag() {
    const scene = Renderer.els.scene;
    let down = false;       // 是否按下
    let moved = false;      // 是否发生了拖拽
    let startX = 0, startY = 0;           // 按下时的屏幕坐标
    let startSceneX = 0, startSceneY = 0; // 按下时的场景偏移

    /* 获取事件坐标（兼容触摸和鼠标） */
    function getXY(e) {
      return {
        x: e.touches ? e.touches[0].clientX : e.clientX,
        y: e.touches ? e.touches[0].clientY : e.clientY,
      };
    }

    /* 按下 */
    function onDown(e) {
      down = true;
      moved = false;
      const pos = getXY(e);
      startX = pos.x;
      startY = pos.y;
      const sp = Renderer.getScenePos();
      startSceneX = sp.x;
      startSceneY = sp.y;
      scene.classList.add('dragging');
    }

    /* 移动 → 场景拖拽 */
    function onMove(e) {
      if (!down) return;
      const pos = getXY(e);
      const dx = pos.x - startX;
      const dy = pos.y - startY;
      // 移动超过阈值才算拖拽（避免误触）
      if (Math.abs(dx) > CONFIG.DRAG_THRESHOLD || Math.abs(dy) > CONFIG.DRAG_THRESHOLD) {
        moved = true;
      }
      if (moved) {
        e.preventDefault();
        const nx = startSceneX + dx;
        const ny = startSceneY + dy;
        const vw = Renderer.els.game.offsetWidth;
        const vh = Renderer.els.game.offsetHeight;
        // 计算拖拽边界：保证背景图每个角落都能到达
        const halfW = CONFIG.SCENE_WIDTH / 2;
        const halfVW = vw / 2;
        const maxX = halfW - halfVW + 100;
        const halfH = CONFIG.SCENE_HEIGHT / 2;
        const halfVH = vh / 2;
        const maxY = halfH - halfVH + 100;
        const clampedX = Utils.clamp(nx, -maxX, maxX);
        const clampedY = Utils.clamp(ny, -maxY, maxY);
        Renderer.updateScenePos(clampedX, clampedY);
      }
    }

    /* 松开 → 判断是点击还是拖拽结束 */
    function onUp(e) {
      scene.classList.remove('dragging');

      if (down && !moved) {
        const target = e.target;

        // 点击菜地格子 → 弹出种植/浇水/收获菜单
        const plot = target.closest('.plot');
        if (plot) {
          Farm.handlePlotClick(parseInt(plot.dataset.idx), plot);
          down = false;
          return;
        }

        // 点击角色 → 触发 NPC 对话
        const charEl = target.closest('.character');
        if (charEl) {
          NPC.talk();
          down = false;
          return;
        }

        // 点击空地 → 角色走到点击位置
        const pos = getXY(e);
        moveCharacterTo(pos.x, pos.y);
      }

      down = false;
    }

    // 绑定事件
    scene.addEventListener('pointerdown', onDown);
    scene.addEventListener('pointermove', onMove);
    scene.addEventListener('pointerup', onUp);
    scene.addEventListener('pointercancel', onUp);

    /* 双击 → 回到默认视角 */
    scene.addEventListener('dblclick', () => {
      const fromX = Renderer.getScenePos().x;
      const fromY = Renderer.getScenePos().y;
      Utils.animate(0, 1, 400,
        (t) => {
          Renderer.updateScenePos(
            fromX + (CONFIG.DEFAULT_CENTER_X - fromX) * t,
            fromY + (CONFIG.DEFAULT_CENTER_Y - fromY) * t
          );
        },
        () => Renderer.updateScenePos(CONFIG.DEFAULT_CENTER_X, CONFIG.DEFAULT_CENTER_Y)
      );
    });

    /* 回到中心按钮 */
    Renderer.els.recenterBtn.addEventListener('click', () => {
      const fromX = Renderer.getScenePos().x;
      const fromY = Renderer.getScenePos().y;
      Utils.animate(0, 1, 400,
        (t) => {
          Renderer.updateScenePos(
            fromX + (CONFIG.DEFAULT_CENTER_X - fromX) * t,
            fromY + (CONFIG.DEFAULT_CENTER_Y - fromY) * t
          );
        },
        () => Renderer.updateScenePos(CONFIG.DEFAULT_CENTER_X, CONFIG.DEFAULT_CENTER_Y)
      );
    });
  }

  /* ===== 右下角浮动菜单 ===== */
  let fabOpen = false;

  function setupBottomBar() {
    const main = Renderer.els.fabMain;
    const actions = Renderer.els.fabActions;
    if (!main) return;

    // 主按钮：展开/收起子按钮
    main.addEventListener('click', (e) => {
      e.stopPropagation();
      fabOpen = !fabOpen;
      actions.classList.toggle('open', fabOpen);
      main.classList.toggle('menu-open', fabOpen);
    });

    // 子按钮：打开对应面板后收起菜单
    actions.querySelectorAll('.fab-btn').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const fn = btn.dataset.fn;
        if (fn) Panels.open(fn);
        fabOpen = false;
        actions.classList.remove('open');
        main.classList.remove('menu-open');
      });
    });

    // 点击菜单外区域关闭
    document.addEventListener('pointerdown', (e) => {
      if (fabOpen && !Renderer.els.fabMenu.contains(e.target)) {
        fabOpen = false;
        actions.classList.remove('open');
        main.classList.remove('menu-open');
      }
    });
  }

  /* ===== 小船点击（直接打开收购面板） ===== */
  function setupBoatClick() {
    const boatIcon = document.getElementById('boatIcon');
    if (boatIcon) {
      boatIcon.addEventListener('click', (e) => {
        e.stopPropagation();
        Panels.open('boat');
      });
    }
  }

  /* ===== 欢迎页 → 角色选择 ===== */
  function setupWelcome() {
    const welcomeBtn = document.getElementById('welcomeBtn');
    if (welcomeBtn) {
      welcomeBtn.addEventListener('click', () => {
        // 隐藏欢迎页
        Renderer.els.welcome.classList.add('hide');
        setTimeout(() => {
          Renderer.els.welcome.style.display = 'none';
          // 首次登录弹出角色选择
          if (!Store._state.settings.hasPickedChar) {
            showCharPicker(false);
          }
        }, 400);
      });
    }
  }

  /* ===== 角色选择器（首次登录 / 菜单更换 共用） ===== */
  function showCharPicker(fromMenu) {
    Renderer.buildCharGrid();  // 生成 5 张角色卡片
    Renderer.els.charPicker.classList.add('show');

    document.getElementById('charConfirmBtn').onclick = () => {
      if (!Store._state.settings.hasPickedChar) {
        Store._state.settings.hasPickedChar = true;
      }
      Store._state.selectedChar = Store._state.selectedChar || 1;
      Store.save();
      Renderer.updatePlayerChar();  // 更新场景中的角色图片
      Renderer.els.charPicker.classList.remove('show');
      Toast.show(fromMenu ? '🎭 角色已更换！' : '🎭 角色已选定！可以在菜单中更换');
    };
  }

  /* ===== 角色点击行走 =====
   * 将屏幕坐标转换为层坐标，移动角色到目标位置
   * 行走时间 0.6s，带弹跳动画
   */
  function moveCharacterTo(cx, cy) {
    const charEl = document.getElementById('playerChar');
    if (!charEl) return;

    const gameRect = Renderer.els.game.getBoundingClientRect();
    const vw = gameRect.width;
    const vh = gameRect.height;
    const sp = Renderer.getScenePos();

    // 屏幕坐标 → 层坐标转换
    const layerLeft = vw / 2 - CONFIG.SCENE_WIDTH / 2 + sp.x;
    const layerTop = vh / 2 - CONFIG.SCENE_HEIGHT / 2 + sp.y;
    const lx = cx - gameRect.left - layerLeft;
    const ly = cy - gameRect.top - layerTop;

    // 限制在地图范围内（角色中心对齐点击位置）
    const tx = Utils.clamp(lx - 24, 0, CONFIG.SCENE_WIDTH - 48);
    const ty = Utils.clamp(ly - 32, 0, CONFIG.SCENE_HEIGHT - 64);

    // 行走动画（CSS transition 驱动）
    charEl.classList.remove('idle');
    charEl.classList.add('walking');
    charEl.style.left = tx + 'px';
    charEl.style.top = ty + 'px';

    // 到达后恢复待机状态
    clearTimeout(charEl._walkTimer);
    charEl._walkTimer = setTimeout(() => {
      charEl.classList.remove('walking');
      charEl.classList.add('idle');
    }, 600);
  }

  return { setup, showCharPicker };
})();
