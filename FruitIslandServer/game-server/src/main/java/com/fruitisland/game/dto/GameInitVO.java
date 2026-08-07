package com.fruitisland.game.dto;

import com.fruitisland.game.entity.CropConfig;
import com.fruitisland.game.entity.CropLevelConfig;
import com.fruitisland.game.entity.CropUnlockSource;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.Inventory;
import com.fruitisland.game.entity.Island;
import com.fruitisland.game.entity.PlayerCrop;
import com.fruitisland.game.entity.PlayerCropGrant;
import com.fruitisland.game.entity.PlayerLevelConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * /game/init 接口返回数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameInitVO implements Serializable {

    /** 玩家信息 */
    private GamePlayer player;

    /** 岛屿信息 */
    private Island island;

    /** 土地列表 */
    private List<LandVO> lands;

    /** 背包物品 */
    private List<Inventory> inventory;

    /** 作物基础配置（名称、稀有度、玩家解锁等级等）。 */
    private List<CropConfig> cropConfigs;

    /** 作物各等级数值配置（成熟时间、产量、升级金币）。 */
    private List<CropLevelConfig> cropLevelConfigs;

    /** 作物永久种植权的可用获得渠道。 */
    private List<CropUnlockSource> cropUnlockSources;

    /** 玩家永久拥有的作物及其当前等级。 */
    private List<PlayerCrop> playerCrops;

    /** 玩家当前有效的限时稀有作物权限。 */
    private List<PlayerCropGrant> cropGrants;

    /** 玩家等级成长配置，用于客户端经验条和升级预览。 */
    private List<PlayerLevelConfig> playerLevelConfigs;

    /** 小岛累计经验、等级阈值及逐级固定奖励状态。 */
    private IslandGrowthVO islandGrowth;

    /** 本次登录自动补结并发放的历史满意度礼品。 */
    private List<SatisfactionStatusVO.History> autoSettledSatisfactionRewards;

    public static GameInitVO of(
            GamePlayer player,
            Island island,
            List<LandVO> lands,
            List<Inventory> inventory,
            List<CropConfig> cropConfigs,
            List<CropLevelConfig> cropLevelConfigs,
            List<CropUnlockSource> cropUnlockSources,
            List<PlayerCrop> playerCrops,
            List<PlayerCropGrant> cropGrants,
            List<PlayerLevelConfig> playerLevelConfigs,
            IslandGrowthVO islandGrowth,
            List<SatisfactionStatusVO.History> autoSettledSatisfactionRewards
    ) {
        GameInitVO vo = new GameInitVO();
        vo.setPlayer(player);
        vo.setIsland(island);
        vo.setLands(lands);
        vo.setInventory(inventory);
        vo.setCropConfigs(cropConfigs);
        vo.setCropLevelConfigs(cropLevelConfigs);
        vo.setCropUnlockSources(cropUnlockSources);
        vo.setPlayerCrops(playerCrops);
        vo.setCropGrants(cropGrants);
        vo.setPlayerLevelConfigs(playerLevelConfigs);
        vo.setIslandGrowth(islandGrowth);
        vo.setAutoSettledSatisfactionRewards(autoSettledSatisfactionRewards);
        return vo;
    }
}
