package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CropConfig;
import com.fruitisland.game.mapper.CropConfigMapper;
import com.fruitisland.game.service.CropConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CropConfigServiceImpl
        extends BaseServiceImplX<CropConfigMapper, CropConfig>
        implements CropConfigService {

    @Override
    public CropConfig findByCropId(String cropId) {
        return getById(cropId);
    }

    @Override
    public List<CropConfig> listAvailableAtLevel(Integer playerLevel) {
        return lambdaQuery()
                .eq(CropConfig::getEnabled, 1)
                .le(CropConfig::getPlayerUnlockLevel, playerLevel)
                .orderByAsc(CropConfig::getPlayerUnlockLevel)
                .list();
    }
}
