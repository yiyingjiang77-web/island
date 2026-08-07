package com.fruitisland.game.service.impl;

import com.fruitisland.game.dto.ExpGainResult;
import com.fruitisland.game.entity.CustomerOrder;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.Inventory;
import com.fruitisland.game.entity.PlayerRecipe;
import com.fruitisland.game.entity.RecipeConfig;
import com.fruitisland.game.entity.RecipeMaterial;
import com.fruitisland.game.service.CustomerOrderService;
import com.fruitisland.game.service.CustomerQueueService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.InventoryService;
import com.fruitisland.game.service.PlayerRecipeService;
import com.fruitisland.game.service.RecipeConfigService;
import com.fruitisland.game.service.RecipeMaterialService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderFulfillmentServiceImplTest {
    @Test
    void deliversTheWholeOrderFromInventoryAndUsesItsRewardSnapshot() {
        InventoryService inventory = mock(InventoryService.class);
        CustomerOrderService orders = mock(CustomerOrderService.class);
        GamePlayerService players = mock(GamePlayerService.class);
        CustomerQueueService queue = mock(CustomerQueueService.class);
        CustomerOrder order = waitingOrder(11L, 7L, 3, 30, 5);
        GamePlayer player = player(7L, 100L);
        when(orders.lockWaiting(11L, 7L)).thenReturn(order);
        when(players.getById(7L)).thenReturn(player);
        when(players.settleDrinkSaleReward(7L, 90, 15)).thenAnswer(invocation -> {
            player.setGold(190L);
            player.setCumulativeExp(15);
            return new ExpGainResult(15, 1, 1, 15, 100, 0, null);
        });

        var service = new OrderFulfillmentServiceImpl(
                inventory, orders, players, queue, mock(PlayerRecipeService.class),
                mock(RecipeConfigService.class), mock(RecipeMaterialService.class), fixedClock());
        var result = service.deliverFromInventory(player, 11L);

        verify(inventory).removeItem(7L, "strawberry_juice", 3);
        verify(players).settleDrinkSaleReward(7L, 90, 15);
        verify(orders).updateById(argThat(value ->
                "DELIVERED".equals(value.getStatus())
                        && "INVENTORY".equals(value.getCloseReason())
                        && value.getCloseTime() != null));
        verify(queue).recordDeparture(7L);
        assertEquals(90, result.getEarnedGold());
        assertEquals(15, result.getEarnedExp());
        assertEquals(3, result.getDeliveredQuantity());
    }

    @Test
    void insufficientInventoryLeavesTheOrderAndRewardsUntouched() {
        InventoryService inventory = mock(InventoryService.class);
        CustomerOrderService orders = mock(CustomerOrderService.class);
        GamePlayerService players = mock(GamePlayerService.class);
        CustomerQueueService queue = mock(CustomerQueueService.class);
        GamePlayer player = player(7L, 100L);
        when(orders.lockWaiting(11L, 7L)).thenReturn(waitingOrder(11L, 7L, 2, 30, 5));
        doThrow(new IllegalArgumentException("库存不足"))
                .when(inventory).removeItem(7L, "strawberry_juice", 2);

        var service = new OrderFulfillmentServiceImpl(
                inventory, orders, players, queue, mock(PlayerRecipeService.class),
                mock(RecipeConfigService.class), mock(RecipeMaterialService.class), fixedClock());

        assertThrows(IllegalArgumentException.class,
                () -> service.deliverFromInventory(player, 11L));
        verify(players, never()).settleDrinkSaleReward(anyLong(), anyInt(), anyInt());
        verify(orders, never()).updateById(any());
        verify(queue, never()).recordDeparture(anyLong());
    }

    @Test
    void craftsTheChosenQuantityDeliversTheOrderAndStoresOnlyExcess() {
        InventoryService inventory = mock(InventoryService.class);
        CustomerOrderService orders = mock(CustomerOrderService.class);
        GamePlayerService players = mock(GamePlayerService.class);
        CustomerQueueService queue = mock(CustomerQueueService.class);
        PlayerRecipeService qualifications = mock(PlayerRecipeService.class);
        RecipeConfigService recipes = mock(RecipeConfigService.class);
        RecipeMaterialService materials = mock(RecipeMaterialService.class);
        GamePlayer player = player(7L, 100L);
        when(orders.lockWaiting(11L, 7L)).thenReturn(waitingOrder(11L, 7L, 2, 30, 5));
        when(players.getById(7L)).thenReturn(player);
        when(players.settleDrinkSaleReward(7L, 60, 10))
                .thenReturn(new ExpGainResult(10, 1, 1, 10, 100, 0, null));
        when(qualifications.findActive(7L, "strawberry_juice")).thenReturn(new PlayerRecipe());
        RecipeConfig recipe = new RecipeConfig();
        recipe.setId("strawberry_juice");
        recipe.setOutputItem("strawberry_juice");
        recipe.setEnabled(1);
        when(recipes.getById("strawberry_juice")).thenReturn(recipe);
        RecipeMaterial material = new RecipeMaterial();
        material.setItemId("strawberry");
        material.setCount(3);
        when(materials.listByRecipe("strawberry_juice")).thenReturn(java.util.List.of(material));
        Inventory raw = new Inventory();
        raw.setCount(20);
        when(inventory.findByPlayerAndItem(7L, "strawberry")).thenReturn(raw);

        var service = new OrderFulfillmentServiceImpl(
                inventory, orders, players, queue, qualifications, recipes, materials, fixedClock());
        var result = service.makeAndDeliver(player, 11L, 4);

        verify(inventory).removeItem(7L, "strawberry", 12);
        verify(inventory).addItem(7L, "strawberry_juice", 2);
        verify(inventory, never()).removeItem(7L, "strawberry_juice", 2);
        assertEquals(2, result.getDeliveredQuantity());
        assertEquals(4, result.getCraftedQuantity());
        assertEquals(2, result.getExcessQuantity());
    }

    @Test
    void expiredQualificationCanStillMakeTheExactQuantityForItsExistingOrder() {
        InventoryService inventory = mock(InventoryService.class);
        CustomerOrderService orders = mock(CustomerOrderService.class);
        GamePlayerService players = mock(GamePlayerService.class);
        PlayerRecipeService qualifications = mock(PlayerRecipeService.class);
        RecipeConfigService recipes = mock(RecipeConfigService.class);
        RecipeMaterialService materials = mock(RecipeMaterialService.class);
        GamePlayer player = player(7L, 100L);
        when(orders.lockWaiting(11L, 7L)).thenReturn(waitingOrder(11L, 7L, 2, 30, 5));
        RecipeConfig disabledRecipe = new RecipeConfig();
        disabledRecipe.setId("strawberry_juice");
        disabledRecipe.setOutputItem("strawberry_juice");
        disabledRecipe.setEnabled(0);
        when(recipes.getById("strawberry_juice")).thenReturn(disabledRecipe);
        RecipeMaterial material = new RecipeMaterial();
        material.setItemId("strawberry");
        material.setCount(2);
        when(materials.listByRecipe("strawberry_juice")).thenReturn(java.util.List.of(material));
        Inventory raw = new Inventory();
        raw.setCount(4);
        when(inventory.findByPlayerAndItem(7L, "strawberry")).thenReturn(raw);
        when(players.settleDrinkSaleReward(7L, 60, 10))
                .thenReturn(new ExpGainResult(10, 1, 1, 10, 100, 0, null));
        when(players.getById(7L)).thenReturn(player);
        var service = new OrderFulfillmentServiceImpl(
                inventory, orders, players, mock(CustomerQueueService.class),
                qualifications, recipes, materials, fixedClock());

        var result = service.makeAndDeliver(player, 11L, 2);

        assertEquals(2, result.getCraftedQuantity());
        assertEquals(0, result.getExcessQuantity());
        verify(inventory).removeItem(7L, "strawberry", 4);
    }

    @Test
    void expiredQualificationCannotMakeMoreThanItsExistingOrder() {
        CustomerOrderService orders = mock(CustomerOrderService.class);
        GamePlayer player = player(7L, 100L);
        when(orders.lockWaiting(11L, 7L)).thenReturn(waitingOrder(11L, 7L, 2, 30, 5));
        var service = new OrderFulfillmentServiceImpl(
                mock(InventoryService.class), orders, mock(GamePlayerService.class),
                mock(CustomerQueueService.class), mock(PlayerRecipeService.class),
                mock(RecipeConfigService.class), mock(RecipeMaterialService.class), fixedClock());

        assertThrows(IllegalArgumentException.class,
                () -> service.makeAndDeliver(player, 11L, 3));
    }

    @Test
    void refusesCraftingLessThanTheWholeOrderBeforeWritingAnything() {
        InventoryService inventory = mock(InventoryService.class);
        CustomerOrderService orders = mock(CustomerOrderService.class);
        GamePlayer player = player(7L, 100L);
        when(orders.lockWaiting(11L, 7L)).thenReturn(waitingOrder(11L, 7L, 3, 30, 5));
        var service = new OrderFulfillmentServiceImpl(
                inventory, orders, mock(GamePlayerService.class), mock(CustomerQueueService.class),
                mock(PlayerRecipeService.class), mock(RecipeConfigService.class),
                mock(RecipeMaterialService.class), fixedClock());

        assertThrows(IllegalArgumentException.class,
                () -> service.makeAndDeliver(player, 11L, 2));
        verifyNoInteractions(inventory);
        verify(orders, never()).updateById(any());
    }

    @Test
    void closesAnOutOfStockOrderWithoutInventoryOrRewards() {
        InventoryService inventory = mock(InventoryService.class);
        CustomerOrderService orders = mock(CustomerOrderService.class);
        GamePlayerService players = mock(GamePlayerService.class);
        CustomerQueueService queue = mock(CustomerQueueService.class);
        GamePlayer player = player(7L, 100L);
        CustomerOrder order = waitingOrder(11L, 7L, 2, 30, 5);
        when(orders.lockWaiting(11L, 7L)).thenReturn(order);
        var service = new OrderFulfillmentServiceImpl(
                inventory, orders, players, queue, mock(PlayerRecipeService.class),
                mock(RecipeConfigService.class), mock(RecipeMaterialService.class), fixedClock());

        var result = service.markOutOfStock(player, 11L);

        assertEquals("OUT_OF_STOCK", result.getStatus());
        assertEquals(0, result.getEarnedGold());
        assertEquals(0, result.getEarnedExp());
        verify(orders).updateById(argThat(value ->
                "OUT_OF_STOCK".equals(value.getStatus())
                        && "OUT_OF_STOCK".equals(value.getCloseReason())
                        && value.getCloseTime() != null));
        verify(queue).recordDeparture(7L);
        verifyNoInteractions(inventory, players);
    }

    @Test
    void cannotCloseAnOrderThatIsNotWaitingForTheJwtPlayer() {
        CustomerOrderService orders = mock(CustomerOrderService.class);
        GamePlayer player = player(7L, 100L);
        when(orders.lockWaiting(11L, 7L)).thenReturn(null);
        var service = new OrderFulfillmentServiceImpl(
                mock(InventoryService.class), orders, mock(GamePlayerService.class),
                mock(CustomerQueueService.class), mock(PlayerRecipeService.class),
                mock(RecipeConfigService.class), mock(RecipeMaterialService.class), fixedClock());

        assertThrows(IllegalArgumentException.class, () -> service.markOutOfStock(player, 11L));
        verify(orders, never()).updateById(any());
    }

    private static CustomerOrder waitingOrder(
            long id, long playerId, int quantity, int gold, int exp) {
        CustomerOrder order = new CustomerOrder();
        order.setId(id);
        order.setPlayerId(playerId);
        order.setItemId("strawberry_juice");
        order.setRecipeId("strawberry_juice");
        order.setQuantity(quantity);
        order.setUnitGoldSnapshot(gold);
        order.setUnitExpSnapshot(exp);
        order.setStatus("WAITING");
        return order;
    }

    private static GamePlayer player(long id, long gold) {
        GamePlayer player = new GamePlayer();
        player.setId(id);
        player.setGold(gold);
        player.setLevel(1);
        player.setExp(0);
        player.setCumulativeExp(0);
        return player;
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-29T02:00:00Z"), ZoneOffset.UTC);
    }
}
