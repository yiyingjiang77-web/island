package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.dto.ExpGainResult;
import com.fruitisland.game.entity.GamePlayer;

public interface GamePlayerService extends BaseServiceX<GamePlayer> {

    /** 根据 userId 查找角色 */
    GamePlayer findByUserId(Long userId);

    /** 为新用户创建初始角色 */
    GamePlayer createPlayer(Long userId);

    /**
     * 增加玩家经验并按 player_level_config 自动升级。
     * 支持一次奖励连续提升多级，并自动发放升级金币。
     */
    ExpGainResult addExp(Long playerId, int amount);
}
