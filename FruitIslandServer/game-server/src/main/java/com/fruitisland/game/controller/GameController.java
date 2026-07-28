package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.dto.GameInitVO;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.Island;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.IslandService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 游戏主 Controller
 *
 * GET /game/init — 游戏初始化（数据驱动入口）
 */
@Slf4j
@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {

    private final GamePlayerService gamePlayerService;
    private final IslandService islandService;

    /**
     * 游戏初始化
     *
     * 客户端进入 MainScene 前调用，获取玩家所有游戏数据
     *
     * 请求头: Authorization: Bearer <token>
     * 响应: {"code": 0, "data": {"player": {...}, "island": {...}}}
     */
    @GetMapping("/init")
    public Result<GameInitVO> gameInit(HttpServletRequest request) {
        // 从 JWT 拦截器注入的 userId 获取
        Long userId = (Long) request.getAttribute("userId");
        log.info("游戏初始化请求: userId={}", userId);

        // 1. 查找或创建游戏角色
        GamePlayer player = gamePlayerService.findByUserId(userId);
        if (player == null) {
            player = gamePlayerService.createPlayer(userId);
            log.info("新角色创建: playerId={}", player.getId());
        }

        // 2. 查找或创建岛屿
        Island island = islandService.findByPlayerId(player.getId());
        if (island == null) {
            island = islandService.createIsland(player.getId());
            log.info("新岛屿创建: islandId={}", island.getId());
        }

        GameInitVO vo = GameInitVO.of(player, island);
        return Result.ok(vo);
    }
}
