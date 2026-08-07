const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');

function deferred() {
  let resolve;
  const promise = new Promise(value => {
    resolve = value;
  });
  return {promise, resolve};
}

function createElement() {
  return {
    children: [],
    classList: {add() {}, remove() {}},
    contentWindow: {postMessage() {}},
    innerHTML: '',
    setAttribute() {},
    style: {},
    textContent: '',
    hidden: false,
  };
}

async function flushPromises() {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
  await new Promise(resolve => setImmediate(resolve));
}

test('clicking an outdoor table while bars load opens that table when loading finishes', async () => {
  const page = fs.readFileSync(
    path.join(__dirname, '..', 'demo2.4-island.html'),
    'utf8',
  );
  const script = page.match(/<script>([\s\S]*)<\/script>/)[1];
  const elements = new Map();
  const barsResponse = deferred();
  const context = vm.createContext({
    clearInterval() {},
    confirm() { return true; },
    console,
    Date,
    document: {
      getElementById(id) {
        if (!elements.has(id)) elements.set(id, createElement());
        return elements.get(id);
      },
    },
    Error,
    fetch(url) {
      if (url.endsWith('/auth/wechat/login')) {
        return Promise.resolve({
          json: async () => ({code: 0, data: {token: 'test-token'}}),
        });
      }
      if (url.endsWith('/game/init')) {
        return Promise.resolve({
          json: async () => ({
            code: 0,
            data: {cropConfigs: [], inventory: []},
          }),
        });
      }
      if (url.endsWith('/drink-shop/bars')) {
        return barsResponse.promise;
      }
      throw new Error(`Unexpected fetch: ${url}`);
    },
    JSON,
    Map,
    Math,
    Number,
    Promise,
    setInterval() { return 1; },
    String,
    window: {
      addEventListener() {},
    },
  });

  vm.runInContext(script, context);
  await flushPromises();
  vm.runInContext('openDrinkBarSlot(6)', context);
  barsResponse.resolve({
    json: async () => ({
      code: 0,
      data: {
        bars: Array.from({length: 6}, (_, index) => ({
          barId: 101 + index,
          slotNumber: 1 + index,
          state: 'EMPTY',
          batch: null,
        })),
        drinks: [],
      },
    }),
  });
  await flushPromises();

  const panel = elements.get('drink-bar-panel').innerHTML;
  assert.match(
    panel,
    /class="bar-slot selected"[^>]*>\s*<strong>6 号吧台<\/strong>/,
  );
});

test('crafting shows remaining material and disables plus at the material limit', () => {
  const page = fs.readFileSync(
    path.join(__dirname, '..', 'demo2.4-island.html'),
    'utf8',
  );
  const script = page.match(/<script>([\s\S]*)<\/script>/)[1];
  const elements = new Map();
  const context = vm.createContext({
    clearInterval() {},
    confirm() { return true; },
    console,
    Date,
    document: {
      getElementById(id) {
        if (!elements.has(id)) elements.set(id, createElement());
        return elements.get(id);
      },
    },
    Error,
    fetch(url) {
      if (url.endsWith('/auth/wechat/login')) {
        return Promise.resolve({
          json: async () => ({code: 0, data: {token: 'test-token'}}),
        });
      }
      if (url.endsWith('/game/init')) {
        return Promise.resolve({
          json: async () => ({
            code: 0,
            data: {cropConfigs: [], inventory: []},
          }),
        });
      }
      if (url.endsWith('/drink-shop/bars')) {
        return Promise.resolve({
          json: async () => ({code: 0, data: {bars: [], drinks: []}}),
        });
      }
      throw new Error(`Unexpected fetch: ${url}`);
    },
    JSON,
    Map,
    Math,
    Number,
    Promise,
    setInterval() { return 1; },
    String,
    window: {
      addEventListener() {},
    },
  });

  vm.runInContext(script, context);
  vm.runInContext(`
    craftRecipes=[{
      recipeId:'strawberry_juice',
      name:'草莓汁',
      outputItem:'strawberry_juice',
      outputCount:1,
      maxCraftable:2,
      materials:[{itemId:'strawberry',requiredCount:2,inventoryCount:4}],
    }];
    selectedRecipe=craftRecipes[0];
    craftQuantity=2;
    renderCraftingStation();
  `, context);

  const station = elements.get('crafting-station').innerHTML;
  assert.match(station, /制作后剩余：0/);
  assert.match(
    station,
    /<button onclick="changeCraftQuantity\(1\)" disabled>＋<\/button>/,
  );
});

