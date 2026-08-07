package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.RecipeConfig;
import com.fruitisland.game.mapper.RecipeConfigMapper;
import com.fruitisland.game.service.RecipeConfigService;
import org.springframework.stereotype.Service;

@Service
public class RecipeConfigServiceImpl extends BaseServiceImplX<RecipeConfigMapper, RecipeConfig> implements RecipeConfigService {
}
