package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
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

    private static final int WATER_MAX = 100;

    /** 作物默认生长时间（秒），用于开发测试 */
    private static final Map<String, Integer> CROP_GROW_SECONDS = Map.of(
            "strawberry", 60,
            "cabbage", 120,
            "carrot", 180,
            "tomato", 240,
            "potato", 300,
            "chili", 480,
            "corn", 600
    );

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

        String seedId = cropId + "_seed";
        Inventory seedItem = inventoryService.findByPlayerAndItem(playerId, seedId);
        if (seedItem == null || seedItem.getCount() <= 0)
            throw new RuntimeException("没有 " + seedId + " 种子");

        seedItem.setCount(seedItem.getCount() - 1);
        inventoryService.updateById(seedItem);

        LocalDateTime now = LocalDateTime.now();

        land.setStatus("PLANTED");
        land.setCropId(cropId);
        land.setPlantTime(now);
        land.setFinishTime(null);
        land.setWaterLevel(0);
        land.setLastWateredAt(null);
        updateById(land);

        CropPlant record = new CropPlant();
        record.setPlayerLandId(playerLandId);
        record.setCropId(cropId);
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
        int growSec = CROP_GROW_SECONDS.getOrDefault(land.getCropId(), 60);
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
    public PlayerLand harvest(Long playerId, Long playerLandId) {
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
        inventoryService.addItem(playerId, cropId, 1);

        CropPlant latestRecord = cropPlantService.findLatestByPlayerLand(playerLandId);
        if (latestRecord != null && "GROWING".equals(latestRecord.getStatus())) {
            latestRecord.setStatus("HARVESTED");
            cropPlantService.updateById(latestRecord);
        }

        land.setStatus("EMPTY");
        land.setCropId(null);
        land.setPlantTime(null);
        land.setFinishTime(null);
        land.setWaterLevel(null);
        land.setLastWateredAt(null);
        updateById(land);

        log.info("玩家 {} 收获土地 {} 的 {}", playerId, playerLandId, cropId);
        return land;
    }
}
