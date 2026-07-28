package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.LandConfig;
import com.fruitisland.game.mapper.LandConfigMapper;
import com.fruitisland.game.service.LandConfigService;
import org.springframework.stereotype.Service;

@Service
public class LandConfigServiceImpl
        extends BaseServiceImplX<LandConfigMapper, LandConfig>
        implements LandConfigService {
}
