import { _decorator, Component, Node, Vec3, Vec2, Camera, find } from 'cc';
import { MapConfig } from '../../configs/MapConfig';

const { ccclass } = _decorator;

/**
 * 摄像机管理器 — Demo1.6
 *
 * 功能：
 * - 平滑跟随 WorldRoot（基于玩家位置）
 * - 手指拖动屏幕移动摄像机
 * - 边界限制：不可看到地图外
 */
@ccclass('CameraManager')
export class CameraManager extends Component {
  /** 跟随目标节点（WorldRoot） */
  private _target: Node | null = null;

  /** 跟随平滑系数 (0=不跟, 1=瞬移) */
  private _smooth: number = 5;

  /** 屏幕尺寸 */
  private _screenW: number = 750;
  private _screenH: number = 1334;

  /** 是否正在拖动 */
  private _dragging: boolean = false;

  /** 拖动累计偏移 */
  private _dragOffset: Vec3 = new Vec3(0, 0, 0);

  /** 摄像机主体 */
  private _camera: Camera | null = null;

  onLoad(): void {
    const camNode = find('MainCamera') || find('Camera');
    if (camNode) {
      this._camera = camNode.getComponent(Camera);
    }
  }

  /** 设置跟随目标 */
  setTarget(target: Node): void {
    this._target = target;
  }

  /** 获取摄像机组件的引用 */
  get camera(): Camera | null {
    return this._camera;
  }

  // ==================== 拖动 ====================

  /** 开始拖动（由 InputManager 调用） */
  onDragStart(): void {
    this._dragging = true;
  }

  /** 拖动中（世界坐标偏移量） */
  onDragMove(delta: Vec2): void {
    if (!this._dragging) return;
    // 摄像机反向移动（手指右滑 → 画面右移 → 摄像机左移）
    this._dragOffset.x -= delta.x;
    this._dragOffset.y -= delta.y;
  }

  /** 结束拖动 */
  onDragEnd(): void {
    this._dragging = false;
  }

  /** 是否正在拖动 */
  get isDragging(): boolean {
    return this._dragging;
  }

  // ==================== 每帧更新 ====================

  update(dt: number): void {
    if (this._dragging) return; // 拖动时暂停跟随

    if (!this._target) return;

    const worldSize = MapConfig.WORLD_SIZE;
    const halfW = this._screenW / 2;
    const halfH = this._screenH / 2;

    // 目标位置：让玩家在屏幕中心
    const playerPos = this._target.position;
    const targetCamX = playerPos.x - halfW;
    const targetCamY = playerPos.y - halfH;

    // 加上拖动偏移
    const desiredX = targetCamX + this._dragOffset.x;
    const desiredY = targetCamY + this._dragOffset.y;

    // 平滑插值
    const current = this.node.position;
    const lerpX = current.x + (desiredX - current.x) * Math.min(1, dt * this._smooth);
    const lerpY = current.y + (desiredY - current.y) * Math.min(1, dt * this._smooth);

    // 边界裁剪：摄像机左上角不能 < 0，右下角不能 > WORLD_SIZE
    const clampedX = Math.max(0, Math.min(worldSize - this._screenW, lerpX));
    const clampedY = Math.max(0, Math.min(worldSize - this._screenH, lerpY));

    this.node.setPosition(clampedX, clampedY, this.node.position.z);
  }

  // ==================== 查询 ====================

  /** 屏幕坐标 → 世界坐标 */
  screenToWorld(screenX: number, screenY: number): Vec3 {
    if (this._camera) {
      return this._camera.screenToWorld(new Vec3(screenX, screenY, 0));
    }
    // 回退：简易转换
    return new Vec3(
      screenX + this.node.position.x - this._screenW / 2,
      screenY + this.node.position.y - this._screenH / 2,
      0,
    );
  }
}
