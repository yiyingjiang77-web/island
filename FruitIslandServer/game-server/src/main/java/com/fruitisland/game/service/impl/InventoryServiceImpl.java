package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.Inventory;
import com.fruitisland.game.mapper.InventoryMapper;
import com.fruitisland.game.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl extends BaseServiceImplX<InventoryMapper, Inventory> implements InventoryService {

    @Override
    public Inventory findByPlayerAndItem(Long playerId, String itemId) {
        return baseMapper.selectByPlayerAndItem(playerId, itemId);
    }

    @Override
    @Transactional
    public void addItem(Long playerId, String itemId, int count) {
        Inventory inv = baseMapper.selectByPlayerAndItem(playerId, itemId);
        if (inv != null) {
            inv.setCount(inv.getCount() + count);
            updateById(inv);
        } else {
            inv = new Inventory();
            inv.setPlayerId(playerId);
            inv.setItemId(itemId);
            inv.setCount(count);
            save(inv);
        }
    }

    @Override
    @Transactional
    public void removeItem(Long playerId, String itemId, int count) {
        if (count <= 0) throw new IllegalArgumentException("扣减数量必须为正数");
        if (baseMapper.decrementIfEnough(playerId, itemId, count) != 1) {
            throw new IllegalArgumentException("材料不足");
        }
    }
}
