package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.FlowerConfig;
import com.fruitisland.game.mapper.FlowerConfigMapper;
import com.fruitisland.game.service.FlowerConfigService;
import org.springframework.stereotype.Service;

@Service
public class FlowerConfigServiceImpl extends BaseServiceImplX<FlowerConfigMapper, FlowerConfig>
        implements FlowerConfigService {
    @Override
    public FlowerConfig findByFlowerId(String flowerId) {
        return getById(flowerId);
    }
}
