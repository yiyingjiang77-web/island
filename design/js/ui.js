/* === 海岛日记 - UI 系统 === */

/* Toast */
const Toast = (function () {
  'use strict';
  let timer;

  function show(msg) {
    const el = Renderer.els.toast;
    if (!el) return;
    el.textContent = msg;
    el.classList.add('show');
    clearTimeout(timer);
    timer = setTimeout(() => el.classList.remove('show'), 2200);
  }

  return { show };
})();

/* 金币飞行特效 */
const CoinFly = (function () {
  'use strict';

  function show(x, y) {
    const fly = document.createElement('div');
    fly.className = 'coin-fly';
    fly.textContent = '🪙';
    fly.style.left = x + 'px';
    fly.style.top = y + 'px';

    const coinEl = Renderer.els.coinText;
    if (coinEl) {
      const rect = coinEl.getBoundingClientRect();
      const gameRect = Renderer.els.game.getBoundingClientRect();
      fly.style.setProperty('--dx', (rect.left - gameRect.left - x + 10) + 'px');
      fly.style.setProperty('--dy', (rect.top - gameRect.top - y - 10) + 'px');
    }

    Renderer.els.game.appendChild(fly);
    setTimeout(() => { if (fly.parentNode) fly.parentNode.removeChild(fly); }, 800);
  }

  return { show };
})();

/* 升级动画 */
const LevelUp = (function () {
  'use strict';

  function show(lvl) {
    const lu = Renderer.els.levelUp;
    if (!lu) return;
    const name = LEVEL_NAMES[lvl - 1] || '';
    lu.querySelector('.lu-text').textContent = 'Lv.' + lvl + ' ' + name;
    lu.classList.add('show');

    for (let i = 0; i < 8; i++) {
      const star = document.createElement('div');
      star.className = 'lu-stars';
      star.textContent = ['⭐', '✨', '🌟', '💫'][i % 4];
      star.style.left = '50%';
      star.style.top = '50%';
      const angle = Math.PI * 2 * i / 8;
      star.style.setProperty('--dx', Math.cos(angle) * 120 + 'px');
      star.style.setProperty('--dy', Math.sin(angle) * 120 + 'px');
      lu.appendChild(star);
      setTimeout(() => { if (star.parentNode) star.parentNode.removeChild(star); }, 1000);
    }

    setTimeout(() => lu.classList.remove('show'), 1500);
    Utils.vibrate([50, 50, 50]);
  }

  return { show };
})();

/* 成就弹窗 */
const Achievements = (function () {
  'use strict';

  function check() {
    const s = Store._state;
    const ach = s.achievements;

    const conditions = {
      first_harvest: s.stats.totalHarvest >= 1,
      harvest_10: s.stats.totalHarvest >= 10,
      harvest_50: s.stats.totalHarvest >= 50,
      coins_1000: s.player.coins >= 1000,
      coins_5000: s.player.coins >= 5000,
      level_3: s.player.level >= 3,
      level_5: s.player.level >= 5,
      all_crops: Object.keys(CROPS).every(k => s.player.level >= CROPS[k].unlockLv),
      water_20: s.stats.totalWater >= 20,
      sell_500: s.stats.totalCoinsEarned >= 500,
    };

    ACHIEVEMENTS.forEach((a) => {
      if (!ach[a.id] && conditions[a.id]) {
        ach[a.id] = true;
        if (a.reward > 0) {
          s.player.coins += a.reward;
        }
        showToast(a);
        Store.save();
      }
    });
    Renderer.updateTopBar();
  }

  function showToast(achievement) {
    const toast = document.createElement('div');
    toast.className = 'achievement-toast';
    toast.innerHTML = '<span class="ach-icon">' + achievement.icon + '</span>'
      + '<span class="ach-text">🏆 ' + achievement.name + '！</span>'
      + '<span class="ach-reward">' + achievement.desc
      + (achievement.reward > 0 ? '  +' + achievement.reward + '🪙' : '') + '</span>';
    document.getElementById('game').appendChild(toast);
    setTimeout(() => { if (toast.parentNode) toast.parentNode.removeChild(toast); }, 3100);
  }

  return { check };
})();

