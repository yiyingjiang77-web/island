package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CropLevelConfig;
import com.fruitisland.game.mapper.CropLevelConfigMapper;
import com.fruitisland.game.service.CropLevelConfigService;
import org.springframework.stereotype.Service;

@Service
public class CropLevelConfigServiceImpl
        extends BaseServiceImplX<CropLevelConfigMapper, CropLevelConfig>
        implements CropLevelConfigService {

    @Override
    public CropLevelConfig findByCropAndLevel(String cropId, Integer cropLevel) {
        return lambdaQuery()
                .eq(CropLevelConfig::getCropId, cropId)
                .eq(CropLevelConfig::getCropLevel, cropLevel)
                .one();
    }
}
