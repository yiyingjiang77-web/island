package com.fruitisland.game.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.game.dto.LivestockStatusVO;
import com.fruitisland.game.entity.BarnConfig;
import com.fruitisland.game.entity.CoopConfig;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerBarn;
import com.fruitisland.game.entity.PlayerCoop;
import com.fruitisland.game.service.BarnConfigService;
import com.fruitisland.game.service.CoopConfigService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerBarnService;
import com.fruitisland.game.service.PlayerCoopService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 畜牧接口。
 *
 * GET  /livestock/status        — 查看牛棚与鸡舍汇总状态（触发惰性结算）
 * POST /livestock/barn/unlock   — 首次解锁牛棚
 * POST /livestock/barn/upgrade  — 升级牛棚
 * POST /livestock/coop/unlock   — 首次解锁鸡舍
 * POST /livestock/coop/upgrade  — 升级鸡舍
 */
@RestController
@RequestMapping("/livestock")
@RequiredArgsConstructor
public class LivestockController {

    private final GamePlayerService gamePlayerService;
    private final PlayerBarnService playerBarnService;
    private final PlayerCoopService playerCoopService;
    private final BarnConfigService barnConfigService;
    private final CoopConfigService coopConfigService;

    @GetMapping("/status")
    public Result<LivestockStatusVO> status(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        // 惰性结算
        playerBarnService.settleMilkProduction(player.getId());
        playerCoopService.settleEggProduction(player.getId());

        PlayerBarn barn = playerBarnService.getOrCreate(player.getId());
        PlayerCoop coop = playerCoopService.getOrCreate(player.getId());

        LivestockStatusVO vo = new LivestockStatusVO();
        vo.setPlayerGold(player.getGold());
        vo.setIslandLevel(player.getLevel() == null ? 1 : player.getLevel());
        vo.setBarn(buildBarnStatus(barn, player));
        vo.setCoop(buildCoopStatus(coop, player));

        return Result.ok(vo);
    }

