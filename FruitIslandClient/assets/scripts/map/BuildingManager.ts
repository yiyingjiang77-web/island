import { _decorator, Component, Node, Vec3, Graphics, Color } from 'cc';
import { GridManager } from './GridManager';
import { BuildingConfig } from './MapLoader';

const { ccclass } = _decorator;

/** 建筑类型 → 颜色映射 */
const BUILDING_COLORS: Record<string, string> = {
  animal: '#A0D468',
  decoration: '#FFB7C5',
  shop: '#FF9F43',
  dock: '#64A0DC',
  house: '#DEB887',
};

/** 建筑类型 → 图标 */
const BUILDING_ICONS: Record<string, string> = {
  chicken_coop: '🐔', cow_barn: '🐄', bee_house: '🐝',
  cake_shop: '🍰', drink_shop: '🥤', exchange_shop: '💱',
  dock: '⚓', player_house: '🏠',
};

/**
 * 建筑管理器 — Demo2.1
 *
 * 职责：
 * - 根据 BuildingConfig 创建建筑节点
 * - 管理建筑生命周期
 * - 提供建筑查询接口
 */
@ccclass('BuildingManager')
export class BuildingManager extends Component {
  private _gridManager: GridManager | null = null;
  private _buildingNodes: Map<string, Node> = new Map();
  /** 存储所有建筑的碰撞区域（世界坐标） */
  private _collisionRects: Array<{
    id: string;
    left: number; right: number; bottom: number; top: number;
  }> = [];

  /** 初始化 */
  init(gridManager: GridManager): void {
    this._gridManager = gridManager;
  }

  /** 根据配置创建所有建筑 */
  createBuildings(buildings: BuildingConfig[]): void {
    if (!this._gridManager) {
      console.error('[BuildingManager] GridManager 未初始化');
      return;
    }

    for (const cfg of buildings) {
      this.createBuilding(cfg);
    }
    console.log(`[BuildingManager] ${buildings.length} 个建筑创建完成`);
  }

  /** 创建单个建筑 */
  createBuilding(cfg: BuildingConfig): Node {
    const gm = this._gridManager!;
    const gs = gm.config.gridSize;

    // 中心世界坐标
    const centerX = (cfg.gx + cfg.width / 2) * gs;
    const centerY = (cfg.gy + cfg.height / 2) * gs;

    const buildingNode = new Node(`Building_${cfg.id}`);
    buildingNode.setPosition(centerX, centerY, 1);

    // 绘制占位方块
    const g = buildingNode.addComponent(Graphics);
    const color = BUILDING_COLORS[cfg.type] || '#888888';
    const { r, g: green, b } = hexToRgb(color);
    g.fillColor = new Color(r, green, b, 160);
    const hw = (cfg.width * gs) / 2;
    const hh = (cfg.height * gs) / 2;
    g.rect(-hw, -hh, cfg.width * gs, cfg.height * gs);
    g.fill();

    // 边框
    g.strokeColor = new Color(255, 255, 255, 200);
    g.lineWidth = 2;
    g.rect(-hw, -hh, cfg.width * gs, cfg.height * gs);
    g.stroke();

    // 记录碰撞区域
    this._collisionRects.push({
      id: cfg.id,
      left: cfg.gx * gs,
      right: (cfg.gx + cfg.width) * gs,
      bottom: cfg.gy * gs,
      top: (cfg.gy + cfg.height) * gs,
    });

    this._buildingNodes.set(cfg.id, buildingNode);
    this.node.addChild(buildingNode);

    return buildingNode;
  }

  /** 判断某个世界坐标是否被建筑阻挡 */
  isBlocked(worldX: number, worldY: number, padding: number = 0): boolean {
    for (const rect of this._collisionRects) {
      if (worldX >= rect.left - padding && worldX < rect.right + padding &&
          worldY >= rect.bottom - padding && worldY < rect.top + padding) {
        return true;
      }
    }
    return false;
  }

  /** 获取所有碰撞矩形 */
  get collisionRects(): Array<{ id: string; left: number; right: number; bottom: number; top: number }> {
    return this._collisionRects;
  }

  /** 获取建筑节点 */
  getBuilding(id: string): Node | undefined {
    return this._buildingNodes.get(id);
  }
}

/** hex → rgb */
function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const h = hex.replace('#', '');
  return {
    r: parseInt(h.slice(0, 2), 16),
    g: parseInt(h.slice(2, 4), 16),
    b: parseInt(h.slice(4, 6), 16),
  };
}
