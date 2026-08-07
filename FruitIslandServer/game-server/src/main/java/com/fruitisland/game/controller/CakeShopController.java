package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.dto.CakeShopStatusVO;
import com.fruitisland.game.dto.CraftResultVO;
import com.fruitisland.game.entity.CakeShopConfig;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerCakeRack;
import com.fruitisland.game.entity.PlayerCakeShop;
import com.fruitisland.game.service.CakeShopConfigService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerCakeRackService;
import com.fruitisland.game.service.PlayerCakeShopService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 蛋糕店接口。
 *
 * GET  /cake-shop/status           — 查看蛋糕店状态
 * GET  /cake-shop/recipes          — 查看可制作的蛋糕配方
 * GET  /cake-shop/racks            — 查看两个蛋糕架状态（触发惰性结算）
 * POST /cake-shop/unlock           — 首次解锁蛋糕店
 * POST /cake-shop/upgrade          — 升级蛋糕店
 * POST /cake-shop/craft            — 制作蛋糕
 * POST /cake-shop/racks/{slot}/list   — 上架蛋糕
 * POST /cake-shop/racks/{slot}/takedown — 下架（退回未售，结算已售）
 * POST /cake-shop/racks/{slot}/collect  — 收取售罄收益
 */
@RestController
@RequestMapping("/cake-shop")
@RequiredArgsConstructor
public class CakeShopController {

    private final GamePlayerService gamePlayerService;
    private final PlayerCakeShopService playerCakeShopService;
    private final PlayerCakeRackService playerCakeRackService;
    private final CakeShopConfigService cakeShopConfigService;

    @GetMapping("/status")
    public Result<CakeShopStatusVO> status(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        PlayerCakeShop shop = playerCakeShopService.getOrCreate(player.getId());

        CakeShopStatusVO vo = new CakeShopStatusVO();
        vo.setPlayerGold(player.getGold());
        vo.setIslandLevel(player.getLevel() == null ? 1 : player.getLevel());
        vo.setShop(buildShopStatus(shop, player));

        return Result.ok(vo);
    }

