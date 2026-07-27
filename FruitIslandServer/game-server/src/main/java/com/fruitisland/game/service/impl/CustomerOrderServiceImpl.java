package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CustomerOrder;
import com.fruitisland.game.mapper.CustomerOrderMapper;
import com.fruitisland.game.service.CustomerOrderService;
import org.springframework.stereotype.Service;

@Service
public class CustomerOrderServiceImpl extends BaseServiceImplX<CustomerOrderMapper, CustomerOrder> implements CustomerOrderService {
}
