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