/* 菜地操作弹窗 */
const PlotAction = (function () {
  'use strict';
  const $ = Utils.$;

  function show(idx, x, y, type) {
    const plot = Store._state.plots[idx];
    const el = Renderer.els.plotAction;
    if (!el) return;
    let html = '';

    if (type === 'seed') {
      html += '<div class="pa-title">选择种子</div><div class="pa-seeds">';
      Object.keys(CROPS).forEach((vk) => {
        const v = CROPS[vk];
        const unlocked = Store._state.player.level >= v.unlockLv;
        const count = Store._state.seeds[vk] || 0;
        html += '<button class="pa-seed-btn" data-seed="' + vk + '" '
          + (unlocked && count > 0 ? '' : 'disabled') + '>'
          + '<span class="pa-seed-emoji">' + v.emoji + '</span>'
          + '<span class="pa-seed-count">' + (unlocked ? (count + '粒') : 'Lv.' + v.unlockLv) + '</span>'
          + '</button>';
      });
      html += '</div>';
    } else if (type === 'water') {
      const pState = Store.getPlotState(idx);
      const crop = CROPS[plot.veg];
      html += '<div class="pa-title">' + crop.name + ' · 生长中</div>';
      html += '<div class="pa-progress-text">进度 ' + Math.round(pState.progress * 100)
        + '% · 水分 ' + Math.round(pState.water) + '%</div>';
      if (pState.water < 100) {
        html += '<button class="pa-action pa-water" data-action="water">💧 浇水</button>';
      } else {
        html += '<div class="pa-progress-text" style="color:var(--green)">水分充足，等待生长~</div>';
      }
    } else if (type === 'harvest') {
      const crop = CROPS[plot.veg];
      html += '<div class="pa-title">' + crop.name + ' · 成熟了！</div>';
      html += '<button class="pa-action pa-harvest" data-action="harvest">🧺 收获 (+' + crop.exp + '经验)</button>';
    }

    el.innerHTML = html;
    el.style.left = x + 'px';
    el.style.top = y + 'px';
    el.classList.add('show');

    /* 绑定事件 */
    el.querySelectorAll('[data-seed]').forEach((btn) => {
      btn.addEventListener('click', () => {
        Farm.plantSeed(idx, btn.dataset.seed);
        hide();
      });
    });
    const waterBtn = el.querySelector('[data-action="water"]');
    if (waterBtn) waterBtn.addEventListener('click', () => {
      Farm.waterPlot(idx);
      show(idx, parseInt(el.style.left), parseInt(el.style.top), 'water');
    });
    const harvestBtn = el.querySelector('[data-action="harvest"]');
    if (harvestBtn) harvestBtn.addEventListener('click', () => {
      Farm.harvestPlot(idx);
      hide();
    });

    setTimeout(() => {
      document.addEventListener('pointerdown', hideOutside, { once: true });
    }, 10);
  }

  function hide() {
    Renderer.els.plotAction.classList.remove('show');
  }

  function hideOutside(e) {
    if (!Renderer.els.plotAction.contains(e.target)) {
      hide();
    } else {
      document.addEventListener('pointerdown', hideOutside, { once: true });
    }
  }

  return { show, hide };
})();

