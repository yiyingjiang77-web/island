package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CakeShopConfig;
import com.fruitisland.game.mapper.CakeShopConfigMapper;
import com.fruitisland.game.service.CakeShopConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CakeShopConfigServiceImpl
        extends BaseServiceImplX<CakeShopConfigMapper, CakeShopConfig>
        implements CakeShopConfigService {

    @Override
    public CakeShopConfig getByLevel(int level) {
        return lambdaQuery()
                .eq(CakeShopConfig::getLevel, level)
                .eq(CakeShopConfig::getEnabled, 1)
                .one();
    }

    @Override
    public List<CakeShopConfig> listEnabled() {
        return lambdaQuery()
                .eq(CakeShopConfig::getEnabled, 1)
                .orderByAsc(CakeShopConfig::getLevel)
                .list();
    }

    @Override
    public int getMaxEnabledLevel() {
        return listEnabled().stream()
                .mapToInt(CakeShopConfig::getLevel)
                .max()
                .orElse(0);
    }
}
