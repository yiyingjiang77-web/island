package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.dto.GameInitVO;
import com.fruitisland.game.dto.LandVO;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.Inventory;
import com.fruitisland.game.entity.Island;
import com.fruitisland.game.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final PlayerLandService playerLandService;
    private final InventoryService inventoryService;
    private final CropConfigService cropConfigService;
    private final CropLevelConfigService cropLevelConfigService;
    private final CropUnlockSourceService cropUnlockSourceService;
    private final PlayerCropService playerCropService;
    private final PlayerCropGrantService playerCropGrantService;
    private final PlayerLevelConfigService playerLevelConfigService;
    private final IslandGrowthService islandGrowthService;
    private final SatisfactionService satisfactionService;

    /**
     * 游戏初始化
     *
     * 客户端进入 MainScene 前调用，获取玩家所有游戏数据
     *
     * 请求头: Authorization: Bearer <token>
     * 响应: {"code": 0, "data": {"player": {...}, "island": {...}, "lands": [...], "inventory": [...]}}
     */
    @GetMapping("/init")
    public Result<GameInitVO> gameInit(HttpServletRequest request) {
        // 从 JWT 拦截器注入的 userId 获取
        Long userId = (Long) request.getAttribute("userId");
        log.info("游戏初始化请求: userId={}", userId);

        // 1. 查找或创建游戏角色
        boolean isNewPlayer = false;
        GamePlayer player = gamePlayerService.findByUserId(userId);
        if (player == null) {
            player = gamePlayerService.createPlayer(userId);
            isNewPlayer = true;
            log.info("新角色创建: playerId={}", player.getId());
        }

        // 2. 查找或创建岛屿
        Island island = islandService.findByPlayerId(player.getId());
        if (island == null) {
            island = islandService.createIsland(player.getId());
            log.info("新岛屿创建: islandId={}", island.getId());
        }

        // 3. 新玩家：赠送第一块地
        if (isNewPlayer) {
            initNewPlayerLands(player.getId());
            log.info("新玩家初始土地初始化完成: playerId={}", player.getId());
        }

        // 小岛等级配置是奖励发放与迁移的权威来源；重复初始化不会重复领取。
        var islandGrowth = islandGrowthService.initialize(player);
        var satisfactionRewards = satisfactionService.settlePastDays(player.getId());

        // 4. 加载土地数据
        List<LandVO> lands = playerLandService.listByPlayer(player.getId(), player.getLevel());

        // 5. 加载背包
        List<Inventory> inventory = inventoryService.lambdaQuery()
                .eq(Inventory::getPlayerId, player.getId())
                .list();

        // 6. 作物配置及玩家权限由数据库返回，客户端不写死成长数值。
        var cropConfigs = cropConfigService.list();
        var cropLevelConfigs = cropLevelConfigService.list();
        var cropUnlockSources = cropUnlockSourceService.lambdaQuery()
                .eq(com.fruitisland.game.entity.CropUnlockSource::getEnabled, 1)
                .orderByAsc(com.fruitisland.game.entity.CropUnlockSource::getRequiredPlayerLevel)
                .list();
        var playerCrops = playerCropService.listByPlayer(player.getId());
        var cropGrants = playerCropGrantService.listActiveByPlayer(
                player.getId(), java.time.LocalDateTime.now());
        var playerLevelConfigs = playerLevelConfigService.lambdaQuery()
                .orderByAsc(com.fruitisland.game.entity.PlayerLevelConfig::getLevel)
                .list();

        GameInitVO vo = GameInitVO.of(
                player,
                island,
                lands,
                inventory,
                cropConfigs,
                cropLevelConfigs,
                cropUnlockSources,
                playerCrops,
                cropGrants,
                playerLevelConfigs,
                islandGrowth,
                satisfactionRewards
        );
        return Result.ok(vo);
    }

    /**
     * 新玩家初始化：赠送第一块农田。
     */
    private void initNewPlayerLands(Long playerId) {
        // 第一块免费地 (land_config_id=1)
        playerLandService.buy(playerId, 1L, 1);
    }
}
