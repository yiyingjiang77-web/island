package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CoopConfig;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerCoop;
import com.fruitisland.game.mapper.PlayerCoopMapper;
import com.fruitisland.game.service.CoopConfigService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.InventoryService;
import com.fruitisland.game.service.PlayerCoopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PlayerCoopServiceImpl
        extends BaseServiceImplX<PlayerCoopMapper, PlayerCoop>
        implements PlayerCoopService {

    private static final int UNLOCK_REQUIRED_LEVEL = 8;
    private static final int UNLOCK_GOLD = 3000;
    private static final int FIRST_CHICKEN_COUNT = 1;
    private static final int FIRST_EGG_COUNT = 5;
    private static final int MAX_ANIMALS = 8;

    private final GamePlayerService gamePlayerService;
    private final CoopConfigService coopConfigService;
    private final InventoryService inventoryService;

    @Override
    public PlayerCoop getOrCreate(Long playerId) {
        PlayerCoop coop = lambdaQuery()
                .eq(PlayerCoop::getPlayerId, playerId)
                .one();
        if (coop != null) return coop;

        coop = new PlayerCoop();
        coop.setPlayerId(playerId);
        coop.setLevel(0);
        coop.setChickenCount(0);
        coop.setCycleSeconds(600);
        coop.setChickenCountSnapshot(0);
        coop.setBonusEggSnapshot(0);
        save(coop);
        return coop;
    }

    @Override
    @Transactional
    public PlayerCoop unlockCoop(Long playerId) {
        PlayerCoop coop = getOrCreate(playerId);
        if (coop.getLevel() != null && coop.getLevel() > 0) {
            throw new RuntimeException("鸡舍已解锁");
        }

        GamePlayer player = gamePlayerService.getById(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");

        int islandLevel = player.getLevel() == null ? 1 : player.getLevel();
        if (islandLevel < UNLOCK_REQUIRED_LEVEL) {
            throw new RuntimeException("岛屿等级不足，需要 " + UNLOCK_REQUIRED_LEVEL + " 级");
        }
        if (player.getGold() < UNLOCK_GOLD) {
            throw new RuntimeException("金币不足，需要 " + UNLOCK_GOLD);
        }

        player.setGold(player.getGold() - UNLOCK_GOLD);
        gamePlayerService.updateById(player);

        CoopConfig config = coopConfigService.getByLevel(1);
        if (config == null) throw new RuntimeException("鸡舍一级配置不存在");

        coop.setLevel(1);
        coop.setChickenCount(FIRST_CHICKEN_COUNT);
        coop.setCycleStartTime(LocalDateTime.now());
        coop.setCycleSeconds(config.getProduceCycleSeconds());
        coop.setChickenCountSnapshot(FIRST_CHICKEN_COUNT);
        coop.setBonusEggSnapshot(config.getBonusEggs());
        updateById(coop);

        inventoryService.addItem(playerId, "egg", FIRST_EGG_COUNT);

        return coop;
    }

    @Override
    @Transactional
    public PlayerCoop upgradeCoop(Long playerId) {
        PlayerCoop coop = getOrCreate(playerId);
        if (coop.getLevel() == null || coop.getLevel() == 0) {
            throw new RuntimeException("请先解锁鸡舍");
        }

        // 先结算当前周期产出
        settleEggProduction(playerId);
        coop = getOrCreate(playerId);

        int nextLevel = coop.getLevel() + 1;
        CoopConfig nextConfig = coopConfigService.getByLevel(nextLevel);
        if (nextConfig == null) {
            throw new RuntimeException("已是最高等级或下一等级未开放");
        }

        GamePlayer player = gamePlayerService.getById(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");

        int islandLevel = player.getLevel() == null ? 1 : player.getLevel();
        if (islandLevel < nextConfig.getRequiredIslandLevel()) {
            throw new RuntimeException("岛屿等级不足，需要 " + nextConfig.getRequiredIslandLevel() + " 级");
        }
        if (player.getGold() < nextConfig.getUpgradeGold()) {
            throw new RuntimeException("金币不足，需要 " + nextConfig.getUpgradeGold());
        }

        player.setGold(player.getGold() - nextConfig.getUpgradeGold());
        gamePlayerService.updateById(player);

        coop.setLevel(nextLevel);
        int newChickenCount = Math.min(
                coop.getChickenCount() + nextConfig.getAnimalAdded(),
                Math.min(nextConfig.getAnimalCapacity(), MAX_ANIMALS));
        coop.setChickenCount(newChickenCount);
        updateById(coop);

        return coop;
    }

    @Override
    @Transactional
    public PlayerCoop settleEggProduction(Long playerId) {
        PlayerCoop coop = getOrCreate(playerId);
        if (coop.getLevel() == null || coop.getLevel() == 0) {
            return coop;
        }
        if (coop.getCycleStartTime() == null) {
            return coop;
        }
        if (coop.getCycleSeconds() == null || coop.getCycleSeconds() <= 0) {
            return coop;
        }

        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = ChronoUnit.SECONDS.between(coop.getCycleStartTime(), now);
        if (elapsedSeconds < coop.getCycleSeconds()) {
            return coop;
        }

        long completedCycles = elapsedSeconds / coop.getCycleSeconds();
        int eggsPerCycle = coop.getChickenCountSnapshot() + coop.getBonusEggSnapshot();
        if (eggsPerCycle <= 0) {
            snapshotCurrentConfig(coop);
            coop.setCycleStartTime(coop.getCycleStartTime().plusSeconds(
                    completedCycles * coop.getCycleSeconds()));
            updateById(coop);
            return coop;
        }

        int totalEggs = (int) (eggsPerCycle * completedCycles);
        inventoryService.addItem(playerId, "egg", totalEggs);

        coop.setCycleStartTime(coop.getCycleStartTime().plusSeconds(
                completedCycles * coop.getCycleSeconds()));

        snapshotCurrentConfig(coop);

        updateById(coop);
        return coop;
    }

    private void snapshotCurrentConfig(PlayerCoop coop) {
        CoopConfig config = coopConfigService.getByLevel(coop.getLevel());
        if (config != null) {
            coop.setCycleSeconds(config.getProduceCycleSeconds());
            coop.setChickenCountSnapshot(coop.getChickenCount());
            coop.setBonusEggSnapshot(config.getBonusEggs());
        }
    }
}
