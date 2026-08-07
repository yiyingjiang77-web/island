package com.fruitisland.game.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fruitisland.game.dto.DrinkBarListingResultVO;
import com.fruitisland.game.dto.DrinkBarCollectionResultVO;
import com.fruitisland.game.dto.DrinkBarStateVO;
import com.fruitisland.game.dto.DrinkBarTakeDownResultVO;
import com.fruitisland.game.entity.DrinkBar;
import com.fruitisland.game.entity.DrinkBarBatch;
import com.fruitisland.game.entity.DrinkBarBatchStatus;
import com.fruitisland.game.entity.Inventory;
import com.fruitisland.game.entity.DrinkShopLevelConfig;
import com.fruitisland.game.entity.RecipeConfig;
import com.fruitisland.game.mapper.DrinkBarBatchMapper;
import com.fruitisland.game.mapper.DrinkBarMapper;
import com.fruitisland.game.service.DrinkBarService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.InventoryService;
import com.fruitisland.game.service.RecipeConfigService;
import com.fruitisland.game.service.DrinkShopService;
import com.fruitisland.game.util.MasteryBonusUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrinkBarServiceImpl implements DrinkBarService {

    private static final int BAR_COUNT = 6;

    private final DrinkBarMapper drinkBarMapper;
    private final DrinkBarBatchMapper drinkBarBatchMapper;
    private final RecipeConfigService recipeConfigService;
    private final InventoryService inventoryService;
    private final Clock clock;
    private final GamePlayerService gamePlayerService;
    private final DrinkShopService drinkShopService;

    @Override
    @Transactional
    public DrinkBarStateVO getBars(Long playerId) {
        DrinkShopLevelConfig config = drinkShopService.getActiveConfig(playerId);
        for (int slotNumber = 1; slotNumber <= BAR_COUNT; slotNumber++) {
            drinkBarMapper.ensureSlot(playerId, slotNumber);
        }

        var bars = drinkBarMapper.selectList(Wrappers.<DrinkBar>lambdaQuery()
                .eq(DrinkBar::getPlayerId, playerId)
                .orderByAsc(DrinkBar::getSlotNumber));
        Map<Long, DrinkBarBatch> batchesByBar = drinkBarBatchMapper.selectList(
                        Wrappers.<DrinkBarBatch>lambdaQuery()
                                .eq(DrinkBarBatch::getPlayerId, playerId)
                                .eq(DrinkBarBatch::getActiveMarker, 1))
                .stream()
                .map(this::advanceSales)
                .collect(Collectors.toMap(DrinkBarBatch::getBarId, Function.identity()));

        var barViews = bars.stream()
                .map(bar -> toView(bar, batchesByBar.get(bar.getId())))
                .toList();
        var drinkViews = recipeConfigService.lambdaQuery()
                .eq(RecipeConfig::getEnabled, 1)
                .orderByAsc(RecipeConfig::getUnlockLevel)
                .orderByAsc(RecipeConfig::getId)
                .list()
                .stream()
                .map(recipe -> toDrinkView(playerId, recipe, config))
                .toList();
        return new DrinkBarStateVO(barViews, drinkViews, config.getLevel(),
                config.getBarCapacity(), config.getSaleIntervalSeconds());
    }

    @Override
    @Transactional
    public DrinkBarListingResultVO listDrink(Long playerId, Long barId, String recipeId) {
        DrinkShopLevelConfig config = drinkShopService.getActiveConfig(playerId);
        Inventory inventory = inventoryService.findByPlayerAndItem(playerId,
                requireRecipe(recipeId).getOutputItem());
        int inventoryCount = inventory == null || inventory.getCount() == null ? 0 : inventory.getCount();
        return listDrink(playerId, barId, recipeId, Math.min(config.getBarCapacity(), inventoryCount));
    }

    @Override
    @Transactional
    public DrinkBarListingResultVO listDrink(Long playerId, Long barId, String recipeId, int quantity) {
        DrinkShopLevelConfig config = drinkShopService.getActiveConfig(playerId);
        DrinkBar bar = drinkBarMapper.lockOwnedBar(playerId, barId);
        if (bar == null || bar.getOpened() == null || bar.getOpened() != 1) {
            throw new IllegalArgumentException("吧台不存在或尚未开放");
        }
        DrinkBarBatch activeBatch = drinkBarBatchMapper.selectOne(
                Wrappers.<DrinkBarBatch>lambdaQuery()
                        .eq(DrinkBarBatch::getBarId, barId)
                        .eq(DrinkBarBatch::getActiveMarker, 1));
        if (activeBatch != null) {
            throw new IllegalArgumentException("吧台当前不是空闲状态");
        }

        RecipeConfig recipe = requireRecipe(recipeId);
        if (recipe.getSaleGold() == null || recipe.getSaleExp() == null
                || config.getSaleIntervalSeconds() == null
                || config.getSaleIntervalSeconds() <= 0) {
            throw new IllegalArgumentException("吧台销售配置不完整");
        }

        Inventory inventory = inventoryService.findByPlayerAndItem(
                playerId, recipe.getOutputItem());
        int inventoryCount = inventory == null || inventory.getCount() == null
                ? 0 : inventory.getCount();
        if (inventoryCount <= 0) {
            throw new IllegalArgumentException("成品库存不足");
        }
        if (quantity < 1 || quantity > config.getBarCapacity()) {
            throw new IllegalArgumentException("上架数量必须为 1–" + config.getBarCapacity());
        }
        if (quantity > inventoryCount) throw new IllegalArgumentException("成品库存不足");
        int listedQuantity = quantity;
        inventoryService.removeItem(playerId, recipe.getOutputItem(), listedQuantity);

        DrinkBarBatch batch = new DrinkBarBatch();
        batch.setPlayerId(playerId);
        batch.setBarId(barId);
        batch.setRecipeId(recipe.getId());
        batch.setItemId(recipe.getOutputItem());
        batch.setListedQuantity(listedQuantity);
        batch.setSoldQuantity(0);
        batch.setStatus(DrinkBarBatchStatus.SELLING.name());
        batch.setActiveMarker(1);
        // ── 精通加成：售价提升（上架时快照锁定） ──
        int playerLevel = gamePlayerService.getById(playerId).getLevel();
        int adjustedGold = MasteryBonusUtil.applyPriceBonus(recipe.getSaleGold(), playerLevel);
        batch.setUnitGoldSnapshot(adjustedGold);
        batch.setUnitExpSnapshot(recipe.getSaleExp() == null ? 0 : recipe.getSaleExp() / 2);
        batch.setSaleIntervalSecondsSnapshot(config.getSaleIntervalSeconds());
        batch.setListedAt(LocalDateTime.now(clock));
        drinkBarBatchMapper.insert(batch);

        return new DrinkBarListingResultVO(
                toView(bar, batch),
                inventoryCount - listedQuantity,
                listedQuantity * adjustedGold,
                listedQuantity * (recipe.getSaleExp() == null ? 0 : recipe.getSaleExp() / 2));
    }

    private RecipeConfig requireRecipe(String recipeId) {
        RecipeConfig recipe = recipeConfigService.getById(recipeId);
        if (recipe == null || recipe.getEnabled() == null || recipe.getEnabled() != 1) {
            throw new IllegalArgumentException("配方不存在或不可用");
        }
        return recipe;
    }

    @Override
    @Transactional
    public DrinkBarTakeDownResultVO takeDown(Long playerId, Long barId) {
        DrinkBar bar = drinkBarMapper.lockOwnedBar(playerId, barId);
        if (bar == null || bar.getOpened() == null || bar.getOpened() != 1) {
            throw new IllegalArgumentException("吧台不存在或尚未开放");
        }
        DrinkBarBatch batch = drinkBarBatchMapper.lockActiveBatch(playerId, barId);
        if (batch == null) {
            throw new IllegalArgumentException("吧台当前没有可下架批次");
        }
        advanceSales(batch);
        if (activeStatus(batch) != DrinkBarBatchStatus.SELLING) {
            throw new IllegalArgumentException("已售罄批次不能下架，请收取收益");
        }

        int soldQuantity = batch.getSoldQuantity();
        int returnedQuantity = batch.getListedQuantity() - soldQuantity;
        int settledGold = soldQuantity * batch.getUnitGoldSnapshot();
        int settledExp = soldQuantity * batch.getUnitExpSnapshot();
        if (returnedQuantity > 0) {
            inventoryService.addItem(playerId, batch.getItemId(), returnedQuantity);
        }
        if (soldQuantity > 0) {
            gamePlayerService.settleDrinkSaleReward(playerId, settledGold, settledExp);
        }

        batch.setStatus(DrinkBarBatchStatus.CLOSED.name());
        batch.setActiveMarker(null);
        batch.setClosedAt(LocalDateTime.now(clock));
        if (drinkBarBatchMapper.closeActiveBatch(batch.getId(), batch.getClosedAt()) != 1) {
            throw new IllegalStateException("批次状态已变化，请刷新后重试");
        }

        var player = gamePlayerService.getById(playerId);
        return new DrinkBarTakeDownResultVO(
                toView(bar, null),
                returnedQuantity,
                settledGold,
                settledExp,
                player.getGold(),
                player.getCumulativeExp(),
                player.getLevel());
    }

    @Override
    @Transactional
    public DrinkBarCollectionResultVO collect(Long playerId, Long barId) {
        DrinkBar bar = drinkBarMapper.lockOwnedBar(playerId, barId);
        if (bar == null || bar.getOpened() == null || bar.getOpened() != 1) {
            throw new IllegalArgumentException("吧台不存在或尚未开放");
        }
        DrinkBarBatch batch = drinkBarBatchMapper.lockActiveBatch(playerId, barId);
        if (batch == null) {
            throw new IllegalArgumentException("吧台当前没有可收取批次");
        }
        advanceSales(batch);
        if (activeStatus(batch) != DrinkBarBatchStatus.SOLD_OUT) {
            throw new IllegalArgumentException("吧台批次尚未售罄");
        }
        return settleCollectedBatches(playerId, List.of(batch));
    }

    @Override
    @Transactional
    public DrinkBarCollectionResultVO collectAll(Long playerId) {
        var bars = drinkBarMapper.selectList(Wrappers.<DrinkBar>lambdaQuery()
                .eq(DrinkBar::getPlayerId, playerId)
                .eq(DrinkBar::getOpened, 1)
                .orderByAsc(DrinkBar::getSlotNumber));
        var soldOutBatches = bars.stream()
                .map(bar -> {
                    drinkBarMapper.lockOwnedBar(playerId, bar.getId());
                    DrinkBarBatch batch =
                            drinkBarBatchMapper.lockActiveBatch(playerId, bar.getId());
                    return batch == null ? null : advanceSales(batch);
                })
                .filter(batch -> batch != null
                        && activeStatus(batch) == DrinkBarBatchStatus.SOLD_OUT)
                .toList();
        if (soldOutBatches.isEmpty()) {
            throw new IllegalArgumentException("当前没有可收取的售罄吧台");
        }
        return settleCollectedBatches(playerId, soldOutBatches);
    }

    private DrinkBarCollectionResultVO settleCollectedBatches(
            Long playerId,
            List<DrinkBarBatch> batches
    ) {
        int settledGold = batches.stream()
                .mapToInt(batch -> batch.getSoldQuantity() * batch.getUnitGoldSnapshot())
                .sum();
        int settledExp = batches.stream()
                .mapToInt(batch -> batch.getSoldQuantity() * batch.getUnitExpSnapshot())
                .sum();
        gamePlayerService.settleDrinkSaleReward(playerId, settledGold, settledExp);

        LocalDateTime closedAt = LocalDateTime.now(clock);
        for (DrinkBarBatch batch : batches) {
            if (drinkBarBatchMapper.closeActiveBatch(batch.getId(), closedAt) != 1) {
                throw new IllegalStateException("批次状态已变化，请刷新后重试");
            }
        }
        var player = gamePlayerService.getById(playerId);
        List<Long> collectedBarIds = batches.stream()
                .map(DrinkBarBatch::getBarId)
                .toList();
        return new DrinkBarCollectionResultVO(
                collectedBarIds,
                collectedBarIds.size(),
                settledGold,
                settledExp,
                player.getGold(),
                player.getCumulativeExp(),
                player.getLevel());
    }

    private DrinkBarStateVO.BarView toView(DrinkBar bar, DrinkBarBatch batch) {
        if (batch == null) {
            return new DrinkBarStateVO.BarView(
                    bar.getId(), bar.getSlotNumber(), bar.getOpened() == 1,
                    "EMPTY", null);
        }
        DrinkBarBatchStatus status = activeStatus(batch);
        String state = status.name();
        int soldQuantity = batch.getSoldQuantity();
        int remainingQuantity = batch.getListedQuantity() - soldQuantity;
        Long nextSaleInSeconds = status == DrinkBarBatchStatus.SELLING
                ? secondsUntilNextSale(batch)
                : null;
        var batchView = new DrinkBarStateVO.BatchView(
                batch.getId(),
                batch.getRecipeId(),
                batch.getItemId(),
                batch.getListedQuantity(),
                soldQuantity,
                remainingQuantity,
                soldQuantity * batch.getUnitGoldSnapshot(),
                soldQuantity * batch.getUnitExpSnapshot(),
                nextSaleInSeconds,
                batch.getStatus(),
                batch.getUnitGoldSnapshot(),
                batch.getUnitExpSnapshot(),
                batch.getSaleIntervalSecondsSnapshot(),
                batch.getListedAt(),
                batch.getSoldOutAt());
        return new DrinkBarStateVO.BarView(
                bar.getId(), bar.getSlotNumber(), bar.getOpened() == 1, state, batchView);
    }

    private DrinkBarBatch advanceSales(DrinkBarBatch batch) {
        if (activeStatus(batch) != DrinkBarBatchStatus.SELLING) {
            return batch;
        }
        long elapsedSeconds = Math.max(0,
                Duration.between(batch.getListedAt(), LocalDateTime.now(clock)).getSeconds());
        int soldQuantity = Math.max(
                batch.getSoldQuantity(),
                (int) Math.min(
                        batch.getListedQuantity(),
                        elapsedSeconds / batch.getSaleIntervalSecondsSnapshot()));
        if (soldQuantity == batch.getSoldQuantity()) {
            return batch;
        }
        batch.setSoldQuantity(soldQuantity);
        if (soldQuantity == batch.getListedQuantity()) {
            batch.setStatus(DrinkBarBatchStatus.SOLD_OUT.name());
            batch.setSoldOutAt(batch.getListedAt().plusSeconds(
                    (long) batch.getListedQuantity()
                            * batch.getSaleIntervalSecondsSnapshot()));
        }
        drinkBarBatchMapper.updateSalesProgress(
                batch.getId(),
                batch.getSoldQuantity(),
                batch.getStatus(),
                batch.getSoldOutAt());
        return batch;
    }

    private DrinkBarBatchStatus activeStatus(DrinkBarBatch batch) {
        DrinkBarBatchStatus status = DrinkBarBatchStatus.fromValue(batch.getStatus());
        if (status == DrinkBarBatchStatus.CLOSED) {
            throw new IllegalStateException("吧台批次状态无效：" + batch.getStatus());
        }
        return status;
    }

    private long secondsUntilNextSale(DrinkBarBatch batch) {
        long elapsedSeconds = Math.max(0,
                Duration.between(batch.getListedAt(), LocalDateTime.now(clock)).getSeconds());
        long interval = batch.getSaleIntervalSecondsSnapshot();
        long remainder = elapsedSeconds % interval;
        return remainder == 0 ? interval : interval - remainder;
    }

    private DrinkBarStateVO.DrinkView toDrinkView(
            Long playerId, RecipeConfig recipe, DrinkShopLevelConfig config) {
        Inventory inventory = inventoryService.findByPlayerAndItem(
                playerId, recipe.getOutputItem());
        int inventoryCount = inventory == null || inventory.getCount() == null
                ? 0 : Math.max(0, inventory.getCount());
        int listingQuantity = Math.min(config.getBarCapacity(), inventoryCount);
        // 精通加成：预览售价也需反映玩家等级
        int pvLevel = gamePlayerService.getById(playerId).getLevel();
        int unitGold = MasteryBonusUtil.applyPriceBonus(
                recipe.getSaleGold() == null ? 0 : recipe.getSaleGold(), pvLevel);
        int unitExp = recipe.getSaleExp() == null ? 0 : recipe.getSaleExp() / 2;
        int interval = config.getSaleIntervalSeconds();
        return new DrinkBarStateVO.DrinkView(
                recipe.getId(),
                recipe.getName(),
                recipe.getOutputItem(),
                inventoryCount,
                listingQuantity,
                unitGold,
                unitExp,
                listingQuantity * unitGold,
                listingQuantity * unitExp,
                interval);
    }
}
