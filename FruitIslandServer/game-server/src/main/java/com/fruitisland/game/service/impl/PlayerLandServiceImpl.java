package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.dto.ExpGainResult;
import com.fruitisland.game.dto.HarvestResultVO;
import com.fruitisland.game.dto.LandVO;
import com.fruitisland.game.entity.*;
import com.fruitisland.game.mapper.PlayerLandMapper;
import com.fruitisland.game.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 玩家土地核心业务
 *
 * 状态合并逻辑：
 *   LOCKED        — player.level < config.unlock_level
 *   UNPURCHASED   — player.level >= config.unlock_level 但无 player_land 记录
 *   EMPTY         — 已购买，未种植
 *   PLANTED       — 已种植，未成熟（含水分系统）
 *   READY         — 已成熟可收获
 *
 * 浇水规则：
 *   种植后 water_level=0、finish_time=null，等待玩家浇水
 *   首次浇水后 water_level=100，并从此刻开始成熟倒计时
 *   每轮作物只浇水一次，不做持续水分衰减
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerLandServiceImpl
        extends BaseServiceImplX<PlayerLandMapper, PlayerLand>
        implements PlayerLandService {

    private final LandConfigService landConfigService;
    private final GamePlayerService gamePlayerService;
    private final InventoryService inventoryService;
    private final CropPlantService cropPlantService;
    private final CropConfigService cropConfigService;
    private final CropLevelConfigService cropLevelConfigService;
    private final PlayerCropService playerCropService;
    private final PlayerCropGrantService playerCropGrantService;
    private final FlowerConfigService flowerConfigService;
    private final FlowerLevelConfigService flowerLevelConfigService;
    private final PlayerFlowerRightService playerFlowerRightService;

    private static final int WATER_MAX = 100;

    /** 当前版本只区分未浇水(0)和已浇水(100)。 */
    private int calcCurrentWater(PlayerLand land) {
        return land.getWaterLevel() == null ? 0 : land.getWaterLevel();
    }

    @Override
    public List<LandVO> listByPlayer(Long playerId, Integer playerLevel) {
        List<LandConfig> allConfigs = landConfigService.list();
        List<PlayerLand> ownedLands = baseMapper.selectByPlayerId(playerId);
        Map<Long, PlayerLand> ownedMap = ownedLands.stream()
                .collect(Collectors.toMap(PlayerLand::getLandConfigId, pl -> pl));

        List<LandVO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (LandConfig config : allConfigs) {
            PlayerLand pl = ownedMap.get(config.getId());

            LandVO.LandVOBuilder builder = LandVO.builder()
                    .landConfigId(config.getId())
                    .areaType(config.getAreaType())
                    .blockId(config.getBlockId())
                    .gridX(config.getGridX())
                    .gridY(config.getGridY())
                    .unlockLevel(config.getUnlockLevel())
                    .buyPrice(config.getBuyPrice());

            if (playerLevel < config.getUnlockLevel()) {
                builder.status("LOCKED");
            } else if (pl == null) {
                builder.status("UNPURCHASED");
            } else {
                int currentWater = calcCurrentWater(pl);

                // 浇水并到达 finishTime 后才成熟。
                if ("PLANTED".equals(pl.getStatus()) && pl.getFinishTime() != null
                        && !pl.getFinishTime().isAfter(now)) {
                    pl.setStatus("READY");
                    updateById(pl);
                }

                builder.status(pl.getStatus())
                        .playerLandId(pl.getId())
                        .cropId(pl.getCropId())
                        .cropLevel(pl.getCropLevel())
                        .yieldCount(pl.getYieldCountSnapshot())
                        .harvestExp(pl.getHarvestExpSnapshot())
                        .accessType(pl.getAccessType())
                        .plantTime(pl.getPlantTime())
                        .finishTime(pl.getFinishTime())
                        .waterLevel(currentWater);
            }

            result.add(builder.build());
        }

        return result;
    }

    @Override
    @Transactional
    public PlayerLand buy(Long playerId, Long landConfigId, Integer playerLevel) {
        LandConfig config = landConfigService.getById(landConfigId);
        if (config == null) throw new RuntimeException("土地配置不存在");

        GamePlayer player = gamePlayerService.getById(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");

        if (playerLevel < config.getUnlockLevel())
            throw new RuntimeException("等级不足，需要 Lv." + config.getUnlockLevel());

        PlayerLand existing = baseMapper.selectByPlayerAndConfig(playerId, landConfigId);
        if (existing != null) throw new RuntimeException("该土地已购买");

        if (player.getGold() < config.getBuyPrice())
            throw new RuntimeException("金币不足");

        player.setGold(player.getGold() - config.getBuyPrice());
        gamePlayerService.updateById(player);

        PlayerLand land = new PlayerLand();
        land.setPlayerId(playerId);
        land.setLandConfigId(landConfigId);
        land.setStatus("EMPTY");
        save(land);

        log.info("玩家 {} 购买了土地 landConfigId={}, 花费 {} 金币", playerId, landConfigId, config.getBuyPrice());
        return land;
    }

    @Override
    @Transactional
    public PlayerLand plant(Long playerId, Long playerLandId, String cropId) {
        PlayerLand land = getById(playerLandId);
        if (land == null) throw new RuntimeException("土地不存在");
        if (!land.getPlayerId().equals(playerId)) throw new RuntimeException("这不是你的土地");
        if (!"EMPTY".equals(land.getStatus()))
            throw new RuntimeException("土地不是空地，当前状态: " + land.getStatus());

        GamePlayer player = gamePlayerService.getById(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");

        /*
         * 统一处理作物和花卉：
         * 先查 crop_config，找到则走作物流程；否则查 flower_config，走花卉流程。
         * 两种植物共享同一张 player_land 和同一套快照字段。
         */
        CropConfig cropConfig = cropConfigService.findByCropId(cropId);
        final int cropLevel;
        final String accessType;
        final Long accessGrantId;
        final int growSeconds;
        final int yieldCount;
        final int harvestExp;

        if (cropConfig != null && Integer.valueOf(1).equals(cropConfig.getEnabled())) {
            // --- 作物流程 ---
            if (player.getLevel() < cropConfig.getPlayerUnlockLevel())
                throw new RuntimeException("玩家等级不足，需要 Lv." + cropConfig.getPlayerUnlockLevel());

            PlayerCrop permanent = playerCropService.findByPlayerAndCrop(playerId, cropId);
            PlayerCropGrant temporary = null;
            if (permanent != null) {
                cropLevel = permanent.getCropLevel();
                accessType = "PERMANENT";
            } else {
                temporary = playerCropGrantService.findActiveGrant(
                        playerId, cropId, LocalDateTime.now());
                if (temporary == null) {
                    throw new RuntimeException("尚未获得该作物的种植权限");
                }
                cropLevel = temporary.getGrantCropLevel();
                accessType = "TEMPORARY";
            }
            accessGrantId = temporary != null ? temporary.getId() : null;

            CropLevelConfig levelConfig = requireLevelConfig(cropId, cropLevel);
            growSeconds = levelConfig.getGrowSeconds();
            yieldCount = levelConfig.getYieldCount();
            harvestExp = levelConfig.getHarvestExp();
        } else {
            // --- 花卉流程 ---
            FlowerConfig flowerConfig = flowerConfigService.findByFlowerId(cropId);
            if (flowerConfig == null || !Integer.valueOf(1).equals(flowerConfig.getEnabled())) {
                throw new RuntimeException("作物或花卉配置不存在: " + cropId);
            }

            PlayerFlowerRight right = playerFlowerRightService.findByPlayerAndFlower(playerId, cropId);
            if (right == null) {
                throw new RuntimeException("尚未获得该花卉的种植权限");
            }
            cropLevel = right.getFlowerLevel();
            accessType = "PERMANENT";
            accessGrantId = null;

            FlowerLevelConfig flowerLevelConfig =
                    flowerLevelConfigService.findByFlowerAndLevel(cropId, cropLevel);
            if (flowerLevelConfig == null)
                throw new RuntimeException("花卉等级配置不存在: " + cropId + " Lv." + cropLevel);
            growSeconds = flowerLevelConfig.getGrowSeconds();
            yieldCount = flowerLevelConfig.getYieldCount();
            harvestExp = flowerLevelConfig.getHarvestExp();
        }

        LocalDateTime now = LocalDateTime.now();

        land.setStatus("PLANTED");
        land.setCropId(cropId);
        land.setCropLevel(cropLevel);
        land.setGrowSecondsSnapshot(growSeconds);
        land.setYieldCountSnapshot(yieldCount);
        land.setHarvestExpSnapshot(harvestExp);
        land.setAccessType(accessType);
        land.setAccessGrantId(accessGrantId);
        land.setPlantTime(now);
        land.setFinishTime(null);
        land.setWaterLevel(0);
        land.setLastWateredAt(null);
        updateById(land);

        CropPlant record = new CropPlant();
        record.setPlayerLandId(playerLandId);
        record.setCropId(cropId);
        record.setCropLevel(cropLevel);
        record.setGrowSecondsSnapshot(growSeconds);
        record.setYieldCountSnapshot(yieldCount);
        record.setHarvestExpSnapshot(harvestExp);
        record.setAccessType(accessType);
        record.setAccessGrantId(accessGrantId);
        record.setPlantTime(now);
        record.setFinishTime(null);
        record.setStatus("WAITING_WATER");
        cropPlantService.save(record);

        log.info("玩家 {} 种植 {} 于土地 {}，等待浇水", playerId, cropId, playerLandId);
        return land;
    }

    @Override
    @Transactional
    public PlayerLand water(Long playerId, Long playerLandId) {
        PlayerLand land = getById(playerLandId);
        if (land == null) throw new RuntimeException("土地不存在");
        if (!land.getPlayerId().equals(playerId)) throw new RuntimeException("这不是你的土地");
        if (!"PLANTED".equals(land.getStatus()))
            throw new RuntimeException("只有种植中的作物需要浇水");
        if (land.getLastWateredAt() != null)
            throw new RuntimeException("这轮作物已经浇过水了");

        LocalDateTime now = LocalDateTime.now();
        /*
         * 使用种植时快照，而不是重新读当前等级配置。
         * 因此生长过程中升级作物或后台调整配置，不会改变这一轮成熟时间。
         */
        Integer growSec = land.getGrowSecondsSnapshot();
        if (growSec == null || growSec <= 0)
            throw new RuntimeException("本轮作物缺少成熟时间快照");
        land.setWaterLevel(WATER_MAX);
        land.setLastWateredAt(now);
        land.setFinishTime(now.plusSeconds(growSec));
        updateById(land);

        CropPlant latestRecord = cropPlantService.findLatestByPlayerLand(playerLandId);
        if (latestRecord != null && "WAITING_WATER".equals(latestRecord.getStatus())) {
            latestRecord.setFinishTime(land.getFinishTime());
            latestRecord.setStatus("GROWING");
            cropPlantService.updateById(latestRecord);
        }

        log.info("玩家 {} 浇水土地 {}，{}秒后成熟", playerId, playerLandId, growSec);
        return land;
    }

    @Override
    @Transactional
    public HarvestResultVO harvest(Long playerId, Long playerLandId) {
        PlayerLand land = getById(playerLandId);
        if (land == null) throw new RuntimeException("土地不存在");
        if (!land.getPlayerId().equals(playerId)) throw new RuntimeException("这不是你的土地");

        if ("EMPTY".equals(land.getStatus()))
            throw new RuntimeException("土地是空的，没东西可收");
        if ("PLANTED".equals(land.getStatus())) {
            if (land.getFinishTime() == null)
                throw new RuntimeException("作物还没有浇水 💧");
            if (land.getFinishTime() != null && land.getFinishTime().isAfter(LocalDateTime.now()))
                throw new RuntimeException("作物还没成熟");
        }

        String cropId = land.getCropId();
        /*
         * 使用种植时的产量快照。限时权限即使已经到期，已种下的作物仍可正常收获。
         */
        Integer yieldCount = land.getYieldCountSnapshot();
        if (yieldCount == null || yieldCount <= 0)
            throw new RuntimeException("本轮作物缺少产量快照");
        Integer harvestExp = land.getHarvestExpSnapshot();
        if (harvestExp == null || harvestExp < 0)
            throw new RuntimeException("本轮作物缺少收获经验快照");

        Integer cropLevel = land.getCropLevel();
        inventoryService.addItem(playerId, cropId, yieldCount);
        ExpGainResult expResult = gamePlayerService.addExp(playerId, harvestExp);

        CropPlant latestRecord = cropPlantService.findLatestByPlayerLand(playerLandId);
        if (latestRecord != null && "GROWING".equals(latestRecord.getStatus())) {
            latestRecord.setStatus("HARVESTED");
            cropPlantService.updateById(latestRecord);
        }

        land.setStatus("EMPTY");
        land.setCropId(null);
        land.setCropLevel(null);
        land.setGrowSecondsSnapshot(null);
        land.setYieldCountSnapshot(null);
        land.setHarvestExpSnapshot(null);
        land.setAccessType(null);
        land.setAccessGrantId(null);
        land.setPlantTime(null);
        land.setFinishTime(null);
        land.setWaterLevel(null);
        land.setLastWateredAt(null);
        updateById(land);

        log.info(
                "玩家 {} 收获土地 {} 的 {} ×{}，经验 +{}，等级 {}→{}",
                playerId, playerLandId, cropId, yieldCount, harvestExp,
                expResult.getBeforeLevel(), expResult.getAfterLevel()
        );
        return HarvestResultVO.builder()
                .playerLandId(playerLandId)
                .cropId(cropId)
                .cropLevel(cropLevel)
                .yieldCount(yieldCount)
                .expGained(harvestExp)
                .playerLevel(expResult.getAfterLevel())
                .cumulativeExp(expResult.getCumulativeExp())
                .nextLevelThreshold(expResult.getNextLevelThreshold())
                .levelsGained(expResult.getLevelsGained())
                .levelRewards(expResult.getLevelRewards())
                .build();
    }

    private CropConfig requireCropConfig(String cropId) {
        CropConfig config = cropConfigService.findByCropId(cropId);
        if (config == null) {
            throw new RuntimeException("作物配置不存在: " + cropId);
        }
        if (!Integer.valueOf(1).equals(config.getEnabled()))
            throw new RuntimeException("作物已停用: " + cropId);
        return config;
    }

    /** 校验并返回当前种植等级的数值配置。 */
    private CropLevelConfig requireLevelConfig(String cropId, Integer cropLevel) {
        CropLevelConfig config =
                cropLevelConfigService.findByCropAndLevel(cropId, cropLevel);
        if (config == null)
            throw new RuntimeException("作物等级配置不存在: " + cropId + " Lv." + cropLevel);
        if (config.getGrowSeconds() == null || config.getGrowSeconds() <= 0)
            throw new RuntimeException("作物成熟时间配置无效: " + cropId);
        if (config.getYieldCount() == null || config.getYieldCount() <= 0)
            throw new RuntimeException("作物产量配置无效: " + cropId);
        if (config.getHarvestExp() == null || config.getHarvestExp() < 0)
            throw new RuntimeException("作物收获经验配置无效: " + cropId);
        return config;
    }
}
