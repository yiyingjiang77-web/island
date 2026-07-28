import { _decorator, Component, Node, Graphics, Color, Vec3 } from 'cc';
import { MapConfig } from '../../configs/MapConfig';

const { ccclass } = _decorator;

/**
 * 地图管理器 - Demo0
 *
 * 职责：
 * - 加载/生成地图
 * - 世界坐标 ↔ 网格坐标
 * - 地图边界
 * - 是否可走判断
 *
 * 以后替换为 TiledMap 或美术资源
 */
@ccclass('MapManager')
export class MapManager extends Component {
  /** 所有地块节点 */
  private _tiles: Node[][] = [];

  onLoad(): void {
    this.generateMap();
    console.log(
      `[MapManager] 地图 ${MapConfig.GRID_COLS}×${MapConfig.GRID_ROWS} ` +
      `(${MapConfig.MAP_WIDTH}×${MapConfig.MAP_HEIGHT}px) ` +
      `边界: [${MapConfig.BOUND_LEFT}, ${MapConfig.BOUND_BOTTOM}] → [${MapConfig.BOUND_RIGHT}, ${MapConfig.BOUND_TOP}]`,
    );
  }

  // ==================== 生成地图 ====================

  private generateMap(): void {
    for (let row = 0; row < MapConfig.GRID_ROWS; row++) {
      this._tiles[row] = [];
      for (let col = 0; col < MapConfig.GRID_COLS; col++) {
        const tile = this.createTile(row, col);
        this._tiles[row][col] = tile;
        this.node.addChild(tile);
      }
    }
  }

  private createTile(row: number, col: number): Node {
    const size = MapConfig.TILE_SIZE;
    const pos = this.gridToWorld(row, col);

    const tileNode = new Node(`Tile_${row}_${col}`);
    tileNode.setPosition(pos.x, pos.y, 0);

    const g = tileNode.addComponent(Graphics);
    g.fillColor = this.getTileColor(row, col);
    g.rect(-size / 2, -size / 2, size, size);
    g.fill();

    g.strokeColor = new Color(0, 0, 0, 30);
    g.lineWidth = 0.5;
    g.rect(-size / 2, -size / 2, size, size);
    g.stroke();

    return tileNode;
  }

  private getTileColor(row: number, col: number): Color {
    const edgeRows = MapConfig.WATER_EDGE_ROWS;
    const isEdge =
      row < edgeRows ||
      row >= MapConfig.GRID_ROWS - edgeRows ||
      col < edgeRows ||
      col >= MapConfig.GRID_COLS - edgeRows;

    if (isEdge) return new Color(100, 160, 220); // 水

    const isSand =
      row < edgeRows + MapConfig.BEACH_ROWS ||
      row >= MapConfig.GRID_ROWS - edgeRows - MapConfig.BEACH_ROWS ||
      col < edgeRows + MapConfig.BEACH_ROWS ||
      col >= MapConfig.GRID_COLS - edgeRows - MapConfig.BEACH_ROWS;

    if (isSand) return new Color(240, 220, 180); // 沙滩

    const shade = 140 + Math.floor(Math.random() * 40);
    return new Color(120, shade, 80); // 草地
  }

  // ==================== 坐标转换 ====================

  /**
   * 网格坐标 → 世界坐标（格子中心）
   */
  gridToWorld(row: number, col: number): Vec3 {
    const x = MapConfig.BOUND_LEFT + col * MapConfig.TILE_SIZE + MapConfig.TILE_SIZE / 2;
    const y = MapConfig.BOUND_BOTTOM + row * MapConfig.TILE_SIZE + MapConfig.TILE_SIZE / 2;
    return new Vec3(x, y, 0);
  }

  /**
   * 世界坐标 → 网格坐标
   */
  worldToGrid(worldPos: Vec3): { row: number; col: number } {
    const col = Math.floor((worldPos.x - MapConfig.BOUND_LEFT) / MapConfig.TILE_SIZE);
    const row = Math.floor((worldPos.y - MapConfig.BOUND_BOTTOM) / MapConfig.TILE_SIZE);
    return { row, col };
  }

  // ==================== 边界与可行走 ====================

  /**
   * 判断世界坐标是否在地图范围内
   */
  isInBounds(worldPos: Vec3): boolean {
    return (
      worldPos.x >= MapConfig.BOUND_LEFT &&
      worldPos.x <= MapConfig.BOUND_RIGHT &&
      worldPos.y >= MapConfig.BOUND_BOTTOM &&
      worldPos.y <= MapConfig.BOUND_TOP
    );
  }

  /**
   * 判断世界坐标是否可走
   *
   * Demo0 规则：不在水里（边缘行）即可走
   * 以后扩展：建筑、树木、NPC 等障碍物
   */
  isWalkable(worldPos: Vec3): boolean {
    if (!this.isInBounds(worldPos)) return false;

    const { row, col } = this.worldToGrid(worldPos);
    if (row < 0 || row >= MapConfig.GRID_ROWS) return false;
    if (col < 0 || col >= MapConfig.GRID_COLS) return false;

    // 水和沙滩不可走，草地可走
    const edgeRows = MapConfig.WATER_EDGE_ROWS + MapConfig.BEACH_ROWS;
    if (row < edgeRows || row >= MapConfig.GRID_ROWS - edgeRows) return false;
    if (col < edgeRows || col >= MapConfig.GRID_COLS - edgeRows) return false;

    return true;
  }

  /**
   * 将世界坐标限制在地图可走区域内
   */
  clampToWalkable(worldPos: Vec3): Vec3 {
    const walkableLeft = MapConfig.BOUND_LEFT + (MapConfig.WATER_EDGE_ROWS + MapConfig.BEACH_ROWS) * MapConfig.TILE_SIZE;
    const walkableRight = MapConfig.BOUND_RIGHT - (MapConfig.WATER_EDGE_ROWS + MapConfig.BEACH_ROWS) * MapConfig.TILE_SIZE;
    const walkableBottom = MapConfig.BOUND_BOTTOM + (MapConfig.WATER_EDGE_ROWS + MapConfig.BEACH_ROWS) * MapConfig.TILE_SIZE;
    const walkableTop = MapConfig.BOUND_TOP - (MapConfig.WATER_EDGE_ROWS + MapConfig.BEACH_ROWS) * MapConfig.TILE_SIZE;

    return new Vec3(
      Math.max(walkableLeft, Math.min(walkableRight, worldPos.x)),
      Math.max(walkableBottom, Math.min(walkableTop, worldPos.y)),
      0,
    );
  }

  /**
   * 获取地图边界（世界坐标）
   */
  getBounds(): { left: number; right: number; bottom: number; top: number } {
    return {
      left: MapConfig.BOUND_LEFT,
      right: MapConfig.BOUND_RIGHT,
      bottom: MapConfig.BOUND_BOTTOM,
      top: MapConfig.BOUND_TOP,
    };
  }

  // ==================== 查询 ====================

  /** 获取某个格子的世界坐标 */
  getTileWorldPos(row: number, col: number): Vec3 | null {
    if (row < 0 || row >= MapConfig.GRID_ROWS || col < 0 || col >= MapConfig.GRID_COLS) {
      return null;
    }
    const tile = this._tiles[row]?.[col];
    return tile ? tile.position.clone() : null;
  }
}
