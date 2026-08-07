package com.fruitisland.game.service;

import com.fruitisland.game.dto.CraftResultVO;
import com.fruitisland.game.dto.CraftingStationVO;
import com.fruitisland.game.dto.DrinkShopProgressVO;
import com.fruitisland.game.dto.DrinkShopRenovationVO;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.DrinkShopLevelConfig;

public interface DrinkShopService {
    CraftingStationVO getCraftingStation(Long playerId);
    CraftResultVO craft(GamePlayer player, String recipeId, int quantity);
    DrinkShopProgressVO getProgress(GamePlayer player);
    DrinkShopRenovationVO renovate(GamePlayer player, int targetLevel);
    DrinkShopLevelConfig getActiveConfig(Long playerId);
}
