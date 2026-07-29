package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.CropConfig;

import java.util.List;

public interface CropConfigService extends BaseServiceX<CropConfig> {

    CropConfig findByCropId(String cropId);

    /** 查询当前玩家等级已经达到种植门槛的启用作物。 */
    List<CropConfig> listAvailableAtLevel(Integer playerLevel);
}
