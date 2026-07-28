import { _decorator, Component, Node, Vec3 } from 'cc';

const { ccclass, property } = _decorator;

/**
 * 摄像机跟随 - Demo0 版本
 *
 * 功能：
 * - 跟随目标节点（玩家）
 * - 保持固定偏移
 * - Demo0 用直接跟随，后续改为平滑插值
 */
@ccclass('CameraFollow')
export class CameraFollow extends Component {
  /** 跟随目标 */
  @property(Node)
  target: Node | null = null;

  /** 偏移量（Z 保持摄像机距离） */
  @property()
  offset: Vec3 = new Vec3(0, 0, 1000);

  onLoad(): void {
    if (this.target) {
      this.snapToTarget();
    }
  }

  /**
   * Demo0：每帧直接跟随目标
   * 后续版本可改为平滑插值：Vec3.lerp(current, target + offset, dt * smoothSpeed)
   */
  update(_dt: number): void {
    if (!this.target) return;

    const targetPos = this.target.position;
    this.node.setPosition(
      targetPos.x + this.offset.x,
      targetPos.y + this.offset.y,
      this.offset.z,
    );
  }

  /** 立即对齐到目标 */
  private snapToTarget(): void {
    if (!this.target) return;
    const tp = this.target.position;
    this.node.setPosition(tp.x + this.offset.x, tp.y + this.offset.y, this.offset.z);
  }

  /** 运行时更换跟随目标 */
  setTarget(target: Node): void {
    this.target = target;
    this.snapToTarget();
  }
}
