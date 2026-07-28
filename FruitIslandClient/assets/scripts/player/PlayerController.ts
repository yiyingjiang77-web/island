import { _decorator, Component, Vec3 } from 'cc';
import { PlayerConfig } from '../../configs/PlayerConfig';

const { ccclass } = _decorator;

/**
 * 玩家控制器 - Demo0
 *
 * 功能：moveTo(target) → update() 直线插值移动 → 到达停止
 *
 * 注意：PlayerController 不监听输入、不做边界判断
 * 它只负责"从 A 走到 B"这一件事
 */
@ccclass('PlayerController')
export class PlayerController extends Component {
  private _targetPos: Vec3 | null = null;

  get isMoving(): boolean {
    return this._targetPos !== null;
  }

  /**
   * 命令玩家移动到世界坐标
   * 调用方（GameManager）已做好边界检查
   */
  moveTo(target: Vec3): void {
    this._targetPos = target.clone();
  }

  /** 停止移动 */
  stop(): void {
    this._targetPos = null;
  }

  update(dt: number): void {
    if (!this._targetPos) return;

    const current = this.node.position;
    const dist = Vec3.distance(current, this._targetPos);

    if (dist <= PlayerConfig.ARRIVE_THRESHOLD) {
      this.node.setPosition(this._targetPos.x, this._targetPos.y, current.z);
      this._targetPos = null;
      return;
    }

    const step = PlayerConfig.MOVE_SPEED * dt;
    if (step >= dist) {
      this.node.setPosition(this._targetPos.x, this._targetPos.y, current.z);
      this._targetPos = null;
      return;
    }

    const dir = this._targetPos.clone().subtract(current).normalize();
    this.node.setPosition(current.x + dir.x * step, current.y + dir.y * step, current.z);
  }
}