    @PostMapping("/barn/unlock")
    public Result<PlayerBarn> unlockBarn(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            return Result.ok(playerBarnService.unlockBarn(player.getId()));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/barn/upgrade")
    public Result<PlayerBarn> upgradeBarn(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            return Result.ok(playerBarnService.upgradeBarn(player.getId()));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/coop/unlock")
    public Result<PlayerCoop> unlockCoop(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            return Result.ok(playerCoopService.unlockCoop(player.getId()));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/coop/upgrade")
    public Result<PlayerCoop> upgradeCoop(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var player = gamePlayerService.findByUserId(userId);
        if (player == null) return Result.fail("玩家不存在");

        try {
            return Result.ok(playerCoopService.upgradeCoop(player.getId()));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    private LivestockStatusVO.BuildingStatus buildBarnStatus(PlayerBarn barn, GamePlayer player) {
        LivestockStatusVO.BuildingStatus status = new LivestockStatusVO.BuildingStatus();
        int level = barn.getLevel() == null ? 0 : barn.getLevel();
        status.setUnlocked(level > 0);
        status.setLevel(level);
        status.setAnimalCount(barn.getCowCount() == null ? 0 : barn.getCowCount());

        BarnConfig currentConfig = level > 0 ? barnConfigService.getByLevel(level) : null;
        status.setCapacity(currentConfig != null ? currentConfig.getAnimalCapacity() : 0);
        status.setCycleSeconds(barn.getCycleSeconds() == null ? 600 : barn.getCycleSeconds());

        if (level > 0 && barn.getCycleStartTime() != null) {
            long elapsed = ChronoUnit.SECONDS.between(barn.getCycleStartTime(), LocalDateTime.now());
            long cycleSecs = barn.getCycleSeconds() == null ? 600 : barn.getCycleSeconds();
            long remaining = Math.max(0, cycleSecs - (elapsed % cycleSecs));
            status.setRemainingSeconds(remaining);
            int milkPerCycle = (barn.getCowCountSnapshot() == null ? 0 : barn.getCowCountSnapshot())
                    * (barn.getMilkPerCowSnapshot() == null ? 0 : barn.getMilkPerCowSnapshot());
            status.setProductionPerCycle(milkPerCycle);
        }

        // Next level preview
        int nextLevel = level + 1;
        BarnConfig nextConfig = barnConfigService.getByLevel(nextLevel);
        if (nextConfig != null) {
            Map<String, Object> next = new LinkedHashMap<>();
            next.put("level", nextConfig.getLevel());
            next.put("requiredIslandLevel", nextConfig.getRequiredIslandLevel());
            next.put("upgradeGold", nextConfig.getUpgradeGold());
            next.put("animalCapacity", nextConfig.getAnimalCapacity());
            next.put("animalAdded", nextConfig.getAnimalAdded());
            next.put("milkPerCow", nextConfig.getMilkPerCow());
            status.setNextLevel(next);
        }

        // Unlock hint
        if (level == 0) {
            int islandLevel = player.getLevel() == null ? 1 : player.getLevel();
            status.setCanUnlock(islandLevel >= 5);
            status.setUnlockHint("需要岛屿 5 级 + 1000 金币，赠送 1 头奶牛 + 10 份牛奶");
        }

        // All levels for UI
        List<Map<String, Object>> allLevels = new ArrayList<>();
        for (BarnConfig c : barnConfigService.listEnabled()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", c.getLevel());
            m.put("requiredIslandLevel", c.getRequiredIslandLevel());
            m.put("upgradeGold", c.getUpgradeGold());
            m.put("animalCapacity", c.getAnimalCapacity());
            m.put("animalAdded", c.getAnimalAdded());
            m.put("milkPerCow", c.getMilkPerCow());
            allLevels.add(m);
        }
        status.setAllLevels(allLevels);

        // Current config
        if (currentConfig != null) {
            Map<String, Object> cur = new LinkedHashMap<>();
            cur.put("level", currentConfig.getLevel());
            cur.put("animalCapacity", currentConfig.getAnimalCapacity());
            cur.put("milkPerCow", currentConfig.getMilkPerCow());
            cur.put("produceCycleSeconds", currentConfig.getProduceCycleSeconds());
            status.setCurrentConfig(cur);
        }

        return status;
    }

    private LivestockStatusVO.BuildingStatus buildCoopStatus(PlayerCoop coop, GamePlayer player) {
        LivestockStatusVO.BuildingStatus status = new LivestockStatusVO.BuildingStatus();
        int level = coop.getLevel() == null ? 0 : coop.getLevel();
        status.setUnlocked(level > 0);
        status.setLevel(level);
        status.setAnimalCount(coop.getChickenCount() == null ? 0 : coop.getChickenCount());

        CoopConfig currentConfig = level > 0 ? coopConfigService.getByLevel(level) : null;
        status.setCapacity(currentConfig != null ? currentConfig.getAnimalCapacity() : 0);
        status.setCycleSeconds(coop.getCycleSeconds() == null ? 600 : coop.getCycleSeconds());

        if (level > 0 && coop.getCycleStartTime() != null) {
            long elapsed = ChronoUnit.SECONDS.between(coop.getCycleStartTime(), LocalDateTime.now());
            long cycleSecs = coop.getCycleSeconds() == null ? 600 : coop.getCycleSeconds();
            long remaining = Math.max(0, cycleSecs - (elapsed % cycleSecs));
            status.setRemainingSeconds(remaining);
            int eggsPerCycle = (coop.getChickenCountSnapshot() == null ? 0 : coop.getChickenCountSnapshot())
                    + (coop.getBonusEggSnapshot() == null ? 0 : coop.getBonusEggSnapshot());
            status.setProductionPerCycle(eggsPerCycle);
        }

        int nextLevel = level + 1;
        CoopConfig nextConfig = coopConfigService.getByLevel(nextLevel);
        if (nextConfig != null) {
            Map<String, Object> next = new LinkedHashMap<>();
            next.put("level", nextConfig.getLevel());
            next.put("requiredIslandLevel", nextConfig.getRequiredIslandLevel());
            next.put("upgradeGold", nextConfig.getUpgradeGold());
            next.put("animalCapacity", nextConfig.getAnimalCapacity());
            next.put("animalAdded", nextConfig.getAnimalAdded());
            next.put("bonusEggs", nextConfig.getBonusEggs());
            next.put("produceCycleSeconds", nextConfig.getProduceCycleSeconds());
            status.setNextLevel(next);
        }

        if (level == 0) {
            int islandLevel = player.getLevel() == null ? 1 : player.getLevel();
            status.setCanUnlock(islandLevel >= 8);
            status.setUnlockHint("需要岛屿 8 级 + 3000 金币，赠送 1 只鸡 + 5 个鸡蛋");
        }

        List<Map<String, Object>> allLevels = new ArrayList<>();
        for (CoopConfig c : coopConfigService.listEnabled()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", c.getLevel());
            m.put("requiredIslandLevel", c.getRequiredIslandLevel());
            m.put("upgradeGold", c.getUpgradeGold());
            m.put("animalCapacity", c.getAnimalCapacity());
            m.put("animalAdded", c.getAnimalAdded());
            m.put("bonusEggs", c.getBonusEggs());
            m.put("produceCycleSeconds", c.getProduceCycleSeconds());
            allLevels.add(m);
        }
        status.setAllLevels(allLevels);

        if (currentConfig != null) {
            Map<String, Object> cur = new LinkedHashMap<>();
            cur.put("level", currentConfig.getLevel());
            cur.put("animalCapacity", currentConfig.getAnimalCapacity());
            cur.put("bonusEggs", currentConfig.getBonusEggs());
            cur.put("produceCycleSeconds", currentConfig.getProduceCycleSeconds());
            status.setCurrentConfig(cur);
        }

        return status;
    }
}
