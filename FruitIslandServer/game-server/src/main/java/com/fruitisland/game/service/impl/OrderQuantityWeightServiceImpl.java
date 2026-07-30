package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.OrderQuantityWeight;
import com.fruitisland.game.mapper.OrderQuantityWeightMapper;
import com.fruitisland.game.service.OrderQuantityWeightService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderQuantityWeightServiceImpl
        extends BaseServiceImplX<OrderQuantityWeightMapper, OrderQuantityWeight>
        implements OrderQuantityWeightService {
    @Override
    public List<OrderQuantityWeight> listEnabled() {
        return lambdaQuery()
                .eq(OrderQuantityWeight::getEnabled, 1)
                .orderByAsc(OrderQuantityWeight::getQuantity)
                .list();
    }
}
