import { _decorator, Component, Node, Graphics, Color, Vec3, find } from 'cc';
import { DataManager } from '../data/DataManager';
import { PlayerController } from './PlayerController';
import { PlayerConfig } from '../../configs/PlayerConfig';

const { ccclass } = _decorator;

/**
 * 玩家管理器 - Demo1
 *
 * 职责：
 * - 根据 DataManager 中的玩家数据创建玩家节点
 * - 管理玩家生命周期
 * - 对外暴露玩家操作接口
 *
 * 注意：PlayerManager 不处理输入，不处理移动逻辑（那些在 PlayerController）
 */
@ccclass('PlayerManager')
export class PlayerManager extends Component {
  private _playerNode: Node | null = null;
  private _controller: PlayerController | null = null;

  /**
   * 根据服务器数据创建玩家
   *
   * 进入 MainScene 后由 GameManager 调用
   */
  createPlayer(): Node {
    const data = DataManager.getInstance();
    const playerData = data.player;

    // 确保 PlayerRoot 存在
    let playerRoot = find('PlayerRoot');
    if (!playerRoot) {
      playerRoot = new Node('PlayerRoot');
      find('Canvas')?.addChild(playerRoot);
    }

    // 创建玩家节点
    this._playerNode = new Node('Player');
    playerRoot.addChild(this._playerNode);

    // 初始位置
    this._playerNode.setPosition(PlayerConfig.SPAWN_X, PlayerConfig.SPAWN_Y, 10);

    // 挂载移动控制器
    this._controller = this._playerNode.addComponent(PlayerController);

    // 绘制角色
    this.drawShape(this._playerNode);

    console.log(`[PlayerManager] 玩家创建完成: ${playerData.nickname} Lv.${playerData.level}`);

    return this._playerNode;
  }

  /** 获取玩家节点 */
  get playerNode(): Node | null {
    return this._playerNode;
  }

  /** 获取移动控制器 */
  get controller(): PlayerController | null {
    return this._controller;
  }

  /** 命令玩家移动到世界坐标 */
  moveTo(worldX: number, worldY: number): void {
    if (this._controller) {
      this._controller.moveTo(new Vec3(worldX, worldY, 0));
    }
  }

  private drawShape(node: Node): void {
    let g = node.getComponent(Graphics);
    if (!g) g = node.addComponent(Graphics);
    g.clear();

    const s = PlayerConfig.SPRITE_SIZE;

    // 身体
    g.fillColor = new Color(255, 200, 100);
    g.circle(0, 0, s * 0.6);
    g.fill();

    // 帽子
    g.fillColor = new Color(220, 80, 60);
    g.moveTo(-s * 0.5, s * 0.3);
    g.lineTo(0, s * 1.0);
    g.lineTo(s * 0.5, s * 0.3);
    g.close();
    g.fill();

    // 边框
    g.strokeColor = new Color(0, 0, 0, 80);
    g.lineWidth = 1;
    g.circle(0, 0, s * 0.6);
    g.stroke();
  }
}
