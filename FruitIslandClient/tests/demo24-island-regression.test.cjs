const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const assert = require('node:assert/strict');

const page = fs.readFileSync(
  path.join(__dirname, '..', 'demo2.4-island.html'),
  'utf8',
);
const world = fs.readFileSync(
  path.join(__dirname, '..', 'demo2.4-world.html'),
  'utf8',
);

test('order actions refresh the world without reading a cross-origin iframe location', () => {
  assert.doesNotMatch(page, /contentWindow\.location\.reload\s*\(/);
  assert.match(
    page,
    /contentWindow\s*\.\s*postMessage\(\{type:'fruit-island:player-changed'\},'\*'\)/,
  );
  assert.match(world, /event\.data\?\.type==='fruit-island:player-changed'\) refresh\(\)/);
});

test('independent crafting and order crafting keep separate quantities', () => {
  assert.match(page, /let craftQuantity=1;/);
  assert.match(page, /let orderCraftQuantity=1;/);
  assert.doesNotMatch(
    page,
    /craftQuantity=Math\.max\(1,customerQueue\.find/,
  );
  assert.match(
    page,
    /orderCraftQuantity=Math\.max\(1,customerQueue\.find/,
  );
});

test('a visible bar entry reaches the JWT listing flow and refreshes authoritative state', () => {
  assert.match(page, /id="drink-bar-button"/);
  assert.match(page, /onclick="toggleFeatureDrawer\('drink-bar'\)"/);
  assert.match(page, /id="drink-bar-drawer"/);
  assert.match(page, /fetch\(`\$\{GAME_URL\}\/drink-shop\/bars`/);
  assert.match(page, /\/drink-shop\/bars\/\$\{selectedBarId\}\/list/);
  assert.match(page, /Promise\.all\(\[loadDrinkBars\(true\),loadInventory\(true\)\]\)/);
});

test('bar listing actions distinguish low and zero inventory and crafting starts unselected at one', () => {
  assert.match(page, /drink\.inventoryCount>0/);
  assert.match(page, /直接上架/);
  assert.match(page, /drink\.inventoryCount<10/);
  assert.match(page, /去制作/);
  assert.match(page, /function openCraftingStationFromBar\(\)/);
  assert.match(page, /selectedRecipe=null;/);
  assert.match(page, /craftQuantity=1;/);
});

test('selling bar detail shows progress and requires confirmation before take down', () => {
  assert.match(page, /剩余.*remainingQuantity/);
  assert.match(page, /已售.*soldQuantity/);
  assert.match(page, /待收.*pendingGold/);
  assert.match(page, /nextSaleInSeconds/);
  assert.match(page, /confirm\('确定下架这个吧台的当前批次吗？'\)/);
  assert.match(page, /\/drink-shop\/bars\/\$\{selectedBarId\}\/take-down/);
  assert.match(page, /Promise\.all\(\[loadDrinkBars\(true\),loadInventory\(true\)\]\)/);
  assert.match(page, /fruit-island:player-changed/);
});

test('sold out bars expose individual and sold-out-only collect all actions with a badge', () => {
  assert.match(page, /id="drink-bar-badge"/);
  assert.match(page, /filter\(bar=>bar\.state==='SOLD_OUT'\)\.length/);
  assert.match(page, /onclick="collectSelectedBar\(\)"/);
  assert.match(page, /\/drink-shop\/bars\/\$\{selectedBarId\}\/collect/);
  assert.match(page, /onclick="collectAllSoldOutBars\(\)"/);
  assert.match(page, /\/drink-shop\/bars\/collect-all/);
  assert.match(page, /collectableCount>0/);
  assert.match(page, /fruit-island:player-changed/);
});

test('the outdoor map renders three clickable bars on each side of the drink shop', () => {
  assert.match(world, /const DRINK_BARS=\[/);
  assert.match(world, /side:'left'/);
  assert.match(world, /side:'right'/);
  assert.match(world, /DRINK_BARS\.filter\(bar=>bar\.side==='left'\)\.length/);
  assert.match(world, /DRINK_BARS\.filter\(bar=>bar\.side==='right'\)\.length/);
  assert.match(world, /fruit-island:drink-bars-state/);
  assert.match(world, /fruit-island:open-drink-bar/);
  assert.match(world, /bar\.state==='SOLD_OUT'.*🪙/s);
  assert.match(world, /bar\.state==='SELLING'.*🥤/s);
  assert.match(world, /🪑/);
  assert.match(page, /fruit-island:drink-bars-state/);
  assert.match(page, /fruit-island:open-drink-bar/);
});
