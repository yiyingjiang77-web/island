package com.fruitisland.game.controller;

import com.fruitisland.game.dto.CustomerQueueVO;
import com.fruitisland.game.dto.CraftingStationVO;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.service.CustomerQueueService;
import com.fruitisland.game.service.DrinkShopService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.OrderFulfillmentService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class DrinkShopControllerTest {
    @Test
    void craftUsesThePlayerResolvedFromJwtUserId() {
        GamePlayerService playerService = mock(GamePlayerService.class);
        DrinkShopService drinkShopService = mock(DrinkShopService.class);
        CustomerQueueService queueService = mock(CustomerQueueService.class);
        OrderFulfillmentService fulfillmentService = mock(OrderFulfillmentService.class);
        DrinkShopController controller = new DrinkShopController(
                playerService, drinkShopService, queueService, fulfillmentService);
        GamePlayer jwtPlayer = new GamePlayer();
        jwtPlayer.setId(7L);
        when(playerService.findByUserId(42L)).thenReturn(jwtPlayer);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 42L);

        var response = controller.craft(
                Map.of("recipeId", "strawberry_juice", "quantity", 4, "playerId", 999L), request);

        assertTrue(response.isSuccess());
        verify(drinkShopService).craft(jwtPlayer, "strawberry_juice", 4);
    }

    @Test
    void drinkShopStateIncludesTheJwtPlayersCustomerQueue() {
        GamePlayerService playerService = mock(GamePlayerService.class);
        DrinkShopService drinkShopService = mock(DrinkShopService.class);
        CustomerQueueService queueService = mock(CustomerQueueService.class);
        OrderFulfillmentService fulfillmentService = mock(OrderFulfillmentService.class);
        DrinkShopController controller = new DrinkShopController(
                playerService, drinkShopService, queueService, fulfillmentService);
        GamePlayer player = new GamePlayer();
        player.setId(7L);
        when(playerService.findByUserId(42L)).thenReturn(player);
        when(drinkShopService.getCraftingStation(7L))
                .thenReturn(new CraftingStationVO(List.of()));
        when(queueService.getQueue(7L)).thenReturn(new CustomerQueueVO(List.of(), null, 120));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 42L);

        var response = controller.getCraftingStation(request);

        assertTrue(response.isSuccess());
        verify(queueService).getQueue(7L);
        assertTrue(response.getData().getCustomers().isEmpty());
        assertTrue(response.getData().getArrivalIntervalSeconds() == 120);
    }

    @Test
    void deliveryUsesJwtPlayerAndIgnoresAnyBodyIdentity() {
        GamePlayerService playerService = mock(GamePlayerService.class);
        DrinkShopService drinkShopService = mock(DrinkShopService.class);
        CustomerQueueService queueService = mock(CustomerQueueService.class);
        OrderFulfillmentService fulfillmentService = mock(OrderFulfillmentService.class);
        DrinkShopController controller = new DrinkShopController(
                playerService, drinkShopService, queueService, fulfillmentService);
        GamePlayer player = new GamePlayer();
        player.setId(7L);
        when(playerService.findByUserId(42L)).thenReturn(player);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 42L);

        var response = controller.deliverFromInventory(11L, request);

        assertTrue(response.isSuccess());
        verify(fulfillmentService).deliverFromInventory(player, 11L);
    }

    @Test
    void makeAndDeliverUsesJwtPlayer() {
        GamePlayerService playerService = mock(GamePlayerService.class);
        OrderFulfillmentService fulfillment = mock(OrderFulfillmentService.class);
        DrinkShopController controller = new DrinkShopController(
                playerService, mock(DrinkShopService.class), mock(CustomerQueueService.class), fulfillment);
        GamePlayer player = new GamePlayer();
        player.setId(7L);
        when(playerService.findByUserId(42L)).thenReturn(player);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 42L);

        assertTrue(controller.makeAndDeliver(
                11L, Map.of("quantity", 4, "playerId", 999L), request).isSuccess());
        verify(fulfillment).makeAndDeliver(player, 11L, 4);
    }

    @Test
    void outOfStockUsesJwtPlayer() {
        GamePlayerService playerService = mock(GamePlayerService.class);
        OrderFulfillmentService fulfillment = mock(OrderFulfillmentService.class);
        DrinkShopController controller = new DrinkShopController(
                playerService, mock(DrinkShopService.class), mock(CustomerQueueService.class), fulfillment);
        GamePlayer player = new GamePlayer();
        player.setId(7L);
        when(playerService.findByUserId(42L)).thenReturn(player);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 42L);

        assertTrue(controller.markOutOfStock(11L, request).isSuccess());
        verify(fulfillment).markOutOfStock(player, 11L);
    }
}