    @PostMapping("/unlock")
    public Result<PlayerCakeShop> unlock(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            return Result.ok(playerCakeShopService.unlockCakeShop(player.getId()));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/upgrade")
    public Result<PlayerCakeShop> upgrade(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            return Result.ok(playerCakeShopService.upgradeCakeShop(player.getId()));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/recipes")
    public Result<List<Map<String, Object>>> recipes(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        return Result.ok(playerCakeShopService.listCraftableRecipes(player.getId()));
    }

    @PostMapping("/craft")
    public Result<CraftResultVO> craft(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            Object recipeValue = body.get("recipeId");
            Object quantityValue = body.get("quantity");
            if (!(recipeValue instanceof String recipeId) || !(quantityValue instanceof Number quantity)) {
                return Result.fail("配方和制作数量不能为空");
            }
            return Result.ok(playerCakeShopService.craft(player.getId(), recipeId, quantity.intValue()));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ========== 蛋糕架 ==========

    @GetMapping("/racks")
    public Result<List<Map<String, Object>>> racks(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        // 惰性结算
        playerCakeRackService.settleAllRacks(player.getId());

        List<PlayerCakeRack> rackList = playerCakeRackService.getOrCreateRacks(player.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (PlayerCakeRack rack : rackList) {
            result.add(buildRackStatus(rack));
        }
        return Result.ok(result);
    }

    @PostMapping("/racks/{slot}/list")
    public Result<Map<String, Object>> listCake(
            @PathVariable int slot,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            Object recipeValue = body.get("recipeId");
            Object quantityValue = body.get("quantity");
            if (!(recipeValue instanceof String recipeId) || !(quantityValue instanceof Number quantity)) {
                return Result.fail("配方和上架数量不能为空");
            }
            PlayerCakeRack rack = playerCakeRackService.listCake(player.getId(), slot, recipeId, quantity.intValue());
            return Result.ok(buildRackStatus(rack));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/racks/{slot}/takedown")
    public Result<Map<String, Object>> takeDown(
            @PathVariable int slot, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            PlayerCakeRack rack = playerCakeRackService.takeDown(player.getId(), slot);
            return Result.ok(buildRackStatus(rack));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/racks/{slot}/collect")
    public Result<Map<String, Object>> collect(
            @PathVariable int slot, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            PlayerCakeRack rack = playerCakeRackService.collect(player.getId(), slot);
            return Result.ok(buildRackStatus(rack));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    private Map<String, Object> buildRackStatus(PlayerCakeRack rack) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("slot", rack.getSlot());
        m.put("status", rack.getStatus());
        m.put("recipeId", rack.getRecipeId());
        m.put("cakeItem", rack.getCakeItem());
        m.put("quantity", rack.getQuantity());
        m.put("sold", rack.getSold());
        m.put("remaining", rack.getQuantity() - rack.getSold());

        if ("SELLING".equals(rack.getStatus()) && rack.getListTime() != null) {
            long elapsed = java.time.temporal.ChronoUnit.SECONDS.between(
                    rack.getListTime(), java.time.LocalDateTime.now());
            long interval = rack.getSaleIntervalSnapshot() == null ? 0 : rack.getSaleIntervalSnapshot();
            long nextSoldIn = 0;
            if (interval > 0) {
                long elapsedInCurrent = elapsed % interval;
                nextSoldIn = interval - elapsedInCurrent;
                // If already sold all, no next sale
                if (rack.getSold() >= rack.getQuantity()) nextSoldIn = 0;
            }
            m.put("nextSoldIn", nextSoldIn);
            m.put("saleGoldPerItem", rack.getSaleGoldSnapshot());
            m.put("saleExpPerItem", rack.getSaleExpSnapshot());
            m.put("saleIntervalSeconds", rack.getSaleIntervalSnapshot());
            m.put("totalGoldReward", (long) rack.getSaleGoldSnapshot() * rack.getQuantity());
            m.put("totalExpReward", (long) rack.getSaleExpSnapshot() * rack.getQuantity());
            m.put("earnedGold", (long) rack.getSaleGoldSnapshot() * rack.getSold());
            m.put("earnedExp", (long) rack.getSaleExpSnapshot() * rack.getSold());
        }

        if ("SOLD_OUT".equals(rack.getStatus())) {
            m.put("totalGoldReward", (long) rack.getSaleGoldSnapshot() * rack.getQuantity());
            m.put("totalExpReward", (long) rack.getSaleExpSnapshot() * rack.getQuantity());
        }

        return m;
    }

    private CakeShopStatusVO.ShopStatus buildShopStatus(PlayerCakeShop shop, GamePlayer player) {
        CakeShopStatusVO.ShopStatus status = new CakeShopStatusVO.ShopStatus();
        int level = shop.getLevel() == null ? 0 : shop.getLevel();
        status.setUnlocked(level > 0);
        status.setLevel(level);

        CakeShopConfig currentConfig = level > 0 ? cakeShopConfigService.getByLevel(level) : null;
        if (currentConfig != null) {
            status.setRackCapacity(currentConfig.getRackCapacity());
            status.setSaleIntervalSeconds(currentConfig.getSaleIntervalSeconds());

            Map<String, Object> cur = new LinkedHashMap<>();
            cur.put("level", currentConfig.getLevel());
            cur.put("rackCapacity", currentConfig.getRackCapacity());
            cur.put("saleIntervalSeconds", currentConfig.getSaleIntervalSeconds());
            status.setCurrentConfig(cur);
        }

        // Next level preview
        int nextLevel = level + 1;
        CakeShopConfig nextConfig = cakeShopConfigService.getByLevel(nextLevel);
        if (nextConfig != null) {
            Map<String, Object> next = new LinkedHashMap<>();
            next.put("level", nextConfig.getLevel());
            next.put("requiredIslandLevel", nextConfig.getRequiredIslandLevel());
            next.put("upgradeGold", nextConfig.getUpgradeGold());
            next.put("rackCapacity", nextConfig.getRackCapacity());
            next.put("saleIntervalSeconds", nextConfig.getSaleIntervalSeconds());
            status.setNextLevel(next);
        }

        // Unlock hint
        if (level == 0) {
            int islandLevel = player.getLevel() == null ? 1 : player.getLevel();
            CakeShopConfig lv1 = cakeShopConfigService.getByLevel(1);
            int needGold = lv1 != null ? lv1.getUpgradeGold() : 5000;
            status.setCanUnlock(islandLevel >= 8);
            status.setUnlockHint("需要岛屿 8 级 + " + needGold + " 金币");
        }

        // All levels for UI
        List<Map<String, Object>> allLevels = new ArrayList<>();
        for (CakeShopConfig c : cakeShopConfigService.listEnabled()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", c.getLevel());
            m.put("requiredIslandLevel", c.getRequiredIslandLevel());
            m.put("upgradeGold", c.getUpgradeGold());
            m.put("rackCapacity", c.getRackCapacity());
            m.put("saleIntervalSeconds", c.getSaleIntervalSeconds());
            allLevels.add(m);
        }
        status.setAllLevels(allLevels);

        return status;
    }
}
