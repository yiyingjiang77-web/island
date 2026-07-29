import { _decorator, Color, Component, Graphics, Label, Node, UITransform } from 'cc';
import { DataManager } from '../data/DataManager';
import type { PlantableCropOption } from '../map/LandManager';

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

  private _messageLabel: Label | null = null;
  private _cropPicker: Node | null = null;

  onLoad(): void {
    this.ensureRuntimeHUD();
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

  /** 显示操作提示；后续可以替换为正式 Toast 预制体。 */
  showMessage(message: string): void {
    if (!this._messageLabel) {
      this.ensureRuntimeHUD();
    }
    if (this._messageLabel) {
      this._messageLabel.string = message;
    }
  }

  /**
   * 显示玩家当前真正可种植的品种。
   *
   * 永久作物显示“永久”，限时稀有作物显示剩余时间。
   * 点击品种只完成选择，必须再点击“种植”按钮才提交，避免误触直接播种。
   */
  showCropPicker(
    options: PlantableCropOption[],
    onSelect: (cropId: string) => void,
  ): void {
    this.closeCropPicker();

    const panel = new Node('CropPicker');
    panel.setPosition(0, 40, 100);
    this.node.addChild(panel);
    this._cropPicker = panel;
    let selectedCropId: string | null = null;

    const width = 540;
    const rowHeight = 64;
    const height = 120 + options.length * rowHeight;
    const transform = panel.addComponent(UITransform);
    transform.setContentSize(width, height);

    const background = panel.addComponent(Graphics);
    background.fillColor = new Color(35, 50, 55, 245);
    background.rect(-width / 2, -height / 2, width, height);
    background.fill();
    background.strokeColor = new Color(150, 215, 135, 255);
    background.lineWidth = 3;
    background.rect(-width / 2, -height / 2, width, height);
    background.stroke();

    const title = this.createPanelLabel(panel, 'Title', 0, height / 2 - 38, 26, width - 40);
    title.string = '🌱 选择要种植的品种';

    options.forEach((option, index) => {
      const button = new Node(`Crop_${option.cropId}`);
      button.setPosition(0, height / 2 - 88 - index * rowHeight, 1);
      panel.addChild(button);

      const buttonTransform = button.addComponent(UITransform);
      buttonTransform.setContentSize(width - 48, 52);
      const buttonGraphics = button.addComponent(Graphics);
      buttonGraphics.fillColor = option.temporary
        ? new Color(105, 70, 145, 245)
        : new Color(65, 125, 70, 245);
      buttonGraphics.rect(-(width - 48) / 2, -26, width - 48, 52);
      buttonGraphics.fill();

      const label = this.createPanelLabel(button, 'Label', 0, 0, 21, width - 70);
      const expiry = option.temporary
        ? `限时 · 剩余${this.formatRemaining(option.validUntil)}`
        : '永久';
      label.string =
        `${this.getCropEmoji(option.cropId)} ${option.name}  Lv.${option.cropLevel}  ${expiry}`;

      button.on(Node.EventType.TOUCH_END, () => {
        selectedCropId = option.cropId;
        title.string =
          `已选择 ${this.getCropEmoji(option.cropId)} ${option.name}，点击“种植”确认`;
      });
    });

    const confirm = new Node('ConfirmPlant');
    confirm.setPosition(100, -height / 2 + 30, 1);
    panel.addChild(confirm);
    confirm.addComponent(UITransform).setContentSize(180, 42);
    const confirmGraphics = confirm.addComponent(Graphics);
    confirmGraphics.fillColor = new Color(65, 155, 75, 255);
    confirmGraphics.rect(-90, -21, 180, 42);
    confirmGraphics.fill();
    const confirmLabel = this.createPanelLabel(confirm, 'Label', 0, 0, 19, 180);
    confirmLabel.string = '🌱 种植';
    confirm.on(Node.EventType.TOUCH_END, () => {
      if (!selectedCropId) {
        this.showMessage('请先选择一种种子');
        return;
      }
      const cropId = selectedCropId;
      this.closeCropPicker();
      onSelect(cropId);
    });

    const cancel = new Node('Cancel');
    cancel.setPosition(-100, -height / 2 + 30, 1);
    panel.addChild(cancel);
    cancel.addComponent(UITransform).setContentSize(180, 42);
    const cancelLabel = this.createPanelLabel(cancel, 'Label', 0, 0, 19, 180);
    cancelLabel.string = '取消';
    cancel.on(Node.EventType.TOUCH_END, () => this.closeCropPicker());
  }

  closeCropPicker(): void {
    if (this._cropPicker) {
      this._cropPicker.destroy();
      this._cropPicker = null;
    }
  }

  /**
   * 当场景里还没有设计好的 HUD 时，创建一套最小可运行 HUD。
   * 已在编辑器绑定的 Label 不会被覆盖。
   */
  private ensureRuntimeHUD(): void {
    this.nicknameLabel = this.nicknameLabel || this.createLabel('Nickname', -250, 585, 24);
    this.levelLabel = this.levelLabel || this.createLabel('Level', -250, 550, 22);
    this.goldLabel = this.goldLabel || this.createLabel('Gold', 170, 585, 24);
    this.diamondLabel = this.diamondLabel || this.createLabel('Diamond', 170, 550, 22);
    this._messageLabel = this._messageLabel || this.createLabel('Message', 0, -580, 24, 680);
  }

  private createLabel(
    name: string,
    x: number,
    y: number,
    fontSize: number,
    width: number = 280,
  ): Label {
    let labelNode = this.node.getChildByName(name);
    if (!labelNode) {
      labelNode = new Node(name);
      this.node.addChild(labelNode);
    }
    labelNode.setPosition(x, y, 0);

    const transform = labelNode.getComponent(UITransform)
      || labelNode.addComponent(UITransform);
    transform.setContentSize(width, 50);

    const label = labelNode.getComponent(Label) || labelNode.addComponent(Label);
    label.fontSize = fontSize;
    label.lineHeight = fontSize + 4;
    label.color = new Color(255, 255, 255, 255);
    return label;
  }

  private createPanelLabel(
    parent: Node,
    name: string,
    x: number,
    y: number,
    fontSize: number,
    width: number,
  ): Label {
    const node = new Node(name);
    node.setPosition(x, y, 2);
    parent.addChild(node);
    node.addComponent(UITransform).setContentSize(width, fontSize + 14);
    const label = node.addComponent(Label);
    label.fontSize = fontSize;
    label.lineHeight = fontSize + 4;
    label.color = new Color(255, 255, 255, 255);
    return label;
  }

  private formatRemaining(validUntil?: string): string {
    if (!validUntil) return '--';
    const seconds = Math.max(
      0,
      Math.ceil((new Date(validUntil).getTime() - Date.now()) / 1000),
    );
    if (seconds >= 86400) return `${Math.ceil(seconds / 86400)}天`;
    if (seconds >= 3600) return `${Math.ceil(seconds / 3600)}小时`;
    return `${Math.ceil(seconds / 60)}分钟`;
  }

  private getCropEmoji(cropId: string): string {
    const icons: Record<string, string> = {
      strawberry: '🍓',
      cabbage: '🥬',
      carrot: '🥕',
      tomato: '🍅',
      potato: '🥔',
      chili: '🌶️',
      corn: '🌽',
      moonberry: '🫐',
    };
    return icons[cropId] ?? '🌿';
  }
}
