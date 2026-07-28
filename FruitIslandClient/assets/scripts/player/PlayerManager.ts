import { _decorator, Component, Node, Graphics, Color, Vec3, find } from 'cc';
import { PlayerController } from './PlayerController';
import { ScaleConfig } from '../../configs/ScaleConfig';
import { MapConfig } from '../../configs/MapConfig';

const { ccclass } = _decorator;

/**
 * 玩家管理器 — Demo1.6
 *
 * 出生点使用 MapConfig.SPAWN_GX / SPAWN_GY。
 */
@ccclass('PlayerManager')
export class PlayerManager extends Component {
  private _playerNode: Node | null = null;
  private _controller: PlayerController | null = null;

  createPlayer(): Node {
    // 挂在 PlayerLayer 下
    let playerLayer = find('WorldRoot/PlayerLayer');
    if (!playerLayer) {
      playerLayer = new Node('PlayerLayer');
      find('WorldRoot')?.addChild(playerLayer);
    }

    this._playerNode = new Node('Player');
    playerLayer.addChild(this._playerNode);

    // 出生点：码头上方主通道
    const spawnX = MapConfig.gridToWorldX(MapConfig.SPAWN_GX);
    const spawnY = MapConfig.gridToWorldY(MapConfig.SPAWN_GY);
    this._playerNode.setPosition(spawnX, spawnY, 10);

    this._controller = this._playerNode.addComponent(PlayerController);
    this.drawShape(this._playerNode);

    console.log(`[PlayerManager] 玩家出生: grid(${MapConfig.SPAWN_GX},${MapConfig.SPAWN_GY}) → world(${spawnX.toFixed(0)},${spawnY.toFixed(0)})`);

    return this._playerNode;
  }

  get playerNode(): Node | null { return this._playerNode; }
  get controller(): PlayerController | null { return this._controller; }

  moveTo(worldX: number, worldY: number): void {
    if (this._controller) {
      this._controller.moveTo(new Vec3(worldX, worldY, 0));
    }
  }

  private drawShape(node: Node): void {
    let g = node.getComponent(Graphics);
    if (!g) g = node.addComponent(Graphics);
    g.clear();

    const s = ScaleConfig.PLAYER.SPRITE_SIZE;

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
