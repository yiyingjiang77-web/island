package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.CropLevelConfig;

public interface CropLevelConfigService extends BaseServiceX<CropLevelConfig> {

    /** 查询某个作物等级的唯一数值配置。 */
    CropLevelConfig findByCropAndLevel(String cropId, Integer cropLevel);
}
