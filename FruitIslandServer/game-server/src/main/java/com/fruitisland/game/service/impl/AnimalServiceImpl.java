package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.Animal;
import com.fruitisland.game.mapper.AnimalMapper;
import com.fruitisland.game.service.AnimalService;
import org.springframework.stereotype.Service;

@Service
public class AnimalServiceImpl extends BaseServiceImplX<AnimalMapper, Animal> implements AnimalService {
}
