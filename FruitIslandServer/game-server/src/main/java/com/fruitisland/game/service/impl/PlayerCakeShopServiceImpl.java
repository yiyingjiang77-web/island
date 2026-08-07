package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.dto.CraftResultVO;
import com.fruitisland.game.entity.CakeShopConfig;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.Inventory;
import com.fruitisland.game.entity.PlayerCakeShop;
import com.fruitisland.game.entity.PlayerRecipe;
import com.fruitisland.game.entity.RecipeConfig;
import com.fruitisland.game.entity.RecipeMaterial;
import com.fruitisland.game.mapper.PlayerCakeShopMapper;
import com.fruitisland.game.service.CakeShopConfigService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.InventoryService;
import com.fruitisland.game.service.PlayerCakeShopService;
import com.fruitisland.game.service.PlayerRecipeService;
import com.fruitisland.game.service.RecipeConfigService;
import com.fruitisland.game.service.RecipeMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlayerCakeShopServiceImpl
        extends BaseServiceImplX<PlayerCakeShopMapper, PlayerCakeShop>
        implements PlayerCakeShopService {

    private static final int UNLOCK_REQUIRED_LEVEL = 8;

    private final GamePlayerService gamePlayerService;
    private final CakeShopConfigService cakeShopConfigService;
    private final RecipeConfigService recipeConfigService;
    private final RecipeMaterialService recipeMaterialService;
    private final PlayerRecipeService playerRecipeService;
    private final InventoryService inventoryService;

    @Override
    public PlayerCakeShop getOrCreate(Long playerId) {
        PlayerCakeShop shop = lambdaQuery()
                .eq(PlayerCakeShop::getPlayerId, playerId)
                .one();
        if (shop != null) return shop;

        shop = new PlayerCakeShop();
        shop.setPlayerId(playerId);
        shop.setLevel(0);
        save(shop);
        return shop;
    }

    @Override
    @Transactional
    public PlayerCakeShop unlockCakeShop(Long playerId) {
        PlayerCakeShop shop = getOrCreate(playerId);
        if (shop.getLevel() != null && shop.getLevel() > 0) {
            throw new RuntimeException("蛋糕店已解锁");
        }

        GamePlayer player = gamePlayerService.getById(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");

        int islandLevel = player.getLevel() == null ? 1 : player.getLevel();
        if (islandLevel < UNLOCK_REQUIRED_LEVEL) {
            throw new RuntimeException("岛屿等级不足，需要 " + UNLOCK_REQUIRED_LEVEL + " 级");
        }

        CakeShopConfig config = cakeShopConfigService.getByLevel(1);
        if (config == null) throw new RuntimeException("蛋糕店一级配置不存在");

        if (player.getGold() < config.getUpgradeGold()) {
            throw new RuntimeException("金币不足，需要 " + config.getUpgradeGold());
        }

        player.setGold(player.getGold() - config.getUpgradeGold());
        gamePlayerService.updateById(player);

        shop.setLevel(1);
        updateById(shop);

        return shop;
    }

    @Override
    @Transactional
    public PlayerCakeShop upgradeCakeShop(Long playerId) {
        PlayerCakeShop shop = getOrCreate(playerId);
        if (shop.getLevel() == null || shop.getLevel() == 0) {
            throw new RuntimeException("请先解锁蛋糕店");
        }

        int nextLevel = shop.getLevel() + 1;
        CakeShopConfig nextConfig = cakeShopConfigService.getByLevel(nextLevel);
        if (nextConfig == null) {
            throw new RuntimeException("已是最高等级或下一等级未开放");
        }

        GamePlayer player = gamePlayerService.getById(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");

        int islandLevel = player.getLevel() == null ? 1 : player.getLevel();
        if (islandLevel < nextConfig.getRequiredIslandLevel()) {
            throw new RuntimeException("岛屿等级不足，需要 " + nextConfig.getRequiredIslandLevel() + " 级");
        }
        if (player.getGold() < nextConfig.getUpgradeGold()) {
            throw new RuntimeException("金币不足，需要 " + nextConfig.getUpgradeGold());
        }

        player.setGold(player.getGold() - nextConfig.getUpgradeGold());
        gamePlayerService.updateById(player);

        shop.setLevel(nextLevel);
        updateById(shop);

        return shop;
    }

    // ========== 蛋糕制作 ==========

    @Override
    @Transactional
    public CraftResultVO craft(Long playerId, String recipeId, int quantity) {
        if (quantity < 1 || quantity > 99) {
            throw new RuntimeException("制作数量必须为 1-99");
        }

        // 检查蛋糕店已解锁
        PlayerCakeShop shop = getOrCreate(playerId);
        if (shop.getLevel() == null || shop.getLevel() == 0) {
            throw new RuntimeException("请先解锁蛋糕店");
        }

        // 检查配方存在且为蛋糕类
        RecipeConfig recipe = recipeConfigService.getById(recipeId);
        if (recipe == null) {
            throw new RuntimeException("配方不存在");
        }
        if (!"cake_shop".equals(recipe.getCraftStation())) {
            throw new RuntimeException("该配方不在蛋糕店制作");
        }
        if (recipe.getEnabled() == null || recipe.getEnabled() != 1) {
            throw new RuntimeException("配方未启用");
        }

        // 检查配方资格
        PlayerRecipe qualification = playerRecipeService.findActive(playerId, recipeId);
        if (qualification == null) {
            throw new RuntimeException("尚未获得该配方");
        }

        // 检查材料
        List<RecipeMaterial> materials = recipeMaterialService.listByRecipe(recipeId);
        if (materials == null || materials.isEmpty()) {
            throw new RuntimeException("配方材料未配置");
        }

        for (RecipeMaterial material : materials) {
            long required = (long) material.getCount() * quantity;
            if (required > Integer.MAX_VALUE || inventoryCount(playerId, material.getItemId()) < required) {
                throw new RuntimeException("材料不足: " + material.getItemId());
            }
        }

        // 扣减材料
        List<CraftResultVO.MaterialResult> consumed = new ArrayList<>();
        for (RecipeMaterial material : materials) {
            int amount = material.getCount() * quantity;
            inventoryService.removeItem(playerId, material.getItemId(), amount);
            consumed.add(new CraftResultVO.MaterialResult(
                    material.getItemId(), amount, inventoryCount(playerId, material.getItemId())));
        }

        // 添加成品
        inventoryService.addItem(playerId, recipe.getOutputItem(), quantity);

        return new CraftResultVO(recipeId, recipe.getOutputItem(), quantity, consumed);
    }

    @Override
    public List<Map<String, Object>> listCraftableRecipes(Long playerId) {
        // 获取玩家所有蛋糕类配方资格
        List<PlayerRecipe> playerRecipes = playerRecipeService.listByPlayer(playerId);
        if (playerRecipes == null || playerRecipes.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有蛋糕类配方配置
        List<RecipeConfig> cakeRecipes = recipeConfigService.lambdaQuery()
                .eq(RecipeConfig::getCraftStation, "cake_shop")
                .eq(RecipeConfig::getEnabled, 1)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (RecipeConfig recipe : cakeRecipes) {
            boolean hasQualification = playerRecipes.stream()
                    .anyMatch(pr -> recipe.getId().equals(pr.getRecipeId()));
            if (!hasQualification) continue;

            List<RecipeMaterial> materials = recipeMaterialService.listByRecipe(recipe.getId());
            int maxCraftable = Integer.MAX_VALUE;
            List<Map<String, Object>> materialList = new ArrayList<>();

            for (RecipeMaterial mat : materials) {
                int have = inventoryCount(playerId, mat.getItemId());
                int canMake = mat.getCount() > 0 ? have / mat.getCount() : Integer.MAX_VALUE;
                maxCraftable = Math.min(maxCraftable, canMake);

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("itemId", mat.getItemId());
                m.put("required", mat.getCount());
                m.put("have", have);
                materialList.add(m);
            }

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("recipeId", recipe.getId());
            r.put("name", recipe.getName());
            r.put("outputItem", recipe.getOutputItem());
            r.put("saleGold", recipe.getSaleGold());
            r.put("saleExp", recipe.getSaleExp());
            r.put("maxCraftable", maxCraftable);
            r.put("materials", materialList);
            result.add(r);
        }

        return result;
    }

    private int inventoryCount(Long playerId, String itemId) {
        Inventory inv = inventoryService.findByPlayerAndItem(playerId, itemId);
        return inv == null ? 0 : (inv.getCount() == null ? 0 : inv.getCount());
    }
}
