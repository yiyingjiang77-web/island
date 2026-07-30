package com.fruitisland.game.service;

import com.fruitisland.game.dto.OrderFulfillmentResultVO;
import com.fruitisland.game.entity.GamePlayer;

public interface OrderFulfillmentService {
    OrderFulfillmentResultVO deliverFromInventory(GamePlayer player, Long orderId);
    OrderFulfillmentResultVO makeAndDeliver(GamePlayer player, Long orderId, int makeQuantity);
    OrderFulfillmentResultVO markOutOfStock(GamePlayer player, Long orderId);
}
