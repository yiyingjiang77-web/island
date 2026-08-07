package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.CakeShopConfig;

import java.util.List;

public interface CakeShopConfigService extends BaseServiceX<CakeShopConfig> {

    CakeShopConfig getByLevel(int level);

    List<CakeShopConfig> listEnabled();

    int getMaxEnabledLevel();
}
