package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CustomerOrder;
import com.fruitisland.game.mapper.CustomerOrderMapper;
import com.fruitisland.game.service.CustomerOrderService;
import org.springframework.stereotype.Service;

@Service
public class CustomerOrderServiceImpl extends BaseServiceImplX<CustomerOrderMapper, CustomerOrder> implements CustomerOrderService {
    @Override
    public java.util.List<CustomerOrder> listWaiting(Long playerId) {
        return lambdaQuery()
                .eq(CustomerOrder::getPlayerId, playerId)
                .eq(CustomerOrder::getStatus, "WAITING")
                .orderByAsc(CustomerOrder::getQueuePosition)
                .list();
    }

    @Override
    public CustomerOrder lockWaiting(Long orderId, Long playerId) {
        return baseMapper.selectWaitingForUpdate(orderId, playerId);
    }
}
