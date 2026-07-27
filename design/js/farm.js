/* === 海岛日记 - 耕种系统 === */

const Farm = (function () {
  'use strict';

  const $ = Utils.$;

  /* 种植 */
  function plantSeed(idx, vegKey) {
    const plot = Store._state.plots[idx];
    if (!plot) return false;
    if (plot.veg) { Toast.show('这块地已经种了东西'); return false; }

    const crop = CROPS[vegKey];
    if (!crop) return false;
    if (Store._state.player.level < crop.unlockLv) {
      Toast.show('等级不足，需要 Lv.' + crop.unlockLv);
      return false;
    }
    if ((Store._state.seeds[vegKey] || 0) <= 0) {
      Toast.show('没有' + crop.name + '种子了');
      return false;
    }

    Store._state.seeds[vegKey]--;
    const now = Date.now();
    plot.veg = vegKey;
    plot.plantedAt = now;
    plot.lastWateredAt = now;
    plot.fertilized = false;
    Store._state.stats.totalPlanted++;

    Toast.show('🌱 种下了' + crop.name + '！');
    Renderer.renderFarmGrid();
    Renderer.updateTopBar();
    Store.save();
    Achievements.check();
    return true;
  }

  /* 浇水 */
  function waterPlot(idx) {
    const plot = Store._state.plots[idx];
    if (!plot || !plot.veg) return false;
    if (Store.getProgress(plot) >= 1) return false;

    plot.lastWateredAt = Date.now();
    Store._state.stats.totalWater++;

    Toast.show('💧 浇水完成！');
    Renderer.renderFarmGrid();
    Store.save();
    Achievements.check();

    /* 更新任务 */
    const q = Store._state.quest;
    if (q && q.type === 'water') {
      q.current++;
      if (q.current >= q.target) {
        completeQuest();
      }
      Renderer.updateTopBar();
    }
    return true;
  }

  /* 收获 */
  function harvestPlot(idx) {
    const plot = Store._state.plots[idx];
    if (!plot || !plot.veg) return false;
    if (Store.getProgress(plot) < 1) return false;

    const crop = CROPS[plot.veg];
    Store._state.veg[plot.veg] = (Store._state.veg[plot.veg] || 0) + 1;
    Store._state.stats.totalHarvest++;

    /* 金币飞行特效 */
    const cell = Renderer.els.farmGrid.querySelectorAll('.plot')[idx];
    if (cell) {
      const rect = cell.getBoundingClientRect();
      const gameRect = Renderer.els.game.getBoundingClientRect();
      CoinFly.show(rect.left - gameRect.left + rect.width / 2, rect.top - gameRect.top);
    }

    Toast.show('🧺 收获了' + crop.name + '！+' + crop.exp + '经验');
    addExp(crop.exp);

    /* 任务检查 */
    const q = Store._state.quest;
    if (q && q.type === 'harvest' && q.veg === plot.veg) {
      q.current++;
      if (q.current >= q.target) {
        completeQuest();
      }
    }

    /* 重置菜地 */
    plot.veg = null;
    plot.plantedAt = null;
    plot.lastWateredAt = null;
    plot.fertilized = false;

    Renderer.renderFarmGrid();
    Renderer.updateTopBar();
    Store.save();
    Achievements.check();
    return true;
  }

  /* 施肥 */
  function fertilizePlot(idx) {
    const plot = Store._state.plots[idx];
    if (!plot || !plot.veg) return false;
    if (Store.getProgress(plot) >= 1) return false;
    if (plot.fertilized) { Toast.show('已经施过肥了'); return false; }

    /* 扣肥料（如果有肥料系统） */
    plot.fertilized = true;
    /* 调整 plantedAt 使得当前 progress 不变但后续加速 */
    const currentProgress = Store.getProgress(plot);
    const crop = CROPS[plot.veg];
    const effectiveGrowTime = crop.growTime / CONFIG.FERTILIZER_SPEED_MULT;
    plot.plantedAt = Date.now() - currentProgress * effectiveGrowTime * 1000;

    Toast.show('✨ 施肥成功！生长速度x' + CONFIG.FERTILIZER_SPEED_MULT);
    Renderer.renderFarmGrid();
    Store.save();
    return true;
  }

  /* 完成任务 */
  function completeQuest() {
    const q = Store._state.quest;
    const reward = q.reward || 30;
    Store._state.player.coins += reward;
    Toast.show('📋 任务完成！+' + reward + '🪙');
    generateNewQuest();
    Renderer.updateTopBar();
  }

  /* 生成新任务 */
  function generateNewQuest() {
    const pool = Object.keys(CROPS).filter(k => Store._state.player.level >= CROPS[k].unlockLv);
    if (pool.length === 0) {
      Store.set('quest', { type: 'harvest', veg: 'cabbage', target: 1, current: 0, reward: 10, label: '收获' });
      return;
    }

    const template = Utils.randPick(QUEST_TEMPLATES);
    let quest;

    switch (template.type) {
      case 'harvest': {
        const vk = Utils.randPick(pool);
        const target = Utils.randInt(2, 4);
        quest = {
          type: 'harvest', veg: vk, target, current: 0,
          reward: template.rewardBase + Utils.randInt(0, 20),
          label: '收获 ' + target + ' ' + CROPS[vk].name,
        };
        break;
      }
      case 'water': {
        const target = Utils.randInt(3, 6);
        quest = {
          type: 'water', target, current: 0,
          reward: template.rewardBase + Utils.randInt(0, 10),
          label: '浇水 ' + target + ' 次',
        };
        break;
      }
      case 'plant': {
        const target = Utils.randInt(2, 4);
        quest = {
          type: 'plant', target, current: 0,
          reward: template.rewardBase + Utils.randInt(0, 15),
          label: '种植 ' + target + ' 次',
        };
        break;
      }
      case 'sell': {
        const target = Utils.randInt(20, 50);
        quest = {
          type: 'sell', target, current: 0,
          reward: template.rewardBase + Utils.randInt(0, 20),
          label: '卖出 ' + target + ' 星币',
        };
        break;
      }
    }

    Store.set('quest', quest);
  }

  /* 增加经验 */
  function addExp(amount) {
    Store._state.player.exp += amount;
    const maxLevel = LEVEL_NAMES.length;

    while (Store._state.player.level < maxLevel
      && Store._state.player.exp >= LEVELS[Store._state.player.level]) {
      Store._state.player.level++;
      LevelUp.show(Store._state.player.level);

      /* 解锁提示 */
      const unlocked = Object.keys(CROPS).filter(k => CROPS[k].unlockLv === Store._state.player.level);
      if (unlocked.length > 0) {
        setTimeout(() => {
          Toast.show('🎉 解锁新作物：' + unlocked.map(k => CROPS[k].name).join('、') + '！');
        }, 800);
      }
    }
    Renderer.updateTopBar();
    Achievements.check();
  }

  /* 处理菜地点击 */
  function handlePlotClick(idx, cellEl) {
    const plot = Store._state.plots[idx];
    if (!plot) return;

    const rect = cellEl.getBoundingClientRect();
    const gameRect = Renderer.els.game.getBoundingClientRect();
    const x = rect.left - gameRect.left + rect.width / 2;
    const y = rect.top - gameRect.top;

    if (!plot.veg) {
      PlotAction.show(idx, x, y, 'seed');
    } else if (Store.getProgress(plot) >= 1) {
      PlotAction.show(idx, x, y, 'harvest');
    } else {
      PlotAction.show(idx, x, y, 'water');
    }
  }

  return {
    plantSeed,
    waterPlot,
    harvestPlot,
    fertilizePlot,
    generateNewQuest,
    addExp,
    handlePlotClick,
  };
})();