/* 面板管理 */
const Panels = (function () {
  'use strict';

  function open(name) {
    if (name === 'diary' || name === 'friends') {
      Toast.show('🔒 该功能将在后续版本开放');
      return;
    }

    let html = '';
    switch (name) {
      case 'seeds': html = buildSeedShop(); break;
      case 'inventory': html = buildInventory(); break;
      case 'boat': html = buildBoatShop(); break;
      case 'menu': html = buildMenu(); break;
    }

    const panel = Renderer.els.panel;
    const overlay = Renderer.els.overlay;
    panel.innerHTML = html;
    panel.classList.add('active');
    overlay.classList.add('active');

    /* 关闭按钮 */
    const closeBtn = panel.querySelector('.panel-close');
    if (closeBtn) closeBtn.addEventListener('click', close);
    overlay.addEventListener('click', close, { once: true });

    /* 绑定按钮事件 */
    panel.querySelectorAll('[data-buy]').forEach((btn) => {
      btn.addEventListener('click', () => buySeed(btn.dataset.buy));
    });
    panel.querySelectorAll('[data-sell]').forEach((btn) => {
      btn.addEventListener('click', () => sellVeg(btn.dataset.sell, 1));
    });
    const sellAll = document.getElementById('sellAllBtn');
    if (sellAll) sellAll.addEventListener('click', sellAllVeg);

    const dt = document.getElementById('debugToggle');
    if (dt) dt.addEventListener('click', () => {
      Store._state.settings.debugMode = !Store._state.settings.debugMode;
      dt.classList.toggle('on', Store._state.settings.debugMode);
      Toast.show(Store._state.settings.debugMode ? '⚡ 加速模式开启！' : '加速模式关闭');
      Store.save();
    });

    const ccBtn = document.getElementById('changeCharBtn');
    if (ccBtn) ccBtn.addEventListener('click', () => {
      close();
      setTimeout(() => Input.showCharPicker(true), 300);
    });

    const rb = document.getElementById('resetBtn');
    if (rb) rb.addEventListener('click', () => {
      if (confirm('确定要重新开始吗？所有进度将被清除。')) {
        Store.reset();
        location.reload();
      }
    });
  }

  function close() {
    Renderer.els.panel.classList.remove('active');
    Renderer.els.overlay.classList.remove('active');
  }

  function buildSeedShop() {
    let html = '<div class="panel-header"><h2>🌱 种子商店</h2><button class="panel-close">×</button></div>'
      + '<div class="panel-body">'
      + '<div style="font-size:12px;color:var(--text-light);margin-bottom:10px">选择种子购买，种在菜地里生长</div>';

    Object.keys(CROPS).forEach((vk) => {
      const v = CROPS[vk];
      const unlocked = Store._state.player.level >= v.unlockLv;
      const canBuy = unlocked && Store._state.player.coins >= v.seedCost;
      html += '<div class="shop-item' + (unlocked ? '' : ' shop-locked') + '">';
      html += '<div class="shop-emoji">' + v.emoji + '</div>';
      html += '<div class="shop-info"><div class="shop-name">' + v.name + '</div>';
      html += '<div class="shop-meta">生长 '
        + (Store._state.settings.debugMode ? '2秒' : v.growTime + '秒')
        + ' · 卖价 ' + v.sellPrice + '🪙 · +' + v.exp + '经验</div></div>';
      if (unlocked) {
        html += '<div class="shop-actions"><button class="btn-buy" data-buy="' + vk + '" '
          + (canBuy ? '' : 'disabled') + '>' + v.seedCost + '🪙</button></div>';
      } else {
        html += '<div class="shop-actions"><span class="shop-locked-tag">Lv.' + v.unlockLv + '解锁</span></div>';
      }
      html += '</div>';
    });
    html += '</div>';
    return html;
  }

  function buildInventory() {
    let html = '<div class="panel-header"><h2>📦 仓库</h2><button class="panel-close">×</button></div>'
      + '<div class="panel-body">';

    /* 种子 */
    html += '<div class="inv-section"><div class="inv-section-title">种子</div><div class="inv-grid">';
    let hasSeeds = false;
    Object.keys(CROPS).forEach((vk) => {
      const count = Store._state.seeds[vk] || 0;
      if (count > 0) {
        hasSeeds = true;
        html += '<div class="inv-item"><div class="inv-emoji">' + CROPS[vk].emoji
          + '</div><div class="inv-count">×' + count + '</div></div>';
      }
    });
    if (!hasSeeds) html += '<div class="inv-empty" style="grid-column:1/-1">还没有种子，去商店买一些吧~</div>';
    html += '</div></div>';

    /* 蔬菜 */
    html += '<div class="inv-section"><div class="inv-section-title">蔬菜</div><div class="inv-grid">';
    let hasVeg = false;
    Object.keys(CROPS).forEach((vk) => {
      const count = Store._state.veg[vk] || 0;
      if (count > 0) {
        hasVeg = true;
        html += '<div class="inv-item"><div class="inv-emoji">' + CROPS[vk].emoji
          + '</div><div class="inv-count">×' + count + '</div></div>';
      }
    });
    if (!hasVeg) html += '<div class="inv-empty" style="grid-column:1/-1">还没有蔬菜，去种菜吧~</div>';
    html += '</div></div>';

    html += '</div>';
    return html;
  }

  function buildBoatShop() {
    let html = '<div class="panel-header"><h2>⛵ 收购船</h2><button class="panel-close">×</button></div>'
      + '<div class="panel-body">'
      + '<div style="font-size:12px;color:var(--text-light);margin-bottom:10px">🚤 收购船今日停靠中，把蔬菜卖给它赚星币！</div>';

    let hasVeg = false, totalValue = 0;
    Object.keys(CROPS).forEach((vk) => {
      const count = Store._state.veg[vk] || 0;
      if (count > 0) {
        hasVeg = true;
        const v = CROPS[vk];
        totalValue += v.sellPrice * count;
        html += '<div class="sell-item">';
        html += '<div class="sell-emoji">' + v.emoji + '</div>';
        html += '<div class="sell-info"><div class="sell-name">' + v.name + ' ×' + count + '</div>'
          + '<div class="sell-price">' + v.sellPrice + '🪙/个</div></div>';
        html += '<button class="btn-sell" data-sell="' + vk + '">卖1个</button>';
        html += '</div>';
      }
    });
    if (!hasVeg) html += '<div class="inv-empty">仓库里没有蔬菜可以出售~</div>';
    html += '<button class="btn-sell-all" id="sellAllBtn" ' + (hasVeg ? '' : 'disabled')
      + '>全部出售 (' + totalValue + '🪙)</button>';
    html += '</div>';
    return html;
  }

  function buildMenu() {
    let html = '<div class="panel-header"><h2>⚙️ 菜单</h2><button class="panel-close">×</button></div>'
      + '<div class="panel-body">';

    html += '<div class="menu-item"><div class="menu-icon">⚡</div>'
      + '<div class="menu-label">加速模式</div><div class="menu-value">作物2秒成熟</div>'
      + '<div class="toggle' + (Store._state.settings.debugMode ? ' on' : '') + '" id="debugToggle"></div></div>';

    html += '<div class="menu-item" id="changeCharBtn"><div class="menu-icon">🎭</div>'
      + '<div class="menu-label">更换角色</div><div class="menu-value">'
      + (CHARACTERS.find(c => c.id === Store._state.selectedChar) || CHARACTERS[0]).name
      + '</div></div>';

    html += '<div class="menu-item" id="resetBtn"><div class="menu-icon">🔄</div>'
      + '<div class="menu-label">重新开始</div><div class="menu-value">清除存档</div></div>';

    html += '<div class="menu-item"><div class="menu-icon">ℹ️</div>'
      + '<div class="menu-label">关于</div><div class="menu-value">v0.2</div></div>';

    html += '<div style="margin-top:16px;padding:12px;background:rgba(255,255,255,.4);border-radius:12px;font-size:12px;color:var(--text-light);line-height:1.8">'
      + '<b style="color:var(--text)">海岛日记 v0.2</b><br>'
      + '核心循环：买种子 → 种菜 → 浇水 → 收获 → 卖菜 → 赚星币<br>'
      + '当前等级 Lv.' + Store._state.player.level + ' (' + (LEVEL_NAMES[Store._state.player.level - 1] || '') + ')<br>'
      + '累计经验：' + Store._state.player.exp + ' / '
      + (Store._state.player.level < LEVEL_NAMES.length ? LEVELS[Store._state.player.level] : '满级')
      + '</div>';

    html += '</div>';
    return html;
  }

  function buySeed(vk) {
    const v = CROPS[vk];
    if (Store._state.player.coins < v.seedCost) { Toast.show('星币不足！'); return; }
    Store._state.player.coins -= v.seedCost;
    Store._state.seeds[vk] = (Store._state.seeds[vk] || 0) + 1;
    Toast.show('🌱 购买了' + v.name + '种子！');
    Renderer.updateTopBar();
    Store.save();
    open('seeds');
  }

  function sellVeg(vk, qty) {
    const v = CROPS[vk];
    if ((Store._state.veg[vk] || 0) < qty) { Toast.show('数量不足'); return; }
    Store._state.veg[vk] -= qty;
    const earned = v.sellPrice * qty;
    Store._state.player.coins += earned;
    Store._state.stats.totalCoinsEarned += earned;

    /* 任务检查 */
    const q = Store._state.quest;
    if (q && q.type === 'sell') {
      q.current += earned;
      if (q.current >= q.target) {
        Store._state.player.coins += q.reward;
        Toast.show('📋 任务完成！+' + q.reward + '🪙');
        Farm.generateNewQuest();
      }
    }

    Toast.show('💰 卖出' + v.name + '×' + qty + '，获得' + earned + '星币！');
    Renderer.updateTopBar();
    Store.save();
    Achievements.check();
    open('boat');
  }

  function sellAllVeg() {
    let total = 0, count = 0;
    Object.keys(CROPS).forEach((vk) => {
      const c = Store._state.veg[vk] || 0;
      if (c > 0) { total += CROPS[vk].sellPrice * c; count += c; Store._state.veg[vk] = 0; }
    });
    if (count === 0) { Toast.show('没有蔬菜可卖'); return; }
    Store._state.player.coins += total;
    Store._state.stats.totalCoinsEarned += total;
    Toast.show('💰 卖出' + count + '个蔬菜，获得' + total + '星币！');
    Renderer.updateTopBar();
    Store.save();
    Achievements.check();
    close();
  }

  return { open, close };
})();

/* NPC 对话 */
const NPC = (function () {
  'use strict';

  function talk() {
    const tod = Utils.getTimeOfDay(Store._state.gameTime);
    let pool = NPC_DIALOGUES.default.slice();

    if (tod === 'morning') pool = pool.concat(NPC_DIALOGUES.morning);
    if (tod === 'evening' || tod === 'night') pool = pool.concat(NPC_DIALOGUES.evening);

    /* 偶尔给礼物 */
    if (Math.random() < 0.15) {
      const gift = Utils.randPick(['cabbage', 'carrot']);
      Store._state.seeds[gift] = (Store._state.seeds[gift] || 0) + 1;
      pool = pool.concat(NPC_DIALOGUES.gift);
      setTimeout(() => {
        Toast.show('🎁 获得了' + CROPS[gift].name + '种子！');
        Renderer.updateTopBar();
      }, 1500);
    }

    Toast.show(Utils.randPick(pool));
  }

  return { talk };
})();
