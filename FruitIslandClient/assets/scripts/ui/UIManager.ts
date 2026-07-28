import { _decorator, Component, Node, Label, find } from 'cc';
import { DataManager } from '../data/DataManager';

const { ccclass, property } = _decorator;

/**
 * UI 管理器 - Demo1
 *
 * 职责：
 * - 管理 HUD（玩家信息、金币、钻石）
 * - 后续扩展：Loading / Toast / Dialog
 *
 * 数据来源：DataManager（服务器数据）
 */
@ccclass('UIManager')
export class UIManager extends Component {
  @property(Label)
  nicknameLabel: Label | null = null;

  @property(Label)
  levelLabel: Label | null = null;

  @property(Label)
  goldLabel: Label | null = null;

  @property(Label)
  diamondLabel: Label | null = null;

  onLoad(): void {
    this.refreshHUD();
  }

  /**
   * 刷新 HUD 显示
   *
   * 后续金币等数据变化时调用
   */
  refreshHUD(): void {
    const data = DataManager.getInstance();
    if (!data.isLoaded) return;

    const p = data.player;

    if (this.nicknameLabel) {
      this.nicknameLabel.string = p.nickname;
    }
    if (this.levelLabel) {
      this.levelLabel.string = `Lv.${p.level}`;
    }
    if (this.goldLabel) {
      this.goldLabel.string = `💰 ${p.gold}`;
    }
    if (this.diamondLabel) {
      this.diamondLabel.string = `💎 ${p.diamond}`;
    }
  }
}
