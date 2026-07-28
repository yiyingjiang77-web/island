import { _decorator, Component, Node, Vec3, find } from 'cc';
import { http } from '../network/HttpClient';
import { Api } from '../network/Api';
import { DataManager } from '../data/DataManager';
import { MapManager } from '../map/MapManager';
import { PlayerManager } from '../player/PlayerManager';
import { PlayerController } from '../player/PlayerController';
import { CameraFollow } from '../camera/CameraFollow';
import { InputManager } from '../input/InputManager';
import { UIManager } from '../ui/UIManager';
import { GameConfig } from '../../configs/GameConfig';
import { MapConfig } from '../../configs/MapConfig';
import { PlayerConfig } from '../../configs/PlayerConfig';

const { ccclass } = _decorator;

/**
 * 游戏入口管理器 - Demo1 数据驱动版
 *
 * 流程：
 * 启动 → 登录 → /game/init → DataManager → 创建场景内容 → 进入游戏
 *
 * 职责：
 * - 登录流程（开发模式自动登录）
 * - 从服务器加载数据
 * - 协调各 Manager 初始化
 * - 运行时中枢：InputManager → handleMapClick → PlayerController
 */
@ccclass('GameManager')
export class GameManager extends Component {
  private _mapManager: MapManager | null = null;
  private _playerManager: PlayerManager | null = null;
  private _playerController: PlayerController | null = null;
  private _inputManager: InputManager | null = null;

  onLoad(): void {
    this.startGame();
  }

  // ==================== 启动流程 ====================

  private async startGame(): Promise<void> {
    console.log('[GameManager] Demo1 启动 🏝️');

    // 1. 尝试用已保存的 Token 加载数据
    const savedToken = http.getToken();
    if (savedToken) {
      console.log('[GameManager] 检测到已保存 Token，尝试自动登录...');
      const loaded = await DataManager.getInstance().loadGameData();
      if (loaded) {
        this.initGameWorld();
        return;
      }
      console.log('[GameManager] Token 失效，重新登录...');
      http.clearToken();
    }

    // 2. 登录
    const loggedIn = await this.login();
    if (!loggedIn) {
      console.error('[GameManager] 登录失败！');
      return;
    }

    // 3. 加载游戏数据
    const loaded = await DataManager.getInstance().loadGameData();
    if (!loaded) {
      console.error('[GameManager] 游戏数据加载失败！');
      return;
    }

    // 4. 初始化游戏世界
    this.initGameWorld();
  }

  /**
   * 登录
   *
   * Demo1: 开发模式自动登录（mock code）
   * 微信小游戏: wx.login → code → 服务端
   */
  private async login(): Promise<boolean> {
    let code: string;

    if (typeof wx !== 'undefined') {
      // 微信小游戏
      code = await new Promise<string>((resolve, reject) => {
        (wx as any).login({
          success: (res: { code: string }) => resolve(res.code),
          fail: (err: any) => reject(err),
        });
      });
    } else {
      // 浏览器开发模式
      code = 'dev_mock_code_' + Date.now();
      console.log('[GameManager] 开发模式，使用 mock code:', code);
    }

    const result = await http.post(Api.WECHAT_LOGIN, { code });

    if (result.code === 0) {
      http.setToken(result.data.token);
      console.log('[GameManager] 登录成功:', result.data.nickname);
      return true;
    }

    // 服务器不可用时，离线模式（纯Demo0体验）
    if (result.code === -1) {
      console.warn('[GameManager] 服务器不可用，使用离线模式');
      return false;
    }

    return false;
  }

  // ==================== 世界初始化 ====================

  /**
   * 初始化游戏世界
   *
   * 顺序：MapRoot/PlayerRoot → Map → Player → Camera → Input → UI
   */
  private initGameWorld(): void {
    this.initRoots();
    this.initMap();
    this.initPlayer();
    this.initCamera();
    this.initInput();
    this.initUI();

    console.log('[GameManager] Demo1 初始化完成！🏝️');
    console.log('  数据驱动：玩家、岛屿信息来自服务器');
    console.log('  点击草地移动，点击水/沙滩不移动');
  }

  private initRoots(): void {
    const canvas = find('Canvas') || this.node;
    for (const name of ['MapRoot', 'PlayerRoot']) {
      if (!find(name)) {
        const node = new Node(name);
        canvas.addChild(node);
      }
    }
  }

  private initMap(): void {
    let mapNode = find('MapRoot/Map');
    if (!mapNode) {
      mapNode = new Node('Map');
      find('MapRoot')!.addChild(mapNode);
    }
    this._mapManager = mapNode.getComponent(MapManager) || mapNode.addComponent(MapManager);
  }

  private initPlayer(): void {
    let playerManagerNode = find('PlayerRoot/PlayerManager');
    if (!playerManagerNode) {
      playerManagerNode = new Node('PlayerManager');
      find('PlayerRoot')!.addChild(playerManagerNode);
    }
    this._playerManager = playerManagerNode.getComponent(PlayerManager)
      || playerManagerNode.addComponent(PlayerManager);

    const playerNode = this._playerManager.createPlayer();
    this._playerController = playerNode.getComponent(PlayerController);
  }

  private initCamera(): void {
    const camNode = find('MainCamera') || find('Camera');
    if (!camNode) {
      console.warn('[GameManager] 未找到摄像机节点');
      return;
    }

    const follow = camNode.getComponent(CameraFollow) || camNode.addComponent(CameraFollow);
    const playerNode = find('PlayerRoot/Player');
    if (playerNode) {
      follow.setTarget(playerNode);
    }
  }

  /**
   * 初始化输入 —— 核心路由
   *
   * InputManager → GameManager.handleMapClick → 边界检查 → PlayerController.moveTo
   */
  private initInput(): void {
    this._inputManager = this.node.getComponent(InputManager)
      || this.node.addComponent(InputManager);

    this._inputManager.onClickMap((worldPos: Vec3) => {
      this.handleMapClick(worldPos);
    });
  }

  private initUI(): void {
    // UIManager 在场景中的 UIRoot 上（在编辑器中手动创建）
    // 如果没有，运行时创建基本 HUD
    const uiRoot = find('UIRoot');
    if (!uiRoot) {
      console.log('[GameManager] 未找到 UIRoot 节点（可在编辑器中创建）');
    }
  }

  // ==================== 运行时中枢 ====================

  /**
   * 处理地图点击
   *
   * 未来扩展：点击建筑→打开UI、点击土地→种植 等，都在这里分发
   */
  private handleMapClick(worldPos: Vec3): void {
    if (!this._mapManager || !this._playerController) return;

    if (!this._mapManager.isInBounds(worldPos)) {
      this.log(`超出地图边界，忽略`);
      return;
    }

    if (!this._mapManager.isWalkable(worldPos)) {
      this.log(`目标不可走（水/沙滩），忽略`);
      return;
    }

    const clamped = this._mapManager.clampToWalkable(worldPos);
    this._playerController.moveTo(clamped);
  }

  private log(msg: string): void {
    if (GameConfig.DEBUG) {
      console.log(`[GameManager] ${msg}`);
    }
  }
}
