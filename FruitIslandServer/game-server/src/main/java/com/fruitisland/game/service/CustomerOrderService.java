package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.CustomerOrder;
import java.util.List;

public interface CustomerOrderService extends BaseServiceX<CustomerOrder> {
    List<CustomerOrder> listWaiting(Long playerId);
    CustomerOrder lockWaiting(Long orderId, Long playerId);
}
