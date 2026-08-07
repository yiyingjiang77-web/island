package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.dto.RecipeShopVO;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.RecipeShopService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 配方商店接口。
 *
 * GET  /recipe-shop/list  — 获取配方商店列表（含购买状态）
 * POST /recipe-shop/buy   — 购买配方（扣金币 + 授予永久使用权）
 */
@RestController
@RequestMapping("/recipe-shop")
@RequiredArgsConstructor
public class RecipeShopController {

    private final GamePlayerService gamePlayerService;
    private final RecipeShopService recipeShopService;

    @GetMapping("/list")
    public Result<RecipeShopVO> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");
        RecipeShopVO vo = recipeShopService.listRecipes(player.getId());
        vo.setPlayerGold(player.getGold());
        return Result.ok(vo);
    }

    @PostMapping("/buy")
    public Result<RecipeShopVO.RecipeItem> buy(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            String recipeId = String.valueOf(body.get("recipeId"));
            return Result.ok(recipeShopService.buyRecipe(player.getId(), recipeId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
