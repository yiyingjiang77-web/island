package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.entity.PlayerBeehive;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerBeehiveService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 蜂箱接口。
 *
 * POST /beehive/purchase — 购买蜂箱（最多 3 个，递增价格）
 * POST /beehive/collect  — 收取已产出的蜂蜜入背包
 * GET  /beehive/status   — 查看蜂箱当前状态（含惰性结算）
 */
@RestController
@RequestMapping("/beehive")
@RequiredArgsConstructor
public class BeehiveController {

    private final GamePlayerService gamePlayerService;
    private final PlayerBeehiveService playerBeehiveService;

    /**
     * 购买一个蜂箱。
     */
    @PostMapping("/purchase")
    public Result<PlayerBeehive> purchase(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            return Result.ok(playerBeehiveService.purchaseBeehive(player.getId()));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 收取蜂蜜。
     *
     * 返回本次收取的蜂蜜数量。
     */
    @PostMapping("/collect")
    public Result<Map<String, Integer>> collect(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            int collected = playerBeehiveService.collectHoney(player.getId());
            return Result.ok(Map.of("honeyCollected", collected));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 查看蜂箱状态（触发惰性结算后返回最新数据）。
     */
    @GetMapping("/status")
    public Result<PlayerBeehive> status(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        return Result.ok(playerBeehiveService.settleProduction(player.getId()));
    }
}
