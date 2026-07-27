/* === 海岛日记 - 工具函数 === */

const Utils = (function () {
  'use strict';

  /* 缓动函数：easeOutCubic */
  function easeOutCubic(t) {
    return 1 - Math.pow(1 - t, 3);
  }

  /* 缓动函数：easeInOutCubic */
  function easeInOutCubic(t) {
    return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
  }

  /* 值动画（返回清理函数） */
  function animate(from, to, duration, onUpdate, onComplete, easing) {
    const ease = easing || easeOutCubic;
    const start = performance.now();

    function tick(now) {
      const elapsed = now - start;
      const t = Math.min(1, elapsed / duration);
      const val = from + (to - from) * ease(t);
      onUpdate(val);

      if (t < 1) {
        requestAnimationFrame(tick);
      } else if (onComplete) {
        onComplete();
      }
    }

    requestAnimationFrame(tick);
  }

  /* 震动反馈 */
  function vibrate(pattern) {
    if (navigator.vibrate) {
      navigator.vibrate(pattern);
    }
  }

  /* 限制范围 */
  function clamp(val, min, max) {
    return Math.max(min, Math.min(max, val));
  }

  /* 随机整数 [min, max] */
  function randInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  /* 从数组中随机选一项 */
  function randPick(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
  }

  /* 格式化数字 */
  function formatNum(n) {
    if (n >= 10000) return (n / 10000).toFixed(1) + '万';
    if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
    return String(n);
  }

  /* 时间格式化（游戏内时间） */
  function formatGameTime(totalSeconds) {
    const daySeconds = totalSeconds % CONFIG.DAY_DURATION;
    const hours = Math.floor(daySeconds / 3600);
    const minutes = Math.floor((daySeconds % 3600) / 60);
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
  }

  /* 获取游戏内时段 */
  function getTimeOfDay(totalSeconds) {
    const daySeconds = totalSeconds % CONFIG.DAY_DURATION;
    const ratio = daySeconds / CONFIG.DAY_DURATION;
    if (ratio < 0.25) return 'morning';
    if (ratio < 0.5) return 'afternoon';
    if (ratio < 0.75) return 'evening';
    return 'night';
  }

  /* Debounce */
  function debounce(fn, delay) {
    let timer;
    return function (...args) {
      clearTimeout(timer);
      timer = setTimeout(() => fn.apply(this, args), delay);
    };
  }

  /* 简化 querySelector */
  function $(selector, parent) {
    return (parent || document).querySelector(selector);
  }

  function $$(selector, parent) {
    return Array.from((parent || document).querySelectorAll(selector));
  }

  return {
    animate,
    vibrate,
    clamp,
    randInt,
    randPick,
    formatNum,
    formatGameTime,
    getTimeOfDay,
    debounce,
    $,
    $$,
  };
})();
