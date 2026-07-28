import { _decorator, Component, Node, Vec3, find, Camera } from 'cc';
import { http } from '../network/HttpClient';
import { Api } from '../network/Api';
import { DataManager } from '../data/DataManager';
import { MapManager } from '../map/MapManager';
import { PlayerManager } from '../player/PlayerManager';
import { PlayerController } from '../player/PlayerController';
import { CameraManager } from '../camera/CameraManager';
import { InputManager } from '../input/InputManager';
import { UIManager } from '../ui/UIManager';
import { GameConfig } from '../../configs/GameConfig';
import { MapConfig } from '../../configs/MapConfig';

const { ccclass } = _decorator;

/**
 * 游戏入口 — Demo2.4
 *
 * 启动流程：
 *   登录 → /game/init → 场景分层 → MapManager.init() → 玩家 → 摄像机 → 输入 → UI
 *
 * 场景结构：
 *   Canvas
 *   ├── WorldRoot           ← 固定世界坐标
 *   │   ├── TerrainLayer    ← 地形（水/沙/草）
 *   │   ├── LandLayer       ← 农田
 *   │   ├── BuildingLayer   ← 建筑
 *   │   ├── NPCLayer        ← NPC（预留）
 *   │   └── PlayerLayer     ← 玩家角色
 *   ├── MainCamera
 *   └── UIRoot              ← 屏幕固定
 */
@ccclass('GameManager')
export class GameManager extends Component {
  private _worldRoot: Node | null = null;
  private _mapManager: MapManager | null = null;
  private _playerNode: Node | null = null;
  private _playerController: PlayerController | null = null;
  private _cameraManager: CameraManager | null = null;
  private _inputManager: InputManager | null = null;

  onLoad(): void {
    this.startGame();
  }

  // ==================== 启动 ====================

  private async startGame(): Promise<void> {
    console.log('[GameManager] Demo2.4 启动 🏝️');

    let dataLoaded = false;
    const savedToken = http.getToken();
    if (savedToken) {
      dataLoaded = await DataManager.getInstance().loadGameData();
      if (!dataLoaded) {
        http.clearToken();
        console.warn('[GameManager] 已保存 Token 无效，尝试重新登录');
      }
    }

    if (!dataLoaded) {
      const ok = await this.login();
      if (ok) {
        dataLoaded = await DataManager.getInstance().loadGameData();
      }
    }

    if (!dataLoaded) {
      console.warn('[GameManager] 服务器不可用，进入离线地图预览模式');
    }

    this.initGameWorld();
  }

  private async login(): Promise<boolean> {
    const code = typeof wx !== 'undefined'
      ? await new Promise<string>((resolve, reject) =>
          (wx as any).login({ success: (r: any) => resolve(r.code), fail: reject }))
      : 'dev_mock_code_' + Date.now();

    const result = await http.post(Api.WECHAT_LOGIN, { code });
    if (result.code === 0) { http.setToken(result.data.token); return true; }
    return false;
  }

  // ==================== 世界初始化 ====================

  private initGameWorld(): void {
    this.createSceneStructure();
    this.initMap();
    this.initPlayer();
    this.initCamera();
    this.initInput();
    this.initUI();

    console.log('[GameManager] Demo2.4 世界就绪 ✅');
  }

  private createSceneStructure(): void {
    const canvas = find('Canvas') || this.node;

    // WorldRoot
    this._worldRoot = find('WorldRoot');
    if (!this._worldRoot) {
      this._worldRoot = new Node('WorldRoot');
      canvas.addChild(this._worldRoot);
    }

    // 子图层
    for (const name of ['TerrainLayer', 'LandLayer', 'BuildingLayer', 'NPCLayer', 'PlayerLayer']) {
      if (!this._worldRoot.getChildByName(name)) {
        this._worldRoot.addChild(new Node(name));
      }
    }

    // UIRoot
    if (!find('UIRoot')) {
      canvas.addChild(new Node('UIRoot'));
    }

    // Camera
    if (!find('MainCamera')) {
      const cam = new Node('MainCamera');
      cam.addComponent(Camera);
      canvas.addChild(cam);
      cam.setPosition(MapConfig.WORLD_CENTER, MapConfig.WORLD_CENTER, 1000);
    }
  }

  private initMap(): void {
    let mapNode = find('WorldRoot/MapManager');
    if (!mapNode) {
      mapNode = new Node('MapManager');
      this._worldRoot!.addChild(mapNode);
    }
    this._mapManager = mapNode.getComponent(MapManager) || mapNode.addComponent(MapManager);
    this._mapManager.init(this._worldRoot!);
  }

  private initPlayer(): void {
    const playerLayer = this._worldRoot!.getChildByName('PlayerLayer')!;
    let pmNode = playerLayer.getChildByName('PlayerManager');
    if (!pmNode) {
      pmNode = new Node('PlayerManager');
      playerLayer.addChild(pmNode);
    }
    const pm = pmNode.getComponent(PlayerManager) || pmNode.addComponent(PlayerManager);
    const playerNode = pm.createPlayer();
    this._playerNode = playerNode;
    this._playerController = playerNode.getComponent(PlayerController);
    this._playerController?.setWalkableChecker((x, y) =>
      this._mapManager?.isWalkable(x, y) ?? true);
  }

  private initCamera(): void {
    const camNode = find('MainCamera')!;
    this._cameraManager = camNode.getComponent(CameraManager) || camNode.addComponent(CameraManager);
    if (this._playerNode) this._cameraManager.setTarget(this._playerNode);
  }

  private initInput(): void {
    this._inputManager = this.node.getComponent(InputManager)
      || this.node.addComponent(InputManager);

    if (this._cameraManager?.camera) {
      this._inputManager.setCamera(this._cameraManager.camera);
    }

    // 点击 → 移动
    this._inputManager.onClickMap((worldPos: Vec3) => {
      this.handleClick(worldPos);
    });

    // 拖动 → 摄像机
    this._inputManager.onDragCamera((delta) => {
      this._cameraManager?.onDragMove(delta);
    });
    this._inputManager.onDragStart(() => {
      this._cameraManager?.onDragStart();
    });
    this._inputManager.onDragEnd(() => {
      this._cameraManager?.onDragEnd();
    });
  }

  private initUI(): void {
    const uiRoot = find('UIRoot');
    if (!uiRoot) return;
    let hud = uiRoot.getChildByName('TopHUD');
    if (!hud) { hud = new Node('TopHUD'); uiRoot.addChild(hud); }
    hud.getComponent(UIManager) || hud.addComponent(UIManager);
  }

  // ==================== 运行时 ====================

  private handleClick(worldPos: Vec3): void {
    if (!this._mapManager || !this._playerController) return;

    if (!this._mapManager.isWalkable(worldPos.x, worldPos.y)) {
      if (GameConfig.DEBUG) console.log('[GameManager] 不可通行');
      return;
    }

    const clamped = this._mapManager.clampToWalkable(worldPos.x, worldPos.y);
    this._cameraManager?.focusTarget();
    this._playerController.moveTo(clamped);
  }
}
