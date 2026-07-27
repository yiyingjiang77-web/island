package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.Land;
import com.fruitisland.game.mapper.LandMapper;
import com.fruitisland.game.service.LandService;
import org.springframework.stereotype.Service;

@Service
public class LandServiceImpl extends BaseServiceImplX<LandMapper, Land> implements LandService {
}
