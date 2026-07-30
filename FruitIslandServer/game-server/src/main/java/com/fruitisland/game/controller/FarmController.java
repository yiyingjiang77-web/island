package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.dto.HarvestResultVO;
import com.fruitisland.game.dto.LandVO;
import com.fruitisland.game.entity.PlayerLand;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerLandService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 农场 Controller
 *
 * GET  /farm/lands   — 获取所有土地状态
 * POST /farm/buy     — 购买土地
 * POST /farm/plant   — 种植
 * POST /farm/harvest — 收获
 */
@Slf4j
@RestController
@RequestMapping("/farm")
@RequiredArgsConstructor
public class FarmController {

    private final PlayerLandService playerLandService;
    private final GamePlayerService gamePlayerService;

    /**
     * 获取土地列表
     *
     * 返回 96 块土地的完整视图，状态根据玩家等级和已购买情况动态计算
     */
    @GetMapping("/lands")
    public Result<List<LandVO>> getLands(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) {
            return Result.fail("玩家不存在");
        }
        List<LandVO> lands = playerLandService.listByPlayer(player.getId(), player.getLevel());
        return Result.ok(lands);
    }

    /**
     * 购买土地
     *
     * body: {"landConfigId": 2}
     */
    @PostMapping("/buy")
    public Result<LandVO> buyLand(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) {
            return Result.fail("玩家不存在");
        }

        Long landConfigId = ((Number) body.get("landConfigId")).longValue();
        try {
            PlayerLand land = playerLandService.buy(player.getId(), landConfigId, player.getLevel());
            // 重新获取最新玩家数据（金币已扣）
            player = gamePlayerService.getById(player.getId());
            log.info("玩家 {} 购买土地成功, 剩余金币: {}", player.getId(), player.getGold());
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 种植
     *
     * body: {"playerLandId": 1, "cropId": "strawberry"}
     */
    @PostMapping("/plant")
    public Result<LandVO> plant(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) {
            return Result.fail("玩家不存在");
        }

        Long playerLandId = ((Number) body.get("playerLandId")).longValue();
        String cropId = (String) body.get("cropId");

        try {
            PlayerLand land = playerLandService.plant(player.getId(), playerLandId, cropId);
            return Result.ok(toLandVO(land));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 浇水
     *
     * body: {"playerLandId": 1}
     */
    @PostMapping("/water")
    public Result<LandVO> water(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) {
            return Result.fail("玩家不存在");
        }

        Long playerLandId = ((Number) body.get("playerLandId")).longValue();

        try {
            PlayerLand land = playerLandService.water(player.getId(), playerLandId);
            return Result.ok(toLandVO(land));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 收获
     *
     * body: {"playerLandId": 1}
     */
    @PostMapping("/harvest")
    public Result<HarvestResultVO> harvest(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) {
            return Result.fail("玩家不存在");
        }

        Long playerLandId = ((Number) body.get("playerLandId")).longValue();

        try {
            HarvestResultVO result = playerLandService.harvest(player.getId(), playerLandId);
            return Result.ok(result);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 简易 VO 转换 */
    private LandVO toLandVO(PlayerLand land) {
        return LandVO.builder()
                .playerLandId(land.getId())
                .status(land.getStatus())
                .cropId(land.getCropId())
                .cropLevel(land.getCropLevel())
                .yieldCount(land.getYieldCountSnapshot())
                .harvestExp(land.getHarvestExpSnapshot())
                .accessType(land.getAccessType())
                .plantTime(land.getPlantTime())
                .finishTime(land.getFinishTime())
                .waterLevel(land.getWaterLevel())
                .build();
    }
}
