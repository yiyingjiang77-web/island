package com.fruitisland.game.service.impl;

import com.fruitisland.game.dto.CraftResultVO;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.Inventory;
import com.fruitisland.game.entity.PlayerRecipe;
import com.fruitisland.game.entity.RecipeConfig;
import com.fruitisland.game.entity.RecipeMaterial;
import com.fruitisland.game.service.InventoryService;
import com.fruitisland.game.service.PlayerRecipeService;
import com.fruitisland.game.service.RecipeConfigService;
import com.fruitisland.game.service.RecipeMaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DrinkShopServiceImplTest {

    private InventoryService inventoryService;
    private PlayerRecipeService playerRecipeService;
    private RecipeConfigService recipeConfigService;
    private RecipeMaterialService recipeMaterialService;
    private DrinkShopServiceImpl service;

    @BeforeEach
    void setUp() {
        inventoryService = mock(InventoryService.class);
        playerRecipeService = mock(PlayerRecipeService.class);
        recipeConfigService = mock(RecipeConfigService.class);
        recipeMaterialService = mock(RecipeMaterialService.class);
        service = new DrinkShopServiceImpl(
                inventoryService, playerRecipeService, recipeConfigService, recipeMaterialService,
                mock(com.fruitisland.game.mapper.PlayerDrinkShopMapper.class),
                mock(com.fruitisland.game.mapper.DrinkShopLevelConfigMapper.class),
                mock(com.fruitisland.game.mapper.GamePlayerMapper.class),
                mock(com.fruitisland.game.mapper.DrinkBarMapper.class));

        PlayerRecipe qualification = new PlayerRecipe();
        qualification.setPlayerId(7L);
        qualification.setRecipeId("strawberry_juice");
        when(playerRecipeService.findPermanent(7L, "strawberry_juice")).thenReturn(qualification);
        when(playerRecipeService.findActive(7L, "strawberry_juice")).thenReturn(qualification);

        RecipeConfig recipe = new RecipeConfig();
        recipe.setId("strawberry_juice");
        recipe.setName("草莓汁");
        recipe.setOutputItem("strawberry_juice");
        when(recipeConfigService.getById("strawberry_juice")).thenReturn(recipe);

        RecipeMaterial material = new RecipeMaterial();
        material.setRecipeId("strawberry_juice");
        material.setItemId("strawberry");
        material.setCount(2);
        when(recipeMaterialService.listByRecipe("strawberry_juice")).thenReturn(List.of(material));

        Inventory strawberries = new Inventory();
        strawberries.setPlayerId(7L);
        strawberries.setItemId("strawberry");
        strawberries.setCount(10);
        when(inventoryService.findByPlayerAndItem(7L, "strawberry")).thenReturn(strawberries);
        doAnswer(invocation -> {
            strawberries.setCount(strawberries.getCount() - invocation.getArgument(2, Integer.class));
            return null;
        }).when(inventoryService).removeItem(eq(7L), eq("strawberry"), anyInt());
    }

    @Test
    void craftingFourStrawberryJuicesConsumesEightStrawberriesAndAddsFourDrinksWithoutRewards() {
        GamePlayer player = new GamePlayer();
        player.setId(7L);
        player.setGold(500L);
        player.setExp(12);

        CraftResultVO result = service.craft(player, "strawberry_juice", 4);

        verify(inventoryService).removeItem(7L, "strawberry", 8);
        verify(inventoryService).addItem(7L, "strawberry_juice", 4);
        assertEquals(2, result.getMaterials().get(0).getRemaining());
        assertEquals(4, result.getOutputCount());
        assertEquals(500L, player.getGold());
        assertEquals(12, player.getExp());
    }

    @Test
    void craftingWithInsufficientMaterialsDoesNotPartiallyChangeInventory() {
        Inventory strawberries = new Inventory();
        strawberries.setCount(7);
        when(inventoryService.findByPlayerAndItem(7L, "strawberry")).thenReturn(strawberries);

        assertThrows(IllegalArgumentException.class,
                () -> service.craft(new GamePlayer(), "strawberry_juice", 4));

        verify(inventoryService, never()).removeItem(anyLong(), anyString(), anyInt());
        verify(inventoryService, never()).addItem(anyLong(), anyString(), anyInt());
    }

    @Test
    void craftingRejectsInvalidQuantityAndMissingRecipeQualificationWithoutChangingInventory() {
        GamePlayer player = new GamePlayer();
        player.setId(7L);

        assertThrows(IllegalArgumentException.class,
                () -> service.craft(player, "strawberry_juice", 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.craft(player, "strawberry_juice", 100));

        when(playerRecipeService.findActive(7L, "strawberry_juice")).thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.craft(player, "strawberry_juice", 1));

        verify(inventoryService, never()).removeItem(anyLong(), anyString(), anyInt());
        verify(inventoryService, never()).addItem(anyLong(), anyString(), anyInt());
    }

    @Test
    void craftingStationShowsMaterialInventoryAndCapsMaximumAtNinetyNine() {
        PlayerRecipe qualification = playerRecipeService.findPermanent(7L, "strawberry_juice");
        when(playerRecipeService.listByPlayer(7L)).thenReturn(List.of(qualification));
        Inventory strawberries = new Inventory();
        strawberries.setCount(250);
        when(inventoryService.findByPlayerAndItem(7L, "strawberry")).thenReturn(strawberries);

        var recipe = service.getCraftingStation(7L).getRecipes().get(0);

        assertEquals("strawberry_juice", recipe.getRecipeId());
        assertEquals(2, recipe.getMaterials().get(0).getRequiredCount());
        assertEquals(250, recipe.getMaterials().get(0).getInventoryCount());
        assertEquals(99, recipe.getMaxCraftable());
    }
}
