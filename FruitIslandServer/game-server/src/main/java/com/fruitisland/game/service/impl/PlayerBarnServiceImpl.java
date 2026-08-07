package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.BarnConfig;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerBarn;
import com.fruitisland.game.mapper.PlayerBarnMapper;
import com.fruitisland.game.service.BarnConfigService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.InventoryService;
import com.fruitisland.game.service.PlayerBarnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PlayerBarnServiceImpl
        extends BaseServiceImplX<PlayerBarnMapper, PlayerBarn>
        implements PlayerBarnService {

    private static final int UNLOCK_REQUIRED_LEVEL = 5;
    private static final int UNLOCK_GOLD = 1000;
    private static final int FIRST_COW_COUNT = 1;
    private static final int FIRST_MILK_COUNT = 10;
    private static final int MAX_ANIMALS = 8;

    private final GamePlayerService gamePlayerService;
    private final BarnConfigService barnConfigService;
    private final InventoryService inventoryService;

    @Override
    public PlayerBarn getOrCreate(Long playerId) {
        PlayerBarn barn = lambdaQuery()
                .eq(PlayerBarn::getPlayerId, playerId)
                .one();
        if (barn != null) return barn;

        barn = new PlayerBarn();
        barn.setPlayerId(playerId);
        barn.setLevel(0);
        barn.setCowCount(0);
        barn.setCycleSeconds(600);
        barn.setCowCountSnapshot(0);
        barn.setMilkPerCowSnapshot(0);
        save(barn);
        return barn;
    }

    @Override
    @Transactional
    public PlayerBarn unlockBarn(Long playerId) {
        PlayerBarn barn = getOrCreate(playerId);
        if (barn.getLevel() != null && barn.getLevel() > 0) {
            throw new RuntimeException("牛棚已解锁");
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

        BarnConfig config = barnConfigService.getByLevel(1);
        if (config == null) throw new RuntimeException("牛棚一级配置不存在");

        barn.setLevel(1);
        barn.setCowCount(FIRST_COW_COUNT);
        barn.setCycleStartTime(LocalDateTime.now());
        barn.setCycleSeconds(config.getProduceCycleSeconds());
        barn.setCowCountSnapshot(FIRST_COW_COUNT);
        barn.setMilkPerCowSnapshot(config.getMilkPerCow());
        updateById(barn);

        inventoryService.addItem(playerId, "milk", FIRST_MILK_COUNT);

        return barn;
    }

    @Override
    @Transactional
    public PlayerBarn upgradeBarn(Long playerId) {
        PlayerBarn barn = getOrCreate(playerId);
        if (barn.getLevel() == null || barn.getLevel() == 0) {
            throw new RuntimeException("请先解锁牛棚");
        }

        // 先结算当前周期产出
        settleMilkProduction(playerId);
        barn = getOrCreate(playerId);

        int nextLevel = barn.getLevel() + 1;
        BarnConfig nextConfig = barnConfigService.getByLevel(nextLevel);
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

        barn.setLevel(nextLevel);
        int newCowCount = Math.min(
                barn.getCowCount() + nextConfig.getAnimalAdded(),
                Math.min(nextConfig.getAnimalCapacity(), MAX_ANIMALS));
        barn.setCowCount(newCowCount);
        // 当前周期快照不变，下一周期用新配置
        updateById(barn);

        return barn;
    }

    @Override
    @Transactional
    public PlayerBarn settleMilkProduction(Long playerId) {
        PlayerBarn barn = getOrCreate(playerId);
        if (barn.getLevel() == null || barn.getLevel() == 0) {
            return barn;
        }
        if (barn.getCycleStartTime() == null) {
            return barn;
        }
        if (barn.getCycleSeconds() == null || barn.getCycleSeconds() <= 0) {
            return barn;
        }

        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = ChronoUnit.SECONDS.between(barn.getCycleStartTime(), now);
        if (elapsedSeconds < barn.getCycleSeconds()) {
            return barn;
        }

        long completedCycles = elapsedSeconds / barn.getCycleSeconds();
        int milkPerCycle = barn.getCowCountSnapshot() * barn.getMilkPerCowSnapshot();
        if (milkPerCycle <= 0) {
            // 重新快照当前配置
            snapshotCurrentConfig(barn);
            barn.setCycleStartTime(barn.getCycleStartTime().plusSeconds(
                    completedCycles * barn.getCycleSeconds()));
            updateById(barn);
            return barn;
        }

        int totalMilk = (int) (milkPerCycle * completedCycles);
        inventoryService.addItem(playerId, "milk", totalMilk);

        // 推进周期开始时间
        barn.setCycleStartTime(barn.getCycleStartTime().plusSeconds(
                completedCycles * barn.getCycleSeconds()));

        // 新周期快照当前配置
        snapshotCurrentConfig(barn);

        updateById(barn);
        return barn;
    }

    private void snapshotCurrentConfig(PlayerBarn barn) {
        BarnConfig config = barnConfigService.getByLevel(barn.getLevel());
        if (config != null) {
            barn.setCycleSeconds(config.getProduceCycleSeconds());
            barn.setCowCountSnapshot(barn.getCowCount());
            barn.setMilkPerCowSnapshot(config.getMilkPerCow());
        }
    }
}
