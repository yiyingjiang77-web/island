import { _decorator, Component, Vec3 } from 'cc';
import { ScaleConfig } from '../../configs/ScaleConfig';

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
  private _isWalkable: ((x: number, y: number) => boolean) | null = null;

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

  /** 注入地图碰撞检查，避免移动过程穿过建筑 */
  setWalkableChecker(checker: (x: number, y: number) => boolean): void {
    this._isWalkable = checker;
  }

  /** 停止移动 */
  stop(): void {
    this._targetPos = null;
  }

  update(dt: number): void {
    if (!this._targetPos) return;

    const current = this.node.position;
    const dist = Vec3.distance(current, this._targetPos);

    if (dist <= ScaleConfig.PLAYER.ARRIVE_THRESHOLD) {
      this.node.setPosition(this._targetPos.x, this._targetPos.y, current.z);
      this._targetPos = null;
      return;
    }

    const step = ScaleConfig.PLAYER.WALK_SPEED * dt;
    if (step >= dist) {
      this.node.setPosition(this._targetPos.x, this._targetPos.y, current.z);
      this._targetPos = null;
      return;
    }

    const dir = this._targetPos.clone().subtract(current).normalize();
    const nextX = current.x + dir.x * step;
    const nextY = current.y + dir.y * step;

    if (!this._isWalkable || this._isWalkable(nextX, nextY)) {
      this.node.setPosition(nextX, nextY, current.z);
      return;
    }

    // 允许沿障碍边缘滑动；两个方向都不可走时停止。
    if (this._isWalkable(nextX, current.y)) {
      this.node.setPosition(nextX, current.y, current.z);
      return;
    }
    if (this._isWalkable(current.x, nextY)) {
      this.node.setPosition(current.x, nextY, current.z);
      return;
    }

    this.stop();
  }
}
