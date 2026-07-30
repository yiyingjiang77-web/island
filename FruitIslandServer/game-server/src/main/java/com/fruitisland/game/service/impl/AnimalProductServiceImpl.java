package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.AnimalProduct;
import com.fruitisland.game.mapper.AnimalProductMapper;
import com.fruitisland.game.service.AnimalProductService;
import org.springframework.stereotype.Service;

@Service
public class AnimalProductServiceImpl extends BaseServiceImplX<AnimalProductMapper, AnimalProduct> implements AnimalProductService {
}
