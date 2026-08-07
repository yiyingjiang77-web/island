package com.fruitisland.game.service.impl;

import com.fruitisland.game.dto.CraftResultVO;
import com.fruitisland.game.dto.CraftingStationVO;
import com.fruitisland.game.dto.DrinkShopProgressVO;
import com.fruitisland.game.dto.DrinkShopRenovationVO;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.Inventory;
import com.fruitisland.game.entity.RecipeConfig;
import com.fruitisland.game.entity.RecipeMaterial;
import com.fruitisland.game.entity.DrinkShopLevelConfig;
import com.fruitisland.game.entity.PlayerDrinkShop;
import com.fruitisland.game.mapper.DrinkShopLevelConfigMapper;
import com.fruitisland.game.mapper.DrinkBarMapper;
import com.fruitisland.game.mapper.GamePlayerMapper;
import com.fruitisland.game.mapper.PlayerDrinkShopMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitisland.game.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DrinkShopServiceImpl implements DrinkShopService {
    private final InventoryService inventoryService;
    private final PlayerRecipeService playerRecipeService;
    private final RecipeConfigService recipeConfigService;
    private final RecipeMaterialService recipeMaterialService;
    private final PlayerDrinkShopMapper playerDrinkShopMapper;
    private final DrinkShopLevelConfigMapper levelConfigMapper;
    private final GamePlayerMapper gamePlayerMapper;
    private final DrinkBarMapper drinkBarMapper;

    @Override
    public DrinkShopProgressVO getProgress(GamePlayer player) {
        requirePlayer(player);
        playerDrinkShopMapper.ensureInitial(player.getId());
        for (int slot = 1; slot <= 6; slot++) drinkBarMapper.ensureSlot(player.getId(), slot);
        PlayerDrinkShop shop = playerDrinkShopMapper.selectOne(new LambdaQueryWrapper<PlayerDrinkShop>()
                .eq(PlayerDrinkShop::getPlayerId, player.getId()));
        DrinkShopLevelConfig current = activeConfig(shop.getShopLevel());
        DrinkShopLevelConfig next = shop.getShopLevel() >= 10 ? null : activeConfig(shop.getShopLevel() + 1);
        long gold = player.getGold() == null ? 0 : player.getGold();
        return new DrinkShopProgressVO(shop.getShopLevel(), gold, current, next,
                next == null ? 0 : Math.max(0, next.getRenovationGold() - gold),
                next == null || player.getLevel() >= next.getRequiredIslandLevel(), next == null);
    }

    @Override
    @Transactional
    public DrinkShopRenovationVO renovate(GamePlayer requestPlayer, int targetLevel) {
        requirePlayer(requestPlayer);
        playerDrinkShopMapper.ensureInitial(requestPlayer.getId());
        PlayerDrinkShop shop = playerDrinkShopMapper.selectForUpdate(requestPlayer.getId());
        GamePlayer player = gamePlayerMapper.selectForUpdate(requestPlayer.getId());
        if (shop == null || player == null) throw new IllegalArgumentException("玩家不存在");
        int previous = shop.getShopLevel();
        if (targetLevel != previous + 1) throw new IllegalArgumentException("饮品店只能逐级装修");
        DrinkShopLevelConfig target = activeConfig(targetLevel);
        if (player.getLevel() < target.getRequiredIslandLevel()) {
            throw new IllegalArgumentException("小岛等级不足，需要达到 " + target.getRequiredIslandLevel() + " 级");
        }
        long gold = player.getGold() == null ? 0 : player.getGold();
        if (gold < target.getRenovationGold()) {
            throw new IllegalArgumentException("金币不足，需要 " + target.getRenovationGold()
                    + "，当前 " + gold + "，还缺 " + (target.getRenovationGold() - gold));
        }
        player.setGold(gold - target.getRenovationGold());
        shop.setShopLevel(targetLevel);
        gamePlayerMapper.updateById(player);
        playerDrinkShopMapper.updateById(shop);
        return new DrinkShopRenovationVO(previous, targetLevel, target.getRenovationGold(), player.getGold());
    }

    private DrinkShopLevelConfig activeConfig(int level) {
        DrinkShopLevelConfig config = levelConfigMapper.selectOne(new LambdaQueryWrapper<DrinkShopLevelConfig>()
                .eq(DrinkShopLevelConfig::getLevel, level)
                .eq(DrinkShopLevelConfig::getEnabled, 1)
                .le(DrinkShopLevelConfig::getEffectiveFrom, java.time.LocalDateTime.now())
                .orderByDesc(DrinkShopLevelConfig::getConfigVersion).last("LIMIT 1"));
        if (config == null) throw new IllegalArgumentException("饮品店等级配置暂未开放");
        return config;
    }

    @Override
    public DrinkShopLevelConfig getActiveConfig(Long playerId) {
        if (playerId == null) throw new IllegalArgumentException("玩家不存在");
        playerDrinkShopMapper.ensureInitial(playerId);
        PlayerDrinkShop shop = playerDrinkShopMapper.selectOne(new LambdaQueryWrapper<PlayerDrinkShop>()
                .eq(PlayerDrinkShop::getPlayerId, playerId));
        if (shop == null) throw new IllegalArgumentException("饮品店状态不存在");
        return activeConfig(shop.getShopLevel());
    }

    private void requirePlayer(GamePlayer player) {
        if (player == null || player.getId() == null) throw new IllegalArgumentException("玩家不存在");
    }

    @Override
    public CraftingStationVO getCraftingStation(Long playerId) {
        List<CraftingStationVO.RecipeView> recipes = playerRecipeService.listByPlayer(playerId).stream()
                .map(qualification -> recipeView(playerId, qualification.getRecipeId()))
                .toList();
        return new CraftingStationVO(recipes);
    }

    private CraftingStationVO.RecipeView recipeView(Long playerId, String recipeId) {
        RecipeConfig recipe = requireRecipe(recipeId);
        List<CraftingStationVO.MaterialView> materials = recipeMaterialService.listByRecipe(recipeId).stream()
                .map(material -> new CraftingStationVO.MaterialView(
                        material.getItemId(), material.getCount(), inventoryCount(playerId, material.getItemId())))
                .toList();
        int max = materials.stream()
                .mapToInt(material -> material.getInventoryCount() / material.getRequiredCount())
                .min().orElse(0);
        return new CraftingStationVO.RecipeView(
                recipe.getId(), recipe.getName(), recipe.getOutputItem(), 1, Math.min(99, max), materials);
    }

    @Override
    @Transactional
    public CraftResultVO craft(GamePlayer player, String recipeId, int quantity) {
        requirePlayer(player);
        if (quantity < 1 || quantity > 99) throw new IllegalArgumentException("制作数量必须为 1–99");
        if (playerRecipeService.findActive(player.getId(), recipeId) == null) {
            throw new IllegalArgumentException("尚未获得该配方");
        }
        RecipeConfig recipe = requireRecipe(recipeId);
        List<RecipeMaterial> materials = recipeMaterialService.listByRecipe(recipeId);
        if (materials.isEmpty()) throw new IllegalArgumentException("配方材料未配置");

        for (RecipeMaterial material : materials) {
            long required = (long) material.getCount() * quantity;
            if (required > Integer.MAX_VALUE || inventoryCount(player.getId(), material.getItemId()) < required) {
                throw new IllegalArgumentException("材料不足");
            }
        }

        List<CraftResultVO.MaterialResult> consumed = new ArrayList<>();
        for (RecipeMaterial material : materials) {
            int amount = material.getCount() * quantity;
            inventoryService.removeItem(player.getId(), material.getItemId(), amount);
            consumed.add(new CraftResultVO.MaterialResult(
                    material.getItemId(), amount, inventoryCount(player.getId(), material.getItemId())));
        }
        inventoryService.addItem(player.getId(), recipe.getOutputItem(), quantity);
        return new CraftResultVO(recipeId, recipe.getOutputItem(), quantity, consumed);
    }

    private RecipeConfig requireRecipe(String recipeId) {
        RecipeConfig recipe = recipeConfigService.getById(recipeId);
        if (recipe == null) throw new IllegalArgumentException("配方不存在");
        return recipe;
    }

    private int inventoryCount(Long playerId, String itemId) {
        Inventory inventory = inventoryService.findByPlayerAndItem(playerId, itemId);
        return inventory == null || inventory.getCount() == null ? 0 : inventory.getCount();
    }
}