test('growth panel renders configured progress, ordered rewards, and max level state', async () => {
  const page = fs.readFileSync(path.join(__dirname, '..', 'demo2.4-island.html'), 'utf8');
  const script = page.match(/<script>([\s\S]*)<\/script>/)[1];
  const elements = new Map();
  const context = vm.createContext({
    clearInterval() {}, confirm() { return true; }, console, Date,
    document: {getElementById(id) { if (!elements.has(id)) elements.set(id, createElement()); return elements.get(id); }},
    Error,
    fetch(url) {
      if (url.endsWith('/auth/wechat/login')) return Promise.resolve({json: async () => ({code: 0, data: {token: 'test-token'}})});
      if (url.endsWith('/game/init')) return Promise.resolve({json: async () => ({code: 0, data: {
        inventory: [], cropConfigs: [{cropId: 'orange', name: '橙子'}],
        islandGrowth: {cumulativeExp: 260, currentLevel: 3, nextLevelThreshold: 450, rewards: [
          {level: 3, cumulativeExp: 250, cropId: 'orange', recipeId: 'orange_juice', claimed: true},
          {level: 2, cumulativeExp: 100, cropId: 'carrot', recipeId: 'carrot_juice', claimed: true},
          {level: 5, cumulativeExp: 700, cropId: 'blueberry', recipeId: 'milk_ice_cream', claimed: false,
            materialSourceHint: '解锁牛棚后获得牛奶', shopCapabilityHint: '饮品店达到5级'},
        ]}, autoSettledSatisfactionRewards: [],
      }})});
      if (url.endsWith('/drink-shop/bars')) return Promise.resolve({json: async () => ({code: 0, data: {bars: [], drinks: []}})});
      throw new Error(`Unexpected fetch: ${url}`);
    },
    JSON, Map, Math, Number, Promise, setInterval() { return 1; }, String,
    window: {addEventListener() {}, setTimeout() {}},
  });
  vm.runInContext(script, context);
  await flushPromises();

  const panel = elements.get('growth-panel').innerHTML;
  assert.match(panel, /Lv\.3/);
  assert.match(panel, /260.*450/s);
  assert.match(panel, /Lv\.2 ·.*Lv\.3 ·/s);
  assert.match(panel, /解锁牛棚后获得牛奶/);
  assert.match(panel, /饮品店达到5级/);

  vm.runInContext('growthState.islandGrowth.currentLevel=10; growthState.islandGrowth.nextLevelThreshold=null; renderIslandGrowth()', context);
  assert.match(elements.get('growth-panel').innerHTML, /已达到当前最高等级/);
});

test('renovation preview renders every server-provided change and blocks unmet requirements', async () => {
  const page = fs.readFileSync(path.join(__dirname, '..', 'demo2.4-island.html'), 'utf8');
  const script = page.match(/<script>([\s\S]*)<\/script>/)[1];
  const elements = new Map();
  const context = vm.createContext({
    clearInterval() {}, confirm() { return true; }, console, Date,
    document: {getElementById(id) { if (!elements.has(id)) elements.set(id, createElement()); return elements.get(id); }},
    Error,
    fetch(url) {
      if (url.endsWith('/auth/wechat/login')) return Promise.resolve({json: async () => ({code: 0, data: {token: 'test-token'}})});
      if (url.endsWith('/game/init')) return Promise.resolve({json: async () => ({code: 0, data: {inventory: [], cropConfigs: [], autoSettledSatisfactionRewards: []}})});
      if (url.endsWith('/drink-shop/bars')) return Promise.resolve({json: async () => ({code: 0, data: {bars: [], drinks: []}})});
      if (url.endsWith('/drink-shop/progress')) return Promise.resolve({json: async () => ({code: 0, data: {
        currentLevel: 4, currentGold: 3000, missingGold: 500, islandLevelMet: false, maxLevel: false,
        currentConfig: {queueCapacity: 6, barCapacity: 13, saleIntervalSeconds: 285, arrivalIntervalSeconds: 120},
        nextConfig: {level: 5, requiredIslandLevel: 5, renovationGold: 3500, queueCapacity: 6,
          barCapacity: 14, saleIntervalSeconds: 285, arrivalIntervalSeconds: 120,
          iceCreamEnabled: 1, advancedRecipeEnabled: 0, improvementText: '开放冰淇淋制作能力'},
      }})});
      throw new Error(`Unexpected fetch: ${url}`);
    },
    JSON, Map, Math, Number, Promise, setInterval() { return 1; }, String,
    window: {addEventListener() {}, setTimeout() {}},
  });
  vm.runInContext(script, context);
  await vm.runInContext('loadDrinkShopProgress()', context);
  await flushPromises();

  const panel = elements.get('renovation-panel').innerHTML;
  assert.match(panel, /4 级.*5 级/s);
  assert.match(panel, /3500/);
  assert.match(panel, /队列容量.*6.*6/s);
  assert.match(panel, /吧台上限.*13.*14/s);
  assert.match(panel, /销售间隔.*285.*285/s);
  assert.match(panel, /到店间隔.*120.*120/s);
  assert.match(panel, /开放冰淇淋制作能力/);
  assert.match(panel, /需要小岛达到 5 级/);
  assert.match(panel, /还缺 500 金币/);
  assert.match(panel, /<button[^>]*disabled[^>]*>装修到 5 级<\/button>/);
});

