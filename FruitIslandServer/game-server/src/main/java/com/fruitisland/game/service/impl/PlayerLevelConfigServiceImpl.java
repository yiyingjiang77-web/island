package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.PlayerLevelConfig;
import com.fruitisland.game.mapper.PlayerLevelConfigMapper;
import com.fruitisland.game.service.PlayerLevelConfigService;
import org.springframework.stereotype.Service;

@Service
public class PlayerLevelConfigServiceImpl
        extends BaseServiceImplX<PlayerLevelConfigMapper, PlayerLevelConfig>
        implements PlayerLevelConfigService {

    @Override
    public PlayerLevelConfig findByLevel(Integer level) {
        return getById(level);
    }
}
