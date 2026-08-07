package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.BarnConfig;

import java.util.List;

public interface BarnConfigService extends BaseServiceX<BarnConfig> {

    BarnConfig getByLevel(int level);

    List<BarnConfig> listEnabled();

    /** 返回已启用的最大等级。 */
    int getMaxEnabledLevel();
}
