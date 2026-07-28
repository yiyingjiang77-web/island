import { _decorator, Component, Vec3, Vec2, input, Input, EventTouch, Camera } from 'cc';

const { ccclass } = _decorator;

/**
 * 输入管理器 — Demo1.6
 *
 * 职责：
 * - 短点击 ( < 150ms, 移动 < 10px ) → 移动玩家
 * - 长按拖动 → 移动摄像机
 * - 屏幕坐标 → 世界坐标
 */
@ccclass('InputManager')
export class InputManager extends Component {
  private _camera: Camera | null = null;

  /** 点击回调 */
  private _onClickMap: ((worldPos: Vec3) => void) | null = null;

  /** 拖动回调 */
  private _onDragCamera: ((delta: Vec2) => void) | null = null;

  /** 触摸追踪 */
  private _touchStartTime: number = 0;
  private _touchStartPos: Vec2 = new Vec2(0, 0);
  private _lastTouchPos: Vec2 = new Vec2(0, 0);
  private _isDragging: boolean = false;

  /** 点击判定阈值 */
  private readonly TAP_MAX_DURATION: number = 150;   // 毫秒
  private readonly TAP_MAX_DISTANCE: number = 10;    // 像素
  private readonly DRAG_THRESHOLD: number = 5;       // 像素

  onLoad(): void {
    input.on(Input.EventType.TOUCH_START, this.onTouchStart, this);
    input.on(Input.EventType.TOUCH_MOVE, this.onTouchMove, this);
    input.on(Input.EventType.TOUCH_END, this.onTouchEnd, this);
  }

  onDestroy(): void {
    input.off(Input.EventType.TOUCH_START, this.onTouchStart, this);
    input.off(Input.EventType.TOUCH_MOVE, this.onTouchMove, this);
    input.off(Input.EventType.TOUCH_END, this.onTouchEnd, this);
  }

  /** 设置摄像机引用 */
  setCamera(camera: Camera): void {
    this._camera = camera;
  }

  /** 注册点击回调 */
  onClickMap(callback: (worldPos: Vec3) => void): void {
    this._onClickMap = callback;
  }

  /** 注册拖动回调 */
  onDragCamera(callback: (delta: Vec2) => void): void {
    this._onDragCamera = callback;
  }

  // ==================== 触摸处理 ====================

  private onTouchStart(event: EventTouch): void {
    this._touchStartTime = Date.now();
    this._touchStartPos = event.getUILocation();
    this._lastTouchPos = this._touchStartPos.clone();
    this._isDragging = false;
  }

  private onTouchMove(event: EventTouch): void {
    const pos = event.getUILocation();
    const totalDist = Vec2.distance(this._touchStartPos, pos);

    // 超过拖动阈值 → 进入拖动模式
    if (totalDist > this.DRAG_THRESHOLD) {
      this._isDragging = true;
    }

    if (this._isDragging && this._onDragCamera) {
      const delta = new Vec2(
        pos.x - this._lastTouchPos.x,
        pos.y - this._lastTouchPos.y,
      );
      this._onDragCamera(delta);
    }

    this._lastTouchPos = pos;
  }

  private onTouchEnd(event: EventTouch): void {
    if (this._isDragging) return; // 拖动结束，不触发点击

    const duration = Date.now() - this._touchStartTime;
    if (duration > this.TAP_MAX_DURATION) return; // 长按，不算点击

    // 短点击 → 移动玩家
    if (this._onClickMap) {
      const uiPos = event.getUILocation();
      const worldPos = this.screenToWorld(uiPos);
      if (worldPos) {
        this._onClickMap(worldPos);
      }
    }
  }

  // ==================== 坐标转换 ====================

  private screenToWorld(screenPos: Vec2): Vec3 | null {
    if (this._camera) {
      return this._camera.screenToWorld(new Vec3(screenPos.x, screenPos.y, 0));
    }
    // 回退
    return new Vec3(screenPos.x - 375, screenPos.y - 667, 0);
  }
}
