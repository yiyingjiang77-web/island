/* === 海岛日记 - 场景渲染 === */

const Renderer = (function () {
  'use strict';

  const $ = Utils.$;

  /* DOM 缓存 */
  let els = {};

  function cacheElements() {
    els.game = document.getElementById('game');
    els.scene = document.getElementById('scene');
    els.farmGrid = document.getElementById('farmGrid');
    els.topbar = document.getElementById('topbar');
    els.fabMenu = document.getElementById('fabMenu');
    els.fabMain = document.getElementById('fabMain');
    els.fabActions = document.getElementById('fabActions');
    els.overlay = document.getElementById('overlay');
    els.panel = document.getElementById('panel');
    els.plotAction = document.getElementById('plotAction');
    els.toast = document.getElementById('toast');
    els.levelUp = document.getElementById('levelUp');
    els.recenterBtn = document.getElementById('recenterBtn');
    els.welcome = document.getElementById('welcome');
    els.charPicker = document.getElementById('charPicker');
    els.charGrid = document.getElementById('charGrid');
    els.playerChar = document.getElementById('playerChar');
    els.dayNightOverlay = document.getElementById('dayNightOverlay');
    els.coinText = document.getElementById('coinText');
    els.levelText = document.getElementById('levelText');
    els.expFill = document.getElementById('expFill');
    els.questText = document.getElementById('questText');
  }

  /* 更新场景位置 */
  function updateScenePos(x, y) {
    Store.set('scene.x', x);
    Store.set('scene.y', y);
    els.scene.style.setProperty('--px', x + 'px');
    els.scene.style.setProperty('--py', y + 'px');
    els.recenterBtn.classList.toggle('show', Math.abs(x - CONFIG.DEFAULT_CENTER_X) > 30 || Math.abs(y - CONFIG.DEFAULT_CENTER_Y) > 30);
  }

  function getScenePos() {
    return { x: Store.get('scene.x'), y: Store.get('scene.y') };
  }

  /* 构建菜地网格 */
  function buildFarmGrid() {
    els.farmGrid.innerHTML = '';
    for (let i = 0; i < CONFIG.GRID_COLS * CONFIG.GRID_ROWS; i++) {
      const cell = document.createElement('div');
      cell.className = 'plot dry';
      cell.dataset.idx = i;
      els.farmGrid.appendChild(cell);
    }
  }

  /* 渲染单个菜地格子 */
  function renderPlotCell(cell, plot) {
    const plotState = Store.getPlotState(plot.idx);
    if (!plotState) return;

    cell.className = 'plot';
    let html = '';

    if (!plotState.veg) {
      cell.classList.add('dry');
    } else {
      if (plotState.water > 50) {
        cell.classList.add('watered');
      } else {
        cell.classList.add('dry');
      }

      const stage = plotState.progress >= 1 ? 4
        : plotState.progress >= 0.66 ? 3
        : plotState.progress >= 0.33 ? 2
        : 1;

      if (stage === 4) {
        cell.classList.add('mature');
        html = '<span class="plot-emoji">' + CROPS[plotState.veg].emoji + '</span>';
      } else if (stage === 1) {
        html = '<span class="plot-seed"></span>';
      } else {
        html = '<span class="plot-emoji plot-' + (stage === 2 ? 'sprout' : 'growing') + '">'
          + STAGE_EMOJI[stage] + '</span>';
      }

      if (plotState.progress < 1) {
        html += '<div class="plot-progress"><div class="plot-progress-fill" style="width:'
          + Math.round(plotState.progress * 100) + '%"></div></div>';
      }

      if (plotState.water < 40 && plotState.progress < 1) {
        html += '<span class="plot-water-icon">💧</span>';
      }

      html += '<div class="plot-water-bar"><div class="plot-water-fill" style="width:'
        + Math.round(plotState.water) + '%;background:'
        + (plotState.water < 40 ? '#E8923C' : plotState.water < 70 ? '#F4D35E' : '#6BB8DD')
        + '"></div></div>';
    }

    cell.innerHTML = html;
  }

  /* 渲染所有菜地 */
  function renderFarmGrid() {
    const cells = els.farmGrid.querySelectorAll('.plot');
    const plots = Store._state.plots;
    for (let i = 0; i < Math.min(cells.length, plots.length); i++) {
      renderPlotCell(cells[i], plots[i]);
    }
  }

  /* 更新顶部状态栏 */
  function updateTopBar() {
    const p = Store._state.player;
    const q = Store._state.quest;

    els.coinText.textContent = p.coins;
    els.levelText.textContent = 'Lv.' + p.level;

    const levelIdx = p.level - 1;
    const expInLevel = p.exp - LEVELS[levelIdx];
    const expNeeded = LEVELS[levelIdx + 1] - LEVELS[levelIdx];
    const expPct = p.level >= LEVEL_NAMES.length ? 100
      : (expInLevel / expNeeded) * 100;
    els.expFill.style.width = Utils.clamp(expPct, 0, 100) + '%';

    if (q && q.type === 'harvest') {
      els.questText.textContent = '收获 ' + q.current + '/' + q.target + ' ' + CROPS[q.veg].emoji;
    } else if (q) {
      els.questText.textContent = q.label + ' ' + q.current + '/' + q.target;
    }
  }

  /* 更新底部功能栏 */
  function updateBottomBar() {
    /* 目前底部栏按钮不随状态变化（除了锁状态） */
    /* 后续可扩展：根据等级解锁日记/好友 */
  }

  /* 更新昼夜覆盖层 */
  function updateDayNightOverlay(gameTime) {
    if (!els.dayNightOverlay) return;
    const ratio = (gameTime % CONFIG.DAY_DURATION) / CONFIG.DAY_DURATION;
    let bg;

    if (ratio < 0.2) {
      bg = 'rgba(255, 180, 80, 0)';   // 清晨
    } else if (ratio < 0.35) {
      const t = (ratio - 0.2) / 0.15;
      bg = 'rgba(255, 200, 100, ' + (t * 0.1) + ')'; // 上午微暖
    } else if (ratio < 0.55) {
      bg = 'rgba(255, 200, 100, 0.08)'; // 正午
    } else if (ratio < 0.7) {
      const t = (ratio - 0.55) / 0.15;
      bg = 'rgba(255, 140, 50, ' + (t * 0.2) + ')'; // 傍晚
    } else if (ratio < 0.85) {
      const t = (ratio - 0.7) / 0.15;
      bg = 'rgba(30, 20, 60, ' + (0.1 + t * 0.2) + ')'; // 入夜
    } else {
      const t = (ratio - 0.85) / 0.15;
      bg = 'rgba(30, 20, 60, ' + (0.3 - t * 0.3) + ')'; // 深夜→黎明
    }

    els.dayNightOverlay.style.background = bg;
  }

  /* 构建森林边缘 CSS 树 */
  function buildForestEdges() {
    const edges = {
      top:    { el: document.getElementById('forestTop'),    count: 35, size: 'small' },
      bottom: { el: document.getElementById('forestBottom'), count: 35, size: 'small' },
      left:   { el: document.getElementById('forestLeft'),   count: 18, size: 'small' },
      right:  { el: document.getElementById('forestRight'),  count: 18, size: 'small' },
    };

    Object.values(edges).forEach(({ el, count, size }) => {
      if (!el) return;
      el.innerHTML = '';
      for (let i = 0; i < count; i++) {
        const tree = document.createElement('span');
        tree.className = 'css-tree ' + (size || '');
        el.appendChild(tree);
      }
    });
  }

  /* 构建石板路 */
  function buildStonePath() {
    const path = $('#stonePath');
    if (!path) return;
    const positions = [
      [0,150], [25,140], [55,130], [30,100],
      [60,90], [35,60], [65,50], [40,20], [70,10]
    ];
    const colors = ['#C4B49A', '#B8A88A', '#D0C0A8', '#C8B898', '#BCAC90'];

    positions.forEach((pos, i) => {
      const s = document.createElement('div');
      s.className = 'stone';
      s.style.left = pos[0] + 'px';
      s.style.top = pos[1] + 'px';
      s.style.background = colors[i % colors.length];
      s.style.width = (18 + Math.random() * 10) + 'px';
      s.style.height = (12 + Math.random() * 8) + 'px';
      path.appendChild(s);
    });
  }

  /* 更新场景中显示的角色图片 */
  function updatePlayerChar() {
    const charId = Store._state.selectedChar || 1;
    const ch = CHARACTERS.find(c => c.id === charId) || CHARACTERS[0];
    if (els.playerChar) {
      els.playerChar.querySelector('img').src = ch.src;
    }
  }

  /* 构建角色选择网格 */
  function buildCharGrid() {
    if (!els.charGrid) return;
    const currentId = Store._state.selectedChar || 1;
    els.charGrid.innerHTML = '';
    CHARACTERS.forEach(ch => {
      const card = document.createElement('div');
      card.className = 'char-card' + (ch.id === currentId ? ' selected' : '');
      card.dataset.charId = ch.id;
      card.innerHTML = '<img src="' + ch.src + '" alt="' + ch.name + '"><span class="char-card-name">' + ch.name + '</span>';
      card.addEventListener('click', () => {
        els.charGrid.querySelectorAll('.char-card').forEach(c => c.classList.remove('selected'));
        card.classList.add('selected');
        Store._state.selectedChar = ch.id;
      });
      els.charGrid.appendChild(card);
    });
  }

  return {
    cacheElements,
    els,
    updateScenePos,
    getScenePos,
    buildFarmGrid,
    buildForestEdges,
    renderFarmGrid,
    renderPlotCell,
    updateTopBar,
    updateBottomBar,
    updateDayNightOverlay,
    buildStonePath,
    updatePlayerChar,
    buildCharGrid,
  };
})();
