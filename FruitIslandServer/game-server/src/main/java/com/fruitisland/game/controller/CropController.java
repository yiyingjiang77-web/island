package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.entity.PlayerCrop;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerCropService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 玩家作物接口。
 */
@RestController
@RequestMapping("/crop")
@RequiredArgsConstructor
public class CropController {

    private final GamePlayerService gamePlayerService;
    private final PlayerCropService playerCropService;

    /**
     * 使用金币将永久作物提升一级。
     *
     * <p>限时奖励不写入 player_crop，因此天然无法调用本接口升级。</p>
     *
     * body: {"cropId": "strawberry"}
     */
    @PostMapping("/upgrade")
    public Result<PlayerCrop> upgrade(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            String cropId = String.valueOf(body.get("cropId"));
            return Result.ok(playerCropService.upgrade(player.getId(), cropId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
