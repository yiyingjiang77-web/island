package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.dto.CraftResultVO;
import com.fruitisland.game.dto.CraftingStationVO;
import com.fruitisland.game.service.DrinkShopService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.CustomerQueueService;
import com.fruitisland.game.service.OrderFulfillmentService;
import com.fruitisland.game.service.SatisfactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/drink-shop")
@RequiredArgsConstructor
public class DrinkShopController {
    private final GamePlayerService gamePlayerService;
    private final DrinkShopService drinkShopService;
    private final CustomerQueueService customerQueueService;
    private final OrderFulfillmentService orderFulfillmentService;
    private final SatisfactionService satisfactionService;

    @GetMapping
    public Result<CraftingStationVO> getCraftingStation(HttpServletRequest request) {
        var player = playerFromJwt(request);
        if (player == null) return Result.fail("玩家不存在");
        CraftingStationVO station = drinkShopService.getCraftingStation(player.getId());
        station.includeQueue(customerQueueService.getQueue(player.getId()));
        return Result.ok(station);
    }

    @GetMapping("/progress")
    public Result<com.fruitisland.game.dto.DrinkShopProgressVO> getProgress(HttpServletRequest request) {
        var player = playerFromJwt(request);
        if (player == null) return Result.fail("玩家不存在");
        try { return Result.ok(drinkShopService.getProgress(player)); }
        catch (RuntimeException exception) { return Result.fail(exception.getMessage()); }
    }

    @GetMapping("/satisfaction")
    public Result<com.fruitisland.game.dto.SatisfactionStatusVO> getSatisfaction(HttpServletRequest request) {
        var player = playerFromJwt(request);
        if (player == null) return Result.fail("玩家不存在");
        try { return Result.ok(satisfactionService.getStatus(player.getId())); }
        catch (RuntimeException exception) { return Result.fail(exception.getMessage()); }
    }

    @PostMapping("/renovate")
    public Result<com.fruitisland.game.dto.DrinkShopRenovationVO> renovate(
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        var player = playerFromJwt(request);
        if (player == null) return Result.fail("玩家不存在");
        try {
            Object value = body.get("targetLevel");
            if (!(value instanceof Number target)) return Result.fail("目标等级不能为空");
            return Result.ok(drinkShopService.renovate(player, target.intValue()));
        } catch (RuntimeException exception) { return Result.fail(exception.getMessage()); }
    }

    @PostMapping("/craft")
    public Result<CraftResultVO> craft(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        var player = playerFromJwt(request);
        if (player == null) return Result.fail("玩家不存在");
        try {
            Object recipeValue = body.get("recipeId");
            Object quantityValue = body.get("quantity");
            if (!(recipeValue instanceof String recipeId) || !(quantityValue instanceof Number quantity)) {
                return Result.fail("配方和制作数量不能为空");
            }
            return Result.ok(drinkShopService.craft(player, recipeId, quantity.intValue()));
        } catch (RuntimeException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/deliver")
    public Result<com.fruitisland.game.dto.OrderFulfillmentResultVO> deliverFromInventory(
            @PathVariable Long orderId, HttpServletRequest request) {
        var player = playerFromJwt(request);
        if (player == null) return Result.fail("玩家不存在");
        try {
            return Result.ok(orderFulfillmentService.deliverFromInventory(player, orderId));
        } catch (RuntimeException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/make-and-deliver")
    public Result<com.fruitisland.game.dto.OrderFulfillmentResultVO> makeAndDeliver(
            @PathVariable Long orderId, @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        var player = playerFromJwt(request);
        if (player == null) return Result.fail("玩家不存在");
        try {
            Object quantity = body.get("quantity");
            if (!(quantity instanceof Number value)) return Result.fail("制作数量不能为空");
            return Result.ok(orderFulfillmentService.makeAndDeliver(
                    player, orderId, value.intValue()));
        } catch (RuntimeException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/out-of-stock")
    public Result<com.fruitisland.game.dto.OrderFulfillmentResultVO> markOutOfStock(
            @PathVariable Long orderId, HttpServletRequest request) {
        var player = playerFromJwt(request);
        if (player == null) return Result.fail("玩家不存在");
        try {
            return Result.ok(orderFulfillmentService.markOutOfStock(player, orderId));
        } catch (RuntimeException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    private com.fruitisland.game.entity.GamePlayer playerFromJwt(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userId == null ? null : gamePlayerService.findByUserId(userId);
    }
}
