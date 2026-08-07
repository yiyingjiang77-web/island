package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CoopConfig;
import com.fruitisland.game.mapper.CoopConfigMapper;
import com.fruitisland.game.service.CoopConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoopConfigServiceImpl
        extends BaseServiceImplX<CoopConfigMapper, CoopConfig>
        implements CoopConfigService {

    @Override
    public CoopConfig getByLevel(int level) {
        return lambdaQuery()
                .eq(CoopConfig::getLevel, level)
                .eq(CoopConfig::getEnabled, 1)
                .one();
    }

    @Override
    public List<CoopConfig> listEnabled() {
        return lambdaQuery()
                .eq(CoopConfig::getEnabled, 1)
                .orderByAsc(CoopConfig::getLevel)
                .list();
    }

    @Override
    public int getMaxEnabledLevel() {
        return listEnabled().stream()
                .mapToInt(CoopConfig::getLevel)
                .max()
                .orElse(0);
    }
}
