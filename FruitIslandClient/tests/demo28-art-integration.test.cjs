const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const assert = require('node:assert/strict');

const clientRoot = path.join(__dirname, '..');
const page = fs.readFileSync(path.join(clientRoot, 'demo2.8-island.html'), 'utf8');
const world = fs.readFileSync(path.join(clientRoot, 'demo2.8-world.html'), 'utf8');

function inlineScripts(html) {
  return [...html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g)]
    .map(match => match[1])
    .filter(Boolean);
}

test('Demo2.8 keeps Demo2.4 intact and opens the new world layer', () => {
  assert.ok(fs.existsSync(path.join(clientRoot, 'demo2.4-island.html')));
  assert.ok(fs.existsSync(path.join(clientRoot, 'demo2.4-world.html')));
  assert.match(page, /<title>果香小岛 Demo2\.8/);
  assert.match(page, /src="\.\/demo2\.8-world\.html"/);
  assert.doesNotMatch(page, /src="\.\/demo2\.4-world\.html"/);
});

test('all production art referenced by the map exists', () => {
  const sourceBlock = world.match(/const ART_SOURCES=\{([\s\S]*?)\n\};/);
  assert.ok(sourceBlock);
  const relativePaths = [...sourceBlock[1].matchAll(/'\.\/(assets\/art\/[^']+)'/g)]
    .map(match => match[1]);
  assert.equal(relativePaths.length, 10);
  for (const relativePath of relativePaths) {
    assert.ok(fs.existsSync(path.join(clientRoot, relativePath)), relativePath);
  }
  assert.match(world, /cowBarn:'\.\/assets\/art\/buildings\/cow-barn-v2\.png'/);
  assert.match(world, /beeHive:'\.\/assets\/art\/buildings\/beehive-v1\.png'/);
});

test('the cow barn and flower garden use their expanded interactive footprints', () => {
  assert.match(world, /id:'cow_barn'.*gx:22,gy:5,w:13,h:9.*art:'cowBarn'/);
  assert.match(world, /id:'flower_garden'.*blockId:'Flower-A'.*type:'garden'.*w:4,h:4.*unlockLevel:10/);
  assert.match(world, /const BEE_HIVES=\[/);
  assert.equal((world.match(/id:'beehive_[ab]'/g) || []).length, 2);
  assert.match(world, /drawArtCentered\('beeHive'/);
  assert.doesNotMatch(world, /id:'bee_house'/);
  assert.doesNotMatch(world, /id:'flower_[ab]'/);
  assert.match(world, /const buildingRects = BUILDINGS\.map\(b => \(\{\s*\.\.\.b,/);
  assert.match(world, /function getClickedBuilding\(wx,wy\)/);
  assert.match(world, /openBuilding\(building\)/);
});

test('shops, mine, dock, two hives, and six outdoor bars render PNG art', () => {
  for (const key of [
    'drinkShop', 'cakeShop', 'exchangeShop', 'chickenCoop', 'cowBarn',
    'mine', 'dock',
  ]) {
    assert.match(world, new RegExp(`art:'${key}'`));
  }
  assert.match(world, /drawArtCentered\('outdoorBar'/);
  assert.match(world, /const DRINK_BARS=MapConfig\.DRINK_BARS/);
  assert.match(world, /left:\(bar\.gx-\.78\)\*TILE/);
  assert.match(world, /right:\(bar\.gx\+\.78\)\*TILE/);
});

test('roads use smooth curves and the garden renders authoritative flower-land states', () => {
  assert.match(world, /function traceSmoothMapPath\(path\)/);
  assert.match(world, /ctx\.bezierCurveTo\(/);
  assert.match(world, /strokeMapPaths\('#f3dfad',66\)/);
  assert.match(world, /item\.areaType==='FLOWER'/);
  assert.match(world, /const blockLands=allLands\.filter\(l=>l\.areaType==='FLOWER'/);
  assert.match(world, /else if\(displayState==='PLANTED'\) icon=/);
  assert.match(world, /else if\(displayState==='READY'\) icon=C_EMOJI\[cell\.cropId\]/);
  assert.doesNotMatch(world, /fruit-island:garden-state/);
  assert.match(world, /apiCall\('GET','\/flower\/catalog'\)/);
  assert.match(world, /apiCall\('POST','\/flower\/buy'/);
});

test('map clicks can open the drink shop drawer through the parent page', () => {
  assert.match(world, /type:'fruit-island:open-feature',feature:'drink-shop'/);
  assert.match(page, /event\.data\?\.type==='fruit-island:open-feature'/);
  assert.match(page, /feature==='drink-shop'/);
});

test('Demo2.8 inline scripts compile', () => {
  for (const script of [...inlineScripts(page), ...inlineScripts(world)]) {
    assert.doesNotThrow(() => new Function(script));
  }
});
