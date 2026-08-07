package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.FlowerLevelConfig;
import com.fruitisland.game.mapper.FlowerLevelConfigMapper;
import com.fruitisland.game.service.FlowerLevelConfigService;
import org.springframework.stereotype.Service;

@Service
public class FlowerLevelConfigServiceImpl
        extends BaseServiceImplX<FlowerLevelConfigMapper, FlowerLevelConfig>
        implements FlowerLevelConfigService {
    @Override
    public FlowerLevelConfig findByFlowerAndLevel(String flowerId, Integer flowerLevel) {
        return lambdaQuery()
                .eq(FlowerLevelConfig::getFlowerId, flowerId)
                .eq(FlowerLevelConfig::getFlowerLevel, flowerLevel)
                .one();
    }
}
