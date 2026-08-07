package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.BarnConfig;
import com.fruitisland.game.mapper.BarnConfigMapper;
import com.fruitisland.game.service.BarnConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BarnConfigServiceImpl
        extends BaseServiceImplX<BarnConfigMapper, BarnConfig>
        implements BarnConfigService {

    @Override
    public BarnConfig getByLevel(int level) {
        return lambdaQuery()
                .eq(BarnConfig::getLevel, level)
                .eq(BarnConfig::getEnabled, 1)
                .one();
    }

    @Override
    public List<BarnConfig> listEnabled() {
        return lambdaQuery()
                .eq(BarnConfig::getEnabled, 1)
                .orderByAsc(BarnConfig::getLevel)
                .list();
    }

    @Override
    public int getMaxEnabledLevel() {
        return listEnabled().stream()
                .mapToInt(BarnConfig::getLevel)
                .max()
                .orElse(0);
    }
}
