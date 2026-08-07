package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.PlayerFlowerRight;

import java.util.List;

public interface PlayerFlowerRightService extends BaseServiceX<PlayerFlowerRight> {

    PlayerFlowerRight findByPlayerAndFlower(Long playerId, String flowerId);

    List<PlayerFlowerRight> listByPlayer(Long playerId);

    /** 购买花卉永久种植权（金币或钻石）。 */
    PlayerFlowerRight purchase(Long playerId, String flowerId);

    /** 使用金币将花卉提升一级。 */
    PlayerFlowerRight upgrade(Long playerId, String flowerId);
}
