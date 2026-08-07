package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.CoopConfig;

import java.util.List;

public interface CoopConfigService extends BaseServiceX<CoopConfig> {

    CoopConfig getByLevel(int level);

    List<CoopConfig> listEnabled();

    int getMaxEnabledLevel();
}
