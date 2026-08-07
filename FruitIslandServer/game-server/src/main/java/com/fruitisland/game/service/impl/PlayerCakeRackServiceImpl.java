package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.*;
import com.fruitisland.game.mapper.PlayerCakeRackMapper;
import com.fruitisland.game.service.*;
import com.fruitisland.game.util.MasteryBonusUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerCakeRackServiceImpl
        extends BaseServiceImplX<PlayerCakeRackMapper, PlayerCakeRack>
        implements PlayerCakeRackService {

    private final PlayerCakeShopService playerCakeShopService;
    private final CakeShopConfigService cakeShopConfigService;
    private final RecipeConfigService recipeConfigService;
    private final InventoryService inventoryService;
    private final GamePlayerService gamePlayerService;

    @Override
    public List<PlayerCakeRack> getOrCreateRacks(Long playerId) {
        List<PlayerCakeRack> existing = lambdaQuery()
                .eq(PlayerCakeRack::getPlayerId, playerId)
                .orderByAsc(PlayerCakeRack::getSlot)
                .list();

        if (existing.size() >= 2) return existing;

        // Create missing racks
        List<PlayerCakeRack> result = new ArrayList<>(existing);
        for (int slot = 1; slot <= 2; slot++) {
            int s = slot;
            boolean exists = result.stream().anyMatch(r -> r.getSlot() != null && r.getSlot() == s);
            if (!exists) {
                PlayerCakeRack rack = new PlayerCakeRack();
                rack.setPlayerId(playerId);
                rack.setSlot(slot);
                rack.setQuantity(0);
                rack.setSold(0);
                rack.setStatus("EMPTY");
                rack.setSaleGoldSnapshot(0);
                rack.setSaleExpSnapshot(0);
                rack.setSaleIntervalSnapshot(0);
                save(rack);
                result.add(rack);
            }
        }
        result.sort((a, b) -> a.getSlot().compareTo(b.getSlot()));
        return result;
    }

    @Override
    @Transactional
    public PlayerCakeRack listCake(Long playerId, int slot, String recipeId, int quantity) {
        if (slot < 1 || slot > 2) throw new RuntimeException("槽位必须是 1 或 2");
        if (quantity < 1) throw new RuntimeException("上架数量必须为正数");

        // 检查蛋糕店已解锁
        PlayerCakeShop shop = playerCakeShopService.getOrCreate(playerId);
        if (shop.getLevel() == null || shop.getLevel() == 0) {
            throw new RuntimeException("请先解锁蛋糕店");
        }

        // 获取蛋糕店配置（单架上限）
        CakeShopConfig config = cakeShopConfigService.getByLevel(shop.getLevel());
        if (config == null) throw new RuntimeException("蛋糕店配置不存在");
        if (quantity > config.getRackCapacity()) {
            throw new RuntimeException("上架数量超过单架上限 " + config.getRackCapacity());
        }

        // 检查配方存在且为蛋糕类
        RecipeConfig recipe = recipeConfigService.getById(recipeId);
        if (recipe == null) throw new RuntimeException("配方不存在");
        if (!"cake_shop".equals(recipe.getCraftStation())) {
            throw new RuntimeException("该配方不是蛋糕类");
        }

        // 检查架子状态
        List<PlayerCakeRack> racks = getOrCreateRacks(playerId);
        PlayerCakeRack rack = racks.stream()
                .filter(r -> r.getSlot() == slot)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("架子不存在"));

        if (!"EMPTY".equals(rack.getStatus())) {
            throw new RuntimeException("该架子正在使用中，请先下架或收取");
        }

        // 检查背包库存
        Inventory inv = inventoryService.findByPlayerAndItem(playerId, recipe.getOutputItem());
        int have = inv == null ? 0 : (inv.getCount() == null ? 0 : inv.getCount());
        if (have < quantity) {
            throw new RuntimeException("背包库存不足");
        }

        // 扣减背包蛋糕
        inventoryService.removeItem(playerId, recipe.getOutputItem(), quantity);

        // 锁定快照并设置批次
        rack.setRecipeId(recipeId);
        rack.setCakeItem(recipe.getOutputItem());
        rack.setQuantity(quantity);
        rack.setSold(0);
        rack.setStatus("SELLING");
        // ── 精通加成：售价提升（上架时快照锁定） ──
        int playerLevel = gamePlayerService.getById(playerId).getLevel();
        int adjustedGold = MasteryBonusUtil.applyPriceBonus(recipe.getSaleGold(), playerLevel);
        rack.setSaleGoldSnapshot(adjustedGold);
        rack.setSaleExpSnapshot(recipe.getSaleExp() == null ? 0 : recipe.getSaleExp() / 2);
        rack.setSaleIntervalSnapshot(config.getSaleIntervalSeconds());
        rack.setListTime(LocalDateTime.now());
        rack.setLastSettleTime(LocalDateTime.now());
        rack.setCloseTime(null);
        updateById(rack);

        return rack;
    }

    @Override
    @Transactional
    public PlayerCakeRack settleRack(Long playerId, int slot) {
        List<PlayerCakeRack> racks = getOrCreateRacks(playerId);
        PlayerCakeRack rack = racks.stream()
                .filter(r -> r.getSlot() == slot)
                .findFirst()
                .orElse(null);
        if (rack == null) return null;

        if (!"SELLING".equals(rack.getStatus())) {
            return rack; // EMPTY or SOLD_OUT, no settlement needed
        }

        if (rack.getListTime() == null || rack.getLastSettleTime() == null) {
            return rack;
        }
        if (rack.getSaleIntervalSnapshot() == null || rack.getSaleIntervalSnapshot() <= 0) {
            return rack;
        }

        LocalDateTime now = LocalDateTime.now();
        long elapsedSinceList = ChronoUnit.SECONDS.between(rack.getListTime(), now);
        long totalSoldByTime = elapsedSinceList / rack.getSaleIntervalSnapshot();
        int newSold = (int) Math.min(totalSoldByTime, rack.getQuantity());

        if (newSold > rack.getSold()) {
            rack.setSold(newSold);
        }

        // 推进结算时间
        long settledSeconds = (long) newSold * rack.getSaleIntervalSnapshot();
        rack.setLastSettleTime(rack.getListTime().plusSeconds(settledSeconds));

        // 全部售出 → SOLD_OUT
        if (rack.getSold() >= rack.getQuantity()) {
            rack.setStatus("SOLD_OUT");
        }

        updateById(rack);
        return rack;
    }

    @Override
    @Transactional
    public void settleAllRacks(Long playerId) {
        settleRack(playerId, 1);
        settleRack(playerId, 2);
    }

    @Override
    @Transactional
    public PlayerCakeRack takeDown(Long playerId, int slot) {
        // 先结算
        PlayerCakeRack rack = settleRack(playerId, slot);
        if (rack == null) throw new RuntimeException("架子不存在");
        if ("EMPTY".equals(rack.getStatus())) {
            throw new RuntimeException("架子为空，无需下架");
        }

        int unsold = rack.getQuantity() - rack.getSold();
        if (unsold > 0) {
            // 退回未售蛋糕到背包
            inventoryService.addItem(playerId, rack.getCakeItem(), unsold);
        }

        // 结算已售部分的收益
        if (rack.getSold() > 0) {
            long goldReward = (long) rack.getSaleGoldSnapshot() * rack.getSold();
            long expReward = (long) rack.getSaleExpSnapshot() * rack.getSold();
            settleRewards(playerId, goldReward, expReward);
        }

        // 关闭批次
        rack.setStatus("EMPTY");
        rack.setCloseTime(LocalDateTime.now());
        clearRackFields(rack);
        updateById(rack);

        return rack;
    }

    @Override
    @Transactional
    public PlayerCakeRack collect(Long playerId, int slot) {
        // 先结算
        PlayerCakeRack rack = settleRack(playerId, slot);
        if (rack == null) throw new RuntimeException("架子不存在");
        if (!"SOLD_OUT".equals(rack.getStatus())) {
            throw new RuntimeException("架子尚未售罄，无法收取");
        }

        // 结算全部收益
        long goldReward = (long) rack.getSaleGoldSnapshot() * rack.getQuantity();
        long expReward = (long) rack.getSaleExpSnapshot() * rack.getQuantity();
        settleRewards(playerId, goldReward, expReward);

        // 关闭批次
        rack.setStatus("EMPTY");
        rack.setCloseTime(LocalDateTime.now());
        clearRackFields(rack);
        updateById(rack);

        return rack;
    }

    private void settleRewards(Long playerId, long goldReward, long expReward) {
        if (goldReward <= 0 && expReward <= 0) return;
        // settleDrinkSaleReward handles gold, exp, and level-up in one transaction
        gamePlayerService.settleDrinkSaleReward(
                playerId,
                (int) Math.min(goldReward, Integer.MAX_VALUE),
                (int) Math.min(expReward, Integer.MAX_VALUE));
    }

    private void clearRackFields(PlayerCakeRack rack) {
        rack.setRecipeId(null);
        rack.setCakeItem(null);
        rack.setQuantity(0);
        rack.setSold(0);
        rack.setSaleGoldSnapshot(0);
        rack.setSaleExpSnapshot(0);
        rack.setSaleIntervalSnapshot(0);
        rack.setListTime(null);
        rack.setLastSettleTime(null);
    }
}
