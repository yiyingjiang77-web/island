package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.OrderQuantityWeight;

import java.util.List;

public interface OrderQuantityWeightService extends BaseServiceX<OrderQuantityWeight> {
    List<OrderQuantityWeight> listEnabled();
}
