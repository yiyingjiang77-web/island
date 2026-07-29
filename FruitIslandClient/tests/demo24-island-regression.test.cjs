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
