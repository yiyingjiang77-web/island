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
 * 游戏入口管理器 — Demo1.6 世界地图版
 *
 * 场景结构：
 *   Canvas
 *   ├── WorldRoot          ← 跟随摄像机移动
 *   │   ├── TerrainLayer
 *   │   ├── LandLayer
 *   │   ├── BuildingLayer  (预留)
 *   │   ├── NPCLayer       (预留)
 *   │   └── PlayerLayer
 *   ├── MainCamera
 *   └── UIRoot             ← 屏幕固定
 *
 * 启动流程：
 *   登录 → /game/init → 创建 WorldRoot+图层 → 地形 → 占位岛 → 玩家 → 摄像机 → 输入 → UI
 */
@ccclass('GameManager')
export class GameManager extends Component {
  private _worldRoot: Node | null = null;
  private _mapManager: MapManager | null = null;
  private _playerManager: PlayerManager | null = null;
  private _playerController: PlayerController | null = null;
  private _cameraManager: CameraManager | null = null;
  private _inputManager: InputManager | null = null;

  onLoad(): void {
    this.startGame();
  }

  // ==================== 启动流程 ====================

  private async startGame(): Promise<void> {
    console.log('[GameManager] Demo1.6 世界地图启动 🏝️');

    // 1. 尝试自动登录
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

  private async login(): Promise<boolean> {
    let code: string;
    if (typeof wx !== 'undefined') {
      code = await new Promise<string>((resolve, reject) => {
        (wx as any).login({
          success: (res: { code: string }) => resolve(res.code),
          fail: (err: any) => reject(err),
        });
      });
    } else {
      code = 'dev_mock_code_' + Date.now();
    }

    const result = await http.post(Api.WECHAT_LOGIN, { code });
    if (result.code === 0) {
      http.setToken(result.data.token);
      console.log('[GameManager] 登录成功');
      return true;
    }
    if (result.code === -1) {
      console.warn('[GameManager] 服务器不可用，使用离线模式');
    }
    return false;
  }

  // ==================== 世界初始化 ====================

  private initGameWorld(): void {
    this.initSceneStructure();
    this.initTerrain();
    this.initPlaceholderIsland();
    this.initPlayer();
    this.initCamera();
    this.initInput();
    this.initUI();

    console.log('[GameManager] Demo1.6 世界地图初始化完成！🏝️');
    console.log(`  世界: ${MapConfig.WORLD_SIZE}×${MapConfig.WORLD_SIZE}px`);
    console.log(`  网格: ${MapConfig.GRID_COUNT}×${MapConfig.GRID_COUNT}  (${MapConfig.TILE_SIZE}px/tile)`);
    console.log(`  可行走区域: grid(${MapConfig.GRASS_START}~${MapConfig.GRASS_END})`);
    console.log('  操作: 点击移动 | 拖拽屏幕');
  }

  /** 创建 WorldRoot + UIRoot + 各 Layer */
  private initSceneStructure(): void {
    const canvas = find('Canvas') || this.node;

    // WorldRoot — 跟随摄像机
    if (!find('WorldRoot')) {
      this._worldRoot = new Node('WorldRoot');
      canvas.addChild(this._worldRoot);
    } else {
      this._worldRoot = find('WorldRoot');
    }

    // 子图层
    const layers = ['TerrainLayer', 'LandLayer', 'BuildingLayer', 'NPCLayer', 'PlayerLayer'];
    for (const name of layers) {
      if (!find(`WorldRoot/${name}`)) {
        const node = new Node(name);
        this._worldRoot!.addChild(node);
      }
    }

    // UIRoot — 屏幕固定
    if (!find('UIRoot')) {
      const uiRoot = new Node('UIRoot');
      canvas.addChild(uiRoot);
    }

    // 摄像机
    if (!find('MainCamera')) {
      const camNode = new Node('MainCamera');
      camNode.addComponent(Camera);
      canvas.addChild(camNode);
      camNode.setPosition(MapConfig.WORLD_CENTER, MapConfig.WORLD_CENTER, 1000);
    }
  }

  private initTerrain(): void {
    const terrainLayer = find('WorldRoot/TerrainLayer')!;
    this._mapManager = terrainLayer.getComponent(MapManager) || terrainLayer.addComponent(MapManager);
    this._mapManager.generateTerrain();
  }

  private initPlaceholderIsland(): void {
    const landLayer = find('WorldRoot/LandLayer');
    if (this._mapManager) {
      this._mapManager.createPlaceholderIsland(landLayer);
    }
  }

  private initPlayer(): void {
    const playerLayer = find('WorldRoot/PlayerLayer')!;
    let pmNode = find('WorldRoot/PlayerLayer/PlayerManager');
    if (!pmNode) {
      pmNode = new Node('PlayerManager');
      playerLayer.addChild(pmNode);
    }
    this._playerManager = pmNode.getComponent(PlayerManager) || pmNode.addComponent(PlayerManager);

    const playerNode = this._playerManager.createPlayer();
    this._playerController = playerNode.getComponent(PlayerController);
  }

  private initCamera(): void {
    const camNode = find('MainCamera')!;
    this._cameraManager = camNode.getComponent(CameraManager) || camNode.addComponent(CameraManager);

    // 摄像机跟随 WorldRoot
    if (this._worldRoot) {
      this._cameraManager.setTarget(this._worldRoot);
    }
  }

  private initInput(): void {
    this._inputManager = this.node.getComponent(InputManager)
      || this.node.addComponent(InputManager);

    // 设置摄像机引用（供 InputManager 坐标转换）
    if (this._cameraManager?.camera) {
      this._inputManager.setCamera(this._cameraManager.camera);
    }

    // 点击移动
    this._inputManager.onClickMap((worldPos: Vec3) => {
      this.handleMapClick(worldPos);
    });

    // 拖动摄像机
    this._inputManager.onDragCamera((delta) => {
      if (this._cameraManager) {
        this._cameraManager.onDragMove(delta);
      }
    });
  }

  private initUI(): void {
    const uiRoot = find('UIRoot');
    if (!uiRoot) {
      console.log('[GameManager] 未找到 UIRoot 节点');
      return;
    }

    // TopHUD
    let hudNode = find('UIRoot/TopHUD');
    if (!hudNode) {
      hudNode = new Node('TopHUD');
      uiRoot.addChild(hudNode);
    }
    hudNode.getComponent(UIManager) || hudNode.addComponent(UIManager);
  }

  // ==================== 运行时 ====================

  private handleMapClick(worldPos: Vec3): void {
    if (!this._mapManager || !this._playerController) return;

    if (!this._mapManager.isInBounds(worldPos.x, worldPos.y)) {
      this.log('超出地图边界');
      return;
    }

    if (!this._mapManager.isWalkable(worldPos.x, worldPos.y)) {
      this.log('不可行走（水域/沙滩）');
      return;
    }

    const clamped = this._mapManager.clampToWalkable(worldPos.x, worldPos.y);
    this._playerController.moveTo(new Vec3(clamped.x, clamped.y, 0));
  }

  private log(msg: string): void {
    if (GameConfig.DEBUG) console.log(`[GameManager] ${msg}`);
  }
}
