package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.Building;
import com.fruitisland.game.mapper.BuildingMapper;
import com.fruitisland.game.service.BuildingService;
import org.springframework.stereotype.Service;

@Service
public class BuildingServiceImpl extends BaseServiceImplX<BuildingMapper, Building> implements BuildingService {
}
