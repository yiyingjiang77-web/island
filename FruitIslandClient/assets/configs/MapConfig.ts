/**
 * 世界地图配置 — Demo2.2 紧凑版
 *
 * 48×48 Grid, 120px/tile, 5760×5760 世界
 * 核心经营区 32×32，外围预留探索扩展
 */
export const MapConfig = {
  // ==================== 世界尺寸 ====================
  GRID_COUNT: 48,
  TILE_SIZE: 120,

  get WORLD_SIZE(): number { return this.GRID_COUNT * this.TILE_SIZE; },
  get WORLD_CENTER(): number { return this.WORLD_SIZE / 2; },

  // ==================== 地形 ====================
  WATER_EDGE: 1,
  BEACH_WIDTH: 1,

  get GRASS_START(): number { return this.WATER_EDGE + this.BEACH_WIDTH; },
  get GRASS_END(): number { return this.GRID_COUNT - this.WATER_EDGE - this.BEACH_WIDTH - 1; },

  // ==================== 坐标转换 ====================
  gridToWorldX(col: number): number { return col * this.TILE_SIZE + this.TILE_SIZE / 2; },
  gridToWorldY(row: number): number { return row * this.TILE_SIZE + this.TILE_SIZE / 2; },
  worldToGridX(wx: number): number { return Math.floor(wx / this.TILE_SIZE); },
  worldToGridY(wy: number): number { return Math.floor(wy / this.TILE_SIZE); },

  TerrainType: { WATER: 'WATER', BEACH: 'BEACH', GRASS: 'GRASS' } as const,

  getTerrainAt(gx: number, gy: number): string {
    if (gx < this.WATER_EDGE || gx >= this.GRID_COUNT - this.WATER_EDGE ||
        gy < this.WATER_EDGE || gy >= this.GRID_COUNT - this.WATER_EDGE) return this.TerrainType.WATER;
    if (gx < this.GRASS_START || gx > this.GRASS_END ||
        gy < this.GRASS_START || gy > this.GRASS_END) return this.TerrainType.BEACH;
    return this.TerrainType.GRASS;
  },

  isWalkableGrid(gx: number, gy: number): boolean {
    return this.getTerrainAt(gx, gy) === this.TerrainType.GRASS;
  },

  // ==================== 紧凑布局 V3 ====================
  // 核心经营区 ~32×32, 玩家 3-5 秒到达任意建筑

  SPAWN_GX: 27,
  SPAWN_GY: 40,  // 码头上方的主通道

  ZONES: [
    // 顶部 — 动物
    { id: 'chicken_coop', name: '鸡舍', label: '🐔', gx: 35, gy: 15, w: 6, h: 5, color: '#A0D468' },
    { id: 'cow_barn', name: '牛棚', label: '🐄', gx: 25, gy: 7, w: 8, h: 6, color: '#A0D468' },
    // 中上部 — 花园+蜂箱
    { id: 'flower_a', name: '花园A', label: '🌸', gx: 25, gy: 22, w: 4, h: 4, color: '#FFB7C5' },
    { id: 'flower_b', name: '花园B', label: '🌸', gx: 30, gy: 22, w: 4, h: 4, color: '#FFA0B0' },
    { id: 'bee_house', name: '蜂箱', label: '🐝', gx: 26, gy: 20, w: 2, h: 2, color: '#FFB7C5' },
    // 商店
    { id: 'drink_shop', name: '饮品店', label: '🥤', gx: 18, gy: 31, w: 6, h: 5, color: '#FF9F43' },
    { id: 'cake_shop', name: '蛋糕店', label: '🍰', gx: 15, gy: 22, w: 6, h: 5, color: '#FF9F43' },
    { id: 'exchange_shop', name: '交易所', label: '💱', gx: 15, gy: 14, w: 6, h: 4, color: '#FF9F43' },
    // 农田
    { id: 'farm_a', name: '农田A', label: '🌱', gx: 31, gy: 30, w: 4, h: 4, color: '#7EC850' },
    { id: 'farm_b', name: '农田B', label: '🌱', gx: 35.5, gy: 30, w: 4, h: 4, color: '#6DB840' },
    { id: 'farm_c', name: '农田C', label: '🌱', gx: 31, gy: 34.5, w: 4, h: 4, color: '#6DB840' },
    { id: 'farm_d', name: '农田D', label: '🌱', gx: 36, gy: 34.5, w: 4, h: 4, color: '#7EC850' },
    // 底部 — 码头
    { id: 'dock', name: '码头', label: '⚓', gx: 25, gy: 42, w: 5, h: 4, color: '#64A0DC' },
    // 外围 — 预留扩展
    { id: 'forest', name: '森林', label: '🌲', gx: 5, gy: 33, w: 5, h: 8, color: '#2D5A27' },
    { id: 'mine', name: '矿山', label: '⛰️', gx: 5, gy: 5, w: 5, h: 6, color: '#888' },
  ],
} as const;
