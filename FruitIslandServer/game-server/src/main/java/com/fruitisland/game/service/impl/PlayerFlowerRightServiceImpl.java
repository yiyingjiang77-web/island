package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.FlowerConfig;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerFlowerRight;
import com.fruitisland.game.mapper.PlayerFlowerRightMapper;
import com.fruitisland.game.service.FlowerConfigService;
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
        if (existing != null) return existing;

        FlowerConfig config = flowerConfigService.findByFlowerId(flowerId);
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            throw new RuntimeException("花卉不存在或已停用: " + flowerId);
        }
        GamePlayer player = gamePlayerService.getById(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");

        long price = config.getPurchasePrice();
        if ("DIAMOND".equals(config.getPurchaseCurrency())) {
            if (player.getDiamond() < price) throw new RuntimeException("钻石不足");
            player.setDiamond((int) (player.getDiamond() - price));
        } else if ("GOLD".equals(config.getPurchaseCurrency())) {
            if (player.getGold() < price) throw new RuntimeException("金币不足");
            player.setGold(player.getGold() - price);
        } else {
            throw new RuntimeException("不支持的购买货币: " + config.getPurchaseCurrency());
        }
        gamePlayerService.updateById(player);

        PlayerFlowerRight right = new PlayerFlowerRight();
        right.setPlayerId(playerId);
        right.setFlowerId(flowerId);
        right.setFlowerLevel(1);
        right.setPurchaseCurrency(config.getPurchaseCurrency());
        right.setPurchaseTime(LocalDateTime.now());
        try {
            save(right);
            return right;
        } catch (DuplicateKeyException ignored) {
            return findByPlayerAndFlower(playerId, flowerId);
        }
    }
}
