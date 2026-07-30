package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.ShopConfig;
import com.fruitisland.game.mapper.ShopConfigMapper;
import com.fruitisland.game.service.ShopConfigService;
import org.springframework.stereotype.Service;

@Service
public class ShopConfigServiceImpl extends BaseServiceImplX<ShopConfigMapper, ShopConfig> implements ShopConfigService {
}
