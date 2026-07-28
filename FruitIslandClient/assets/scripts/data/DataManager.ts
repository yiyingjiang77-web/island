import { GamePlayer, Island, GameInitData, IslandArea, Land, Building, InventoryItem } from '../types';
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
  private _areas: IslandArea[] = [];
  private _lands: Land[] = [];
  private _buildings: Building[] = [];
  private _inventory: InventoryItem[] = [];

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
    this._areas = data.areas || [];
    this._lands = data.lands || [];
    this._buildings = data.buildings || [];
    this._inventory = data.inventory || [];

    this._loaded = true;

    console.log('[DataManager] 游戏数据加载完成:');
    console.log(`  岛主: ${this._player.nickname}  Lv.${this._player.level}`);
    console.log(`  金币: ${this._player.gold}  钻石: ${this._player.diamond}`);
    console.log(`  岛屿: ${this._island.islandName}  Lv.${this._island.level}`);

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

  get areas(): IslandArea[] {
    return this._areas;
  }

  get lands(): Land[] {
    return this._lands;
  }

  get buildings(): Building[] {
    return this._buildings;
  }

  get inventory(): InventoryItem[] {
    return this._inventory;
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
}
