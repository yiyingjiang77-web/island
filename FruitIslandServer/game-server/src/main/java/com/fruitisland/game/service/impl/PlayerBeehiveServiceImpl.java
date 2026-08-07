package com.fruitisland.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.FlowerConfig;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerBeehive;
import com.fruitisland.game.entity.PlayerFlowerRight;
import com.fruitisland.game.entity.PlayerLand;
import com.fruitisland.game.mapper.PlayerBeehiveMapper;
import com.fruitisland.game.mapper.PlayerLandMapper;
import com.fruitisland.game.service.FlowerConfigService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.InventoryService;
import com.fruitisland.game.service.PlayerBeehiveService;
import com.fruitisland.game.service.PlayerFlowerRightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlayerBeehiveServiceImpl
        extends BaseServiceImplX<PlayerBeehiveMapper, PlayerBeehive>
        implements PlayerBeehiveService {

    private static final int MAX_BEEHIVES = 3;
    private static final long PRODUCE_CYCLE_SECONDS = 7200L;
    private static final int[] BEEHIVE_PRICES = {1000, 2000, 3000};
    private static final int[] STORAGE_CAPS = {0, 20, 40, 60};

    private final GamePlayerService gamePlayerService;
    private final FlowerConfigService flowerConfigService;
    private final PlayerFlowerRightService playerFlowerRightService;
    private final InventoryService inventoryService;
    private final PlayerLandMapper playerLandMapper;

    @Override
    public PlayerBeehive getOrCreate(Long playerId) {
        PlayerBeehive beehive = lambdaQuery()
                .eq(PlayerBeehive::getPlayerId, playerId)
                .one();
        if (beehive != null) return beehive;

        beehive = new PlayerBeehive();
        beehive.setPlayerId(playerId);
        beehive.setBeehiveCount(0);
        beehive.setHoneyStored(0);
        save(beehive);
        return beehive;
    }

    @Override
    @Transactional
    public PlayerBeehive purchaseBeehive(Long playerId) {
        PlayerBeehive beehive = getOrCreate(playerId);
        int currentCount = beehive.getBeehiveCount() == null ? 0 : beehive.getBeehiveCount();
        if (currentCount >= MAX_BEEHIVES) {
            throw new RuntimeException("蜂箱数量已达上限");
        }

        int price = BEEHIVE_PRICES[currentCount];
        GamePlayer player = gamePlayerService.getById(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");
        if (player.getGold() < price) {
            throw new RuntimeException("金币不足，需要 " + price);
        }

        player.setGold(player.getGold() - price);
        gamePlayerService.updateById(player);

        beehive.setBeehiveCount(currentCount + 1);
        if (beehive.getLastProduceTime() == null) {
            beehive.setLastProduceTime(LocalDateTime.now());
        }
        updateById(beehive);
        return beehive;
    }

    @Override
    @Transactional
    public PlayerBeehive settleProduction(Long playerId) {
        PlayerBeehive beehive = getOrCreate(playerId);
        if (beehive.getBeehiveCount() == null || beehive.getBeehiveCount() == 0) {
            return beehive;
        }

        int storageCap = STORAGE_CAPS[beehive.getBeehiveCount()];
        if (beehive.getHoneyStored() != null && beehive.getHoneyStored() >= storageCap) {
            return beehive;
        }

        int honeyPerCycle = calculateHoneyPerCycle(playerId);
        if (honeyPerCycle <= 0) {
            return beehive;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastProduce = beehive.getLastProduceTime();
        if (lastProduce == null) {
            lastProduce = now;
        }

        long elapsedSeconds = ChronoUnit.SECONDS.between(lastProduce, now);
        if (elapsedSeconds < PRODUCE_CYCLE_SECONDS) {
            return beehive;
        }

        long cycles = elapsedSeconds / PRODUCE_CYCLE_SECONDS;
        int produced = (int) Math.min(
                (long) honeyPerCycle * cycles,
                (long) storageCap - beehive.getHoneyStored()
        );
        if (produced <= 0) {
            return beehive;
        }

        beehive.setHoneyStored(beehive.getHoneyStored() + produced);
        beehive.setLastProduceTime(lastProduce.plusSeconds(cycles * PRODUCE_CYCLE_SECONDS));
        updateById(beehive);
        return beehive;
    }

    @Override
    @Transactional
    public int collectHoney(Long playerId) {
        PlayerBeehive beehive = settleProduction(playerId);
        if (beehive.getHoneyStored() == null || beehive.getHoneyStored() <= 0) {
            return 0;
        }

        int collected = beehive.getHoneyStored();
        inventoryService.addItem(playerId, "honey", collected);
        beehive.setHoneyStored(0);
        beehive.setLastCollectTime(LocalDateTime.now());
        updateById(beehive);
        return collected;
    }

    /**
     * 计算每周期产蜜量：floor(Σ(已成熟花朵数 × 蜂蜜系数 × 等级倍率))
     */
    private int calculateHoneyPerCycle(Long playerId) {
        List<FlowerConfig> flowerConfigs = flowerConfigService.listEnabled();
        if (flowerConfigs.isEmpty()) return 0;

        Map<String, FlowerConfig> configMap = new HashMap<>();
        for (FlowerConfig fc : flowerConfigs) {
            configMap.put(fc.getFlowerId(), fc);
        }

        List<PlayerFlowerRight> rights = playerFlowerRightService.listByPlayer(playerId);
        Map<String, Integer> levelMap = new HashMap<>();
        for (PlayerFlowerRight r : rights) {
            levelMap.put(r.getFlowerId(), r.getFlowerLevel());
        }

        List<PlayerLand> readyLands = playerLandMapper.selectList(
                new LambdaQueryWrapper<PlayerLand>()
                        .eq(PlayerLand::getPlayerId, playerId)
                        .eq(PlayerLand::getStatus, "READY")
                        .in(PlayerLand::getCropId, configMap.keySet()));

        if (readyLands.isEmpty()) return 0;

        Map<String, Integer> matureCount = new HashMap<>();
        for (PlayerLand land : readyLands) {
            matureCount.merge(land.getCropId(), 1, Integer::sum);
        }

        double total = 0;
        for (Map.Entry<String, Integer> entry : matureCount.entrySet()) {
            String flowerId = entry.getKey();
            int count = entry.getValue();
            FlowerConfig config = configMap.get(flowerId);
            if (config == null) continue;

            int coefficient = config.getHoneyCoefficient() == null ? 1 : config.getHoneyCoefficient();
            int level = levelMap.getOrDefault(flowerId, 1);
            double multiplier = Math.min(1 + 0.4 * Math.pow(level - 1, 0.85), 5.0);

            total += count * coefficient * multiplier;
        }

        return (int) Math.floor(total);
    }
}
