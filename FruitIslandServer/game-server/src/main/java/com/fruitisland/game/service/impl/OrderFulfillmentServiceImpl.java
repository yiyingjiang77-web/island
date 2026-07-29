package com.fruitisland.game.service.impl;

import com.fruitisland.game.dto.ExpGainResult;
import com.fruitisland.game.dto.OrderFulfillmentResultVO;
import com.fruitisland.game.entity.CustomerOrder;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.RecipeConfig;
import com.fruitisland.game.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderFulfillmentServiceImpl implements OrderFulfillmentService {
    private final InventoryService inventoryService;
    private final CustomerOrderService customerOrderService;
    private final GamePlayerService gamePlayerService;
    private final CustomerQueueService customerQueueService;
    private final PlayerRecipeService playerRecipeService;
    private final RecipeConfigService recipeConfigService;
    private final RecipeMaterialService recipeMaterialService;
    private final Clock clock;

    @Override
    @Transactional
    public OrderFulfillmentResultVO deliverFromInventory(GamePlayer player, Long orderId) {
        if (player == null || player.getId() == null) throw new IllegalArgumentException("玩家不存在");
        CustomerOrder order = customerOrderService.lockWaiting(orderId, player.getId());
        if (order == null) throw new IllegalArgumentException("订单不存在或已关闭");
        int quantity = positive(order.getQuantity(), "订单数量无效");
        inventoryService.removeItem(player.getId(), order.getItemId(), quantity);
        return closeDelivered(player.getId(), order, quantity, 0, 0, "INVENTORY");
    }

    @Override
    @Transactional
    public OrderFulfillmentResultVO makeAndDeliver(
            GamePlayer player, Long orderId, int makeQuantity) {
        if (player == null || player.getId() == null) throw new IllegalArgumentException("玩家不存在");
        if (makeQuantity < 1 || makeQuantity > 99) {
            throw new IllegalArgumentException("制作数量必须为 1–99");
        }
        CustomerOrder order = customerOrderService.lockWaiting(orderId, player.getId());
        if (order == null) throw new IllegalArgumentException("订单不存在或已关闭");
        int ordered = positive(order.getQuantity(), "订单数量无效");
        if (makeQuantity < ordered) throw new IllegalArgumentException("制作数量必须覆盖完整订单");
        boolean qualificationActive =
                playerRecipeService.findActive(player.getId(), order.getRecipeId()) != null;
        if (!qualificationActive && makeQuantity != ordered) {
            throw new IllegalArgumentException("已过期配方只能制作订单要求的准确数量");
        }
        RecipeConfig recipe = recipeConfigService.getById(order.getRecipeId());
        if (recipe == null || !order.getItemId().equals(recipe.getOutputItem())) {
            throw new IllegalArgumentException("订单配方无效");
        }
        var materials = recipeMaterialService.listByRecipe(order.getRecipeId());
        if (materials.isEmpty()) throw new IllegalArgumentException("配方材料未配置");
        for (var material : materials) {
            int required = Math.multiplyExact(positive(material.getCount(), "配方材料无效"), makeQuantity);
            var inventory = inventoryService.findByPlayerAndItem(player.getId(), material.getItemId());
            if (inventory == null || inventory.getCount() == null || inventory.getCount() < required) {
                throw new IllegalArgumentException("材料不足");
            }
        }
        for (var material : materials) {
            inventoryService.removeItem(
                    player.getId(), material.getItemId(), material.getCount() * makeQuantity);
        }
        int excess = makeQuantity - ordered;
        if (excess > 0) inventoryService.addItem(player.getId(), order.getItemId(), excess);
        return closeDelivered(player.getId(), order, ordered, makeQuantity, excess, "MADE_TO_ORDER");
    }

    @Override
    @Transactional
    public OrderFulfillmentResultVO markOutOfStock(GamePlayer player, Long orderId) {
        if (player == null || player.getId() == null) throw new IllegalArgumentException("玩家不存在");
        CustomerOrder order = customerOrderService.lockWaiting(orderId, player.getId());
        if (order == null) throw new IllegalArgumentException("订单不存在或已关闭");
        order.setStatus("OUT_OF_STOCK");
        order.setCloseReason("OUT_OF_STOCK");
        order.setCloseTime(LocalDateTime.now(clock));
        customerOrderService.updateById(order);
        customerQueueService.recordDeparture(player.getId());
        return new OrderFulfillmentResultVO(
                order.getId(), order.getStatus(), 0, 0, 0, 0, 0,
                player.getLevel(), player.getExp(), player.getGold());
    }

    private OrderFulfillmentResultVO closeDelivered(
            Long playerId, CustomerOrder order, int delivered, int crafted, int excess, String reason) {
        int gold = Math.multiplyExact(delivered, nonNegative(order.getUnitGoldSnapshot()));
        int exp = Math.multiplyExact(delivered, nonNegative(order.getUnitExpSnapshot()));
        ExpGainResult expResult = gamePlayerService.settleDrinkSaleReward(playerId, gold, exp);
        order.setStatus("DELIVERED");
        order.setCloseReason(reason);
        order.setCloseTime(LocalDateTime.now(clock));
        customerOrderService.updateById(order);
        customerQueueService.recordDeparture(playerId);
        GamePlayer refreshed = gamePlayerService.getById(playerId);
        if (refreshed == null) throw new IllegalArgumentException("玩家不存在");
        return new OrderFulfillmentResultVO(
                order.getId(), order.getStatus(), delivered, crafted, excess, gold, exp,
                expResult.getAfterLevel(), expResult.getCurrentExp(), refreshed.getGold());
    }

    private static int positive(Integer value, String message) {
        if (value == null || value <= 0) throw new IllegalArgumentException(message);
        return value;
    }

    private static int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }
}
