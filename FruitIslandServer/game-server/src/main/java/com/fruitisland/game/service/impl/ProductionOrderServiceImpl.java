package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.ProductionOrder;
import com.fruitisland.game.mapper.ProductionOrderMapper;
import com.fruitisland.game.service.ProductionOrderService;
import org.springframework.stereotype.Service;

@Service
public class ProductionOrderServiceImpl extends BaseServiceImplX<ProductionOrderMapper, ProductionOrder> implements ProductionOrderService {
}
