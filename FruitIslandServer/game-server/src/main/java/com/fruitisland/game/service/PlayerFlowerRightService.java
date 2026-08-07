package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.PlayerFlowerRight;

import java.util.List;

public interface PlayerFlowerRightService extends BaseServiceX<PlayerFlowerRight> {
    PlayerFlowerRight findByPlayerAndFlower(Long playerId, String flowerId);
    List<PlayerFlowerRight> listByPlayer(Long playerId);
    PlayerFlowerRight purchase(Long playerId, String flowerId);
}
