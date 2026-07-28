import { _decorator, Component, Vec3, Vec2, input, Input, EventTouch, Camera, find } from 'cc';

const { ccclass } = _decorator;

/**
 * 输入管理器 - Demo0
 *
 * 职责：
 * - 监听屏幕点击/触摸
 * - 屏幕坐标 → 世界坐标
 * - 通知 GameManager 处理（不直接调用 Player）
 *
 * 关键设计：InputManager 只管"点在哪里"，不管"能不能走"
 */
@ccclass('InputManager')
export class InputManager extends Component {
  private _camera: Camera | null = null;

  /** 外部注册的点击回调（GameManager 注册） */
  private _onClickMap: ((worldPos: Vec3) => void) | null = null;

  onLoad(): void {
    input.on(Input.EventType.TOUCH_START, this.onTouchStart, this);
  }

  start(): void {
    const camNode = find('MainCamera') || find('Camera');
    if (camNode) {
      this._camera = camNode.getComponent(Camera);
    }
  }

  onDestroy(): void {
    input.off(Input.EventType.TOUCH_START, this.onTouchStart, this);
  }

  /**
   * 注册点击地图回调
   *
   * GameManager 在初始化时调用：
   * inputManager.onClickMap((worldPos) => { ... 检查边界 → player.moveTo() })
   */
  onClickMap(callback: (worldPos: Vec3) => void): void {
    this._onClickMap = callback;
  }

  private onTouchStart(event: EventTouch): void {
    const uiPos: Vec2 = event.getUILocation();
    const worldPos = this.screenToWorld(uiPos);
    if (!worldPos) return;

    // 通知 GameManager（由它决定是否移动）
    if (this._onClickMap) {
      this._onClickMap(worldPos);
    }
  }

  private screenToWorld(screenPos: Vec2): Vec3 | null {
    if (this._camera) {
      return this._camera.screenToWorld(new Vec3(screenPos.x, screenPos.y, 0));
    }
    // 回退：简易转换
    return new Vec3(screenPos.x - 375, screenPos.y - 667, 0);
  }
}
