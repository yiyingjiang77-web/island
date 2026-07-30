package com.fruitisland.game.service.impl;

import com.fruitisland.game.dto.CraftResultVO;
import com.fruitisland.game.dto.CraftingStationVO;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.Inventory;
import com.fruitisland.game.entity.RecipeConfig;
import com.fruitisland.game.entity.RecipeMaterial;
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
        if (player == null || player.getId() == null) throw new IllegalArgumentException("玩家不存在");
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
