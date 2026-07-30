package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.dto.DrinkBarListingResultVO;
import com.fruitisland.game.dto.DrinkBarCollectionResultVO;
import com.fruitisland.game.dto.DrinkBarStateVO;
import com.fruitisland.game.dto.DrinkBarTakeDownResultVO;
import com.fruitisland.game.service.DrinkBarService;
import com.fruitisland.game.service.GamePlayerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/drink-shop/bars")
@RequiredArgsConstructor
public class DrinkBarController {

    private final GamePlayerService gamePlayerService;
    private final DrinkBarService drinkBarService;

    @GetMapping
    public Result<DrinkBarStateVO> getBars(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = userId == null ? null : gamePlayerService.findByUserId(userId);
        if (player == null) {
            return Result.fail("玩家不存在");
        }
        try {
            return Result.ok(drinkBarService.getBars(player.getId()));
        } catch (RuntimeException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @PostMapping("/{barId}/list")
    public Result<DrinkBarListingResultVO> listDrink(
            @PathVariable Long barId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        var player = userId == null ? null : gamePlayerService.findByUserId(userId);
        if (player == null) {
            return Result.fail("玩家不存在");
        }
        Object recipeValue = body.get("recipeId");
        if (!(recipeValue instanceof String recipeId) || recipeId.isBlank()) {
            return Result.fail("配方不能为空");
        }
        try {
            return Result.ok(drinkBarService.listDrink(player.getId(), barId, recipeId));
        } catch (RuntimeException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @PostMapping("/{barId}/take-down")
    public Result<DrinkBarTakeDownResultVO> takeDown(
            @PathVariable Long barId,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        var player = userId == null ? null : gamePlayerService.findByUserId(userId);
        if (player == null) {
            return Result.fail("玩家不存在");
        }
        try {
            return Result.ok(drinkBarService.takeDown(player.getId(), barId));
        } catch (RuntimeException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @PostMapping("/{barId}/collect")
    public Result<DrinkBarCollectionResultVO> collect(
            @PathVariable Long barId,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        var player = userId == null ? null : gamePlayerService.findByUserId(userId);
        if (player == null) {
            return Result.fail("玩家不存在");
        }
        try {
            return Result.ok(drinkBarService.collect(player.getId(), barId));
        } catch (RuntimeException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @PostMapping("/collect-all")
    public Result<DrinkBarCollectionResultVO> collectAll(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = userId == null ? null : gamePlayerService.findByUserId(userId);
        if (player == null) {
            return Result.fail("玩家不存在");
        }
        try {
            return Result.ok(drinkBarService.collectAll(player.getId()));
        } catch (RuntimeException exception) {
            return Result.fail(exception.getMessage());
        }
    }

}
