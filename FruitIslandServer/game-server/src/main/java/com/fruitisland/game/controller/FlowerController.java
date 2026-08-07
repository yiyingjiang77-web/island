package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.entity.PlayerFlowerRight;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerFlowerRightService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 花卉接口。
 *
 * <p>花卉种植/收获复用 {@link FarmController} 的 /farm/plant 和 /farm/harvest，
 * 此 Controller 仅提供购买种植权和升级两个操作。</p>
 *
 * POST /flower/purchase — 购买花卉永久种植权（金币或钻石）
 * POST /flower/upgrade  — 使用金币升级花卉等级
 */
@RestController
@RequestMapping("/flower")
@RequiredArgsConstructor
public class FlowerController {

    private final GamePlayerService gamePlayerService;
    private final PlayerFlowerRightService playerFlowerRightService;

    /**
     * 购买花卉永久种植权。
     *
     * body: {"flowerId": "rose"}
     */
    @PostMapping("/purchase")
    public Result<PlayerFlowerRight> purchase(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            String flowerId = String.valueOf(body.get("flowerId"));
            return Result.ok(playerFlowerRightService.purchase(player.getId(), flowerId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 升级花卉等级（消耗金币）。
     *
     * body: {"flowerId": "rose"}
     */
    @PostMapping("/upgrade")
    public Result<PlayerFlowerRight> upgrade(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            String flowerId = String.valueOf(body.get("flowerId"));
            return Result.ok(playerFlowerRightService.upgrade(player.getId(), flowerId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
