package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.dto.FlowerCatalogVO;
import com.fruitisland.game.entity.PlayerFlowerRight;
import com.fruitisland.game.service.FlowerConfigService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerFlowerRightService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 花店目录和永久花卉种植权。 */
@RestController
@RequestMapping("/flower")
@RequiredArgsConstructor
public class FlowerController {
    private final FlowerConfigService flowerConfigService;
    private final PlayerFlowerRightService playerFlowerRightService;
    private final GamePlayerService gamePlayerService;

    @GetMapping("/catalog")
    public Result<FlowerCatalogVO> catalog(HttpServletRequest request) {
        var player = requirePlayer(request);
        if (player == null) return Result.fail("玩家不存在");
        var flowers = flowerConfigService.lambdaQuery()
                .eq(com.fruitisland.game.entity.FlowerConfig::getEnabled, 1)
                .orderByAsc(com.fruitisland.game.entity.FlowerConfig::getPurchasePrice)
                .list();
        return Result.ok(new FlowerCatalogVO(
                flowers,
                playerFlowerRightService.listByPlayer(player.getId())
        ));
    }

    @PostMapping("/buy")
    public Result<PlayerFlowerRight> buy(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        var player = requirePlayer(request);
        if (player == null) return Result.fail("玩家不存在");
        Object rawFlowerId = body.get("flowerId");
        if (!(rawFlowerId instanceof String flowerId) || flowerId.isBlank()) {
            return Result.fail("缺少 flowerId");
        }
        try {
            return Result.ok(playerFlowerRightService.purchase(player.getId(), flowerId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    private com.fruitisland.game.entity.GamePlayer requirePlayer(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return gamePlayerService.findByUserId(userId);
    }
}
