package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.ItemConfig;
import com.fruitisland.game.mapper.ItemConfigMapper;
import com.fruitisland.game.service.ItemConfigService;
import org.springframework.stereotype.Service;

@Service
public class ItemConfigServiceImpl extends BaseServiceImplX<ItemConfigMapper, ItemConfig> implements ItemConfigService {
}
