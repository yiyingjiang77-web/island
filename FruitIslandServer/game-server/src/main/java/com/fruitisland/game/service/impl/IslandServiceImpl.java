package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.Island;
import com.fruitisland.game.mapper.IslandMapper;
import com.fruitisland.game.service.IslandService;
import org.springframework.stereotype.Service;

@Service
public class IslandServiceImpl extends BaseServiceImplX<IslandMapper, Island> implements IslandService {
}
