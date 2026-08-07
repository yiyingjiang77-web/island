package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.FlowerConfig;

import java.util.List;

public interface FlowerConfigService extends BaseServiceX<FlowerConfig> {

    FlowerConfig findByFlowerId(String flowerId);

    List<FlowerConfig> listEnabled();
}
