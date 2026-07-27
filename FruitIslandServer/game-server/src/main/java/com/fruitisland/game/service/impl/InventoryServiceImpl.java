package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.Inventory;
import com.fruitisland.game.mapper.InventoryMapper;
import com.fruitisland.game.service.InventoryService;
import org.springframework.stereotype.Service;

@Service
public class InventoryServiceImpl extends BaseServiceImplX<InventoryMapper, Inventory> implements InventoryService {
}