test('satisfaction panel renders authoritative rules, tier gap, full history, and one closable notice', () => {
  const page = fs.readFileSync(path.join(__dirname, '..', 'demo2.4-island.html'), 'utf8');
  const script = page.match(/<script>([\s\S]*)<\/script>/)[1];
  const elements = new Map();
  const context = vm.createContext({
    clearInterval() {}, confirm() { return true; }, console, Date,
    document: {getElementById(id) { if (!elements.has(id)) elements.set(id, createElement()); return elements.get(id); }},
    fetch(url) {
      if (url.endsWith('/auth/wechat/login')) return Promise.resolve({json: async () => ({code: 0, data: {token: 'test-token'}})});
      if (url.endsWith('/game/init')) return Promise.resolve({json: async () => ({code: 0, data: {inventory: [], cropConfigs: [], autoSettledSatisfactionRewards: []}})});
      if (url.endsWith('/drink-shop/bars')) return Promise.resolve({json: async () => ({code: 0, data: {bars: [], drinks: []}})});
      throw new Error(`Unexpected fetch: ${url}`);
    },
    Error, JSON, Map, Math, Number, Promise, setInterval() { return 1; }, String,
    window: {addEventListener() {}, setTimeout() {}},
  });
  vm.runInContext(script, context);
  vm.runInContext(`
    satisfactionState={
      today:{deliveredOrders:3,rejectedOrders:1,deliveredQuantity:21,satisfactionPercent:75,
        expectedTier:'S63',expectedGold:123,quantityNeeded:0,nextTier:'S77',nextTierPercentNeeded:2},
      giftRules:[
        {tierCode:'S63',minimumPercent:63,minimumDeliveredQuantity:19,rewardGold:123},
        {tierCode:'S77',minimumPercent:77,minimumDeliveredQuantity:19,rewardGold:456},
      ],
      recentHistory:[{businessDate:'2026-08-04',deliveredOrders:4,rejectedOrders:2,
        deliveredQuantity:24,satisfactionPercent:66,giftTier:'S63',rewardGold:123,rewardStatus:'GRANTED'}],
    };
    renderSatisfaction();
    toggleSatisfactionRules();
  `, context);

  const panel = elements.get('satisfaction-panel').innerHTML;
  assert.match(panel, /当前预计 S63/);
  assert.match(panel, /距离 S77 还差 2 个百分点/);
  assert.match(panel, /至少交付 19 份/);
  assert.match(panel, /63%.*S63.*🪙123/s);
  assert.match(panel, /2026-08-04/);
  assert.match(panel, /成功 4.*拒绝 2/s);
  assert.match(panel, /24 份.*66%.*S63/s);
  assert.match(panel, /🪙123/);

  vm.runInContext('toggleSatisfactionRules()', context);
  assert.doesNotMatch(elements.get('satisfaction-panel').innerHTML, /至少交付 19 份/);

  vm.runInContext(`showSatisfactionGiftNotifications([{businessDate:'2026-08-04',giftTier:'S63',rewardGold:123}])`, context);
  const toast = elements.get('gift-toast');
  assert.equal(toast.hidden, false);
  assert.match(toast.innerHTML, /已自动补发 1 天满意度礼品/);
  assert.match(toast.innerHTML, /closeSatisfactionGiftNotification/);
  vm.runInContext('closeSatisfactionGiftNotification()', context);
  assert.equal(toast.hidden, true);
  vm.runInContext(`showSatisfactionGiftNotifications([{businessDate:'2026-08-04',giftTier:'S63',rewardGold:123}])`, context);
  assert.equal(toast.hidden, true);
});
