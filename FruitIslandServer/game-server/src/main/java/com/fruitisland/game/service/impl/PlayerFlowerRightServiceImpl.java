package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.FlowerConfig;
import com.fruitisland.game.entity.FlowerLevelConfig;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerFlowerRight;
import com.fruitisland.game.mapper.PlayerFlowerRightMapper;
import com.fruitisland.game.service.FlowerConfigService;
import com.fruitisland.game.service.FlowerLevelConfigService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerFlowerRightService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerFlowerRightServiceImpl
        extends BaseServiceImplX<PlayerFlowerRightMapper, PlayerFlowerRight>
        implements PlayerFlowerRightService {

    private final FlowerConfigService flowerConfigService;
    private final FlowerLevelConfigService flowerLevelConfigService;
    private final GamePlayerService gamePlayerService;

    @Override
    public PlayerFlowerRight findByPlayerAndFlower(Long playerId, String flowerId) {
        return lambdaQuery()
                .eq(PlayerFlowerRight::getPlayerId, playerId)
                .eq(PlayerFlowerRight::getFlowerId, flowerId)
                .one();
    }

    @Override
    public List<PlayerFlowerRight> listByPlayer(Long playerId) {
        return lambdaQuery()
                .eq(PlayerFlowerRight::getPlayerId, playerId)
                .orderByAsc(PlayerFlowerRight::getId)
                .list();
    }

    @Override
    @Transactional
    public PlayerFlowerRight purchase(Long playerId, String flowerId) {
        PlayerFlowerRight existing = findByPlayerAndFlower(playerId, flowerId);
        if (existing != null) {
            throw new RuntimeException("已拥有该花卉种植权");
        }

        FlowerConfig config = flowerConfigService.findByFlowerId(flowerId);
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            throw new RuntimeException("花卉不存在或已停用: " + flowerId);
        }

        GamePlayer player = requirePlayer(playerId);
        String currency = config.getCurrencyType();
        long price = config.getSeedPrice() == null ? 0 : config.getSeedPrice();

        if ("DIAMOND".equals(currency)) {
            if (player.getDiamond() < price) {
                throw new RuntimeException("钻石不足，需要 " + price);
            }
            player.setDiamond(player.getDiamond() - (int) price);
        } else {
            if (player.getGold() < price) {
                throw new RuntimeException("金币不足，需要 " + price);
            }
            player.setGold(player.getGold() - price);
        }
        gamePlayerService.updateById(player);

        PlayerFlowerRight right = new PlayerFlowerRight();
        right.setPlayerId(playerId);
        right.setFlowerId(flowerId);
        right.setFlowerLevel(1);
        right.setUnlockSource("DIAMOND".equals(currency) ? "DIAMOND_SHOP" : "GOLD_SHOP");
        right.setUnlockTime(LocalDateTime.now());
        try {
            save(right);
            return right;
        } catch (DuplicateKeyException ignored) {
            return findByPlayerAndFlower(playerId, flowerId);
        }
    }

    @Override
    @Transactional
    public PlayerFlowerRight upgrade(Long playerId, String flowerId) {
        PlayerFlowerRight right = findByPlayerAndFlower(playerId, flowerId);
        if (right == null) {
            throw new RuntimeException("尚未拥有该花卉，不能升级");
        }

        FlowerConfig config = flowerConfigService.findByFlowerId(flowerId);
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            throw new RuntimeException("花卉不存在或已停用: " + flowerId);
        }

        int targetLevel = right.getFlowerLevel() + 1;
        if (targetLevel > config.getMaxLevel()) {
            throw new RuntimeException("花卉已达到最高等级");
        }

        FlowerLevelConfig targetConfig =
                flowerLevelConfigService.findByFlowerAndLevel(flowerId, targetLevel);
        if (targetConfig == null || targetConfig.getUpgradeGold() == null) {
            throw new RuntimeException("缺少目标等级配置: " + flowerId + " Lv." + targetLevel);
        }

        GamePlayer player = requirePlayer(playerId);
        if (player.getGold() < targetConfig.getUpgradeGold()) {
            throw new RuntimeException("金币不足，需要 " + targetConfig.getUpgradeGold());
        }

        player.setGold(player.getGold() - targetConfig.getUpgradeGold());
        gamePlayerService.updateById(player);
        right.setFlowerLevel(targetLevel);
        updateById(right);
        return right;
    }

    private GamePlayer requirePlayer(Long playerId) {
        GamePlayer player = gamePlayerService.getById(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");
        return player;
    }
}
