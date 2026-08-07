package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.FlowerConfig;
import com.fruitisland.game.mapper.FlowerConfigMapper;
import com.fruitisland.game.service.FlowerConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlowerConfigServiceImpl
        extends BaseServiceImplX<FlowerConfigMapper, FlowerConfig>
        implements FlowerConfigService {

    @Override
    public FlowerConfig findByFlowerId(String flowerId) {
        return getById(flowerId);
    }

    @Override
    public List<FlowerConfig> listEnabled() {
        return lambdaQuery()
                .eq(FlowerConfig::getEnabled, 1)
                .orderByAsc(FlowerConfig::getSeedPrice)
                .list();
    }
}
