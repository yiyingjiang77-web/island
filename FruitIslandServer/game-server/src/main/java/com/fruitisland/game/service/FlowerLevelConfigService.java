package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.FlowerLevelConfig;

import java.util.List;

public interface FlowerLevelConfigService extends BaseServiceX<FlowerLevelConfig> {

    FlowerLevelConfig findByFlowerAndLevel(String flowerId, Integer flowerLevel);

    List<FlowerLevelConfig> listByFlower(String flowerId);
}
