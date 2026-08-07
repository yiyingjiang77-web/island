package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.PlayerLevelConfig;

public interface PlayerLevelConfigService extends BaseServiceX<PlayerLevelConfig> {

    /** 查询当前等级升到下一级所需的配置。 */
    PlayerLevelConfig findByLevel(Integer level);
}
