package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.Inventory;

public interface InventoryService extends BaseServiceX<Inventory> {

    /** 根据玩家和物品查找 */
    Inventory findByPlayerAndItem(Long playerId, String itemId);

    /** 添加物品到背包（不存在则创建） */
    void addItem(Long playerId, String itemId, int count);
}
