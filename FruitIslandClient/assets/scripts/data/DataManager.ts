import {
  CropConfig,
  CropLevelConfig,
  CropUnlockSource,
  FlowerConfig,
  FlowerLevelConfig,
  GamePlayer,
  Island,
  GameInitData,
  IslandGrowthVO,
  LandVO,
  InventoryItem,
  PlayerBeehive,
  PlayerCrop,
  PlayerCropGrant,
  PlayerFlowerRight,
  PlayerLevelConfig,
} from '../types';
import { http } from '../network/HttpClient';
import { Api } from '../network/Api';

/**
 * 数据中心 - Demo1
 *
 * 职责：
 * - 从服务器加载游戏数据
 * - 统一管理所有游戏状态
 * - 提供数据访问接口供 UI / Manager 使用
 *
 * 数据流：
 * GameManager → HttpClient → /game/init → DataManager → 各 Manager / UI
 */
export class DataManager {
  private static instance: DataManager;

  private _player: GamePlayer | null = null;
  private _island: Island | null = null;
  private _lands: LandVO[] = [];
  private _inventory: InventoryItem[] = [];
  private _cropConfigs: CropConfig[] = [];
  private _cropLevelConfigs: CropLevelConfig[] = [];
  private _cropUnlockSources: CropUnlockSource[] = [];
  private _playerCrops: PlayerCrop[] = [];
  private _cropGrants: PlayerCropGrant[] = [];
  private _playerLevelConfigs: PlayerLevelConfig[] = [];
  private _islandGrowth: IslandGrowthVO | null = null;
  private _flowerConfigs: FlowerConfig[] = [];
  private _flowerLevelConfigs: FlowerLevelConfig[] = [];
  private _playerFlowerRights: PlayerFlowerRight[] = [];
  private _playerBeehive: PlayerBeehive | null = null;

  private _loaded: boolean = false;

  static getInstance(): DataManager {
    if (!DataManager.instance) {
      DataManager.instance = new DataManager();
    }
    return DataManager.instance;
  }

  /** 是否已加载游戏数据 */
  get isLoaded(): boolean {
    return this._loaded;
  }

  // ==================== 数据加载 ====================

  /**
   * 从服务器加载游戏初始化数据
   *
   * 调用时机：登录成功后，进入 MainScene 前
   */
  async loadGameData(): Promise<boolean> {
    console.log('[DataManager] 正在从服务器加载游戏数据...');

    const result = await http.get<GameInitData>(Api.GAME_INIT);

    if (result.code !== 0) {
      console.error('[DataManager] 游戏数据加载失败:', result.message);
      return false;
    }

    const data = result.data;
    this._player = data.player;
    this._island = data.island;
    this._lands = data.lands || [];
    this._inventory = data.inventory || [];
    this._cropConfigs = data.cropConfigs || [];
    this._cropLevelConfigs = data.cropLevelConfigs || [];
    this._cropUnlockSources = data.cropUnlockSources || [];
    this._playerCrops = data.playerCrops || [];
    this._cropGrants = data.cropGrants || [];
    this._playerLevelConfigs = data.playerLevelConfigs || [];
    this._islandGrowth = data.islandGrowth || null;
    this._flowerConfigs = data.flowerConfigs || [];
    this._flowerLevelConfigs = data.flowerLevelConfigs || [];
    this._playerFlowerRights = data.playerFlowerRights || [];
    this._playerBeehive = data.playerBeehive || null;

    this._loaded = true;

    console.log('[DataManager] 游戏数据加载完成:');
    console.log(`  岛主: ${this._player.nickname}  Lv.${this._player.level}`);
    console.log(`  金币: ${this._player.gold}  钻石: ${this._player.diamond}`);
    console.log(`  岛屿: ${this._island.islandName}  Lv.${this._island.level}`);
    console.log(`  土地: ${this._lands.length} 块  背包: ${this._inventory.length} 种物品`);
    console.log(`  作物配置: ${this._cropConfigs.length} 种，永久权限: ${this._playerCrops.length} 种，限时权限: ${this._cropGrants.length} 种`);
    console.log(`  花卉配置: ${this._flowerConfigs.length} 种，花卉权限: ${this._playerFlowerRights.length} 种`);
    console.log(`  蜂箱: ${this._playerBeehive ? this._playerBeehive.beehiveCount + ' 个' : '无'}`);

    return true;
  }

  // ==================== 数据访问 ====================

  get player(): GamePlayer {
    if (!this._player) throw new Error('Player data not loaded');
    return this._player;
  }

  get island(): Island {
    if (!this._island) throw new Error('Island data not loaded');
    return this._island;
  }

  get lands(): LandVO[] {
    return this._lands;
  }

  get inventory(): InventoryItem[] {
    return this._inventory;
  }

  get cropConfigs(): CropConfig[] {
    return this._cropConfigs;
  }

  get cropLevelConfigs(): CropLevelConfig[] {
    return this._cropLevelConfigs;
  }

  get cropUnlockSources(): CropUnlockSource[] {
    return this._cropUnlockSources;
  }

  get playerCrops(): PlayerCrop[] {
    return this._playerCrops;
  }

  get cropGrants(): PlayerCropGrant[] {
    return this._cropGrants;
  }

  get playerLevelConfigs(): PlayerLevelConfig[] {
    return this._playerLevelConfigs;
  }

  get islandGrowth(): IslandGrowthVO | null {
    return this._islandGrowth;
  }

  get flowerConfigs(): FlowerConfig[] {
    return this._flowerConfigs;
  }

  get flowerLevelConfigs(): FlowerLevelConfig[] {
    return this._flowerLevelConfigs;
  }

  get playerFlowerRights(): PlayerFlowerRight[] {
    return this._playerFlowerRights;
  }

  get playerBeehive(): PlayerBeehive | null {
    return this._playerBeehive;
  }

  /** 本地更新金币（服务器同步后也更新这里） */
  updateGold(amount: number): void {
    if (this._player) {
      this._player.gold = amount;
    }
  }

  /** 本地更新钻石 */
  updateDiamond(amount: number): void {
    if (this._player) {
      this._player.diamond = amount;
    }
  }

  /** 查找花卉基础配置 */
  getFlowerConfig(flowerId: string): FlowerConfig | undefined {
    return this._flowerConfigs.find(f => f.flowerId === flowerId);
  }

  /** 查找花卉指定等级配置 */
  getFlowerLevelConfig(flowerId: string, level: number): FlowerLevelConfig | undefined {
    return this._flowerLevelConfigs.find(f => f.flowerId === flowerId && f.flowerLevel === level);
  }

  /** 查找玩家花卉种植权 */
  getPlayerFlowerRight(flowerId: string): PlayerFlowerRight | undefined {
    return this._playerFlowerRights.find(r => r.flowerId === flowerId);
  }

  /** 获取可种植的花卉列表（已购买种植权的） */
  getPlantableFlowers(): PlayerFlowerRight[] {
    return this._playerFlowerRights;
  }

  /** 本地更新蜂箱数据（购买/收取后无需全量刷新） */
  updateBeehive(beehive: PlayerBeehive): void {
    this._playerBeehive = beehive;
  }

  /** 本地更新花卉种植权（购买/升级后无需全量刷新） */
  upsertFlowerRight(right: PlayerFlowerRight): void {
    const idx = this._playerFlowerRights.findIndex(r => r.flowerId === right.flowerId);
    if (idx >= 0) {
      this._playerFlowerRights[idx] = right;
    } else {
      this._playerFlowerRights.push(right);
    }
  }
}
