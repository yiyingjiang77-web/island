package com.fruitisland.game.service;

import com.fruitisland.game.dto.CraftResultVO;
import com.fruitisland.game.dto.CraftingStationVO;
import com.fruitisland.game.entity.GamePlayer;

public interface DrinkShopService {
    CraftingStationVO getCraftingStation(Long playerId);
    CraftResultVO craft(GamePlayer player, String recipeId, int quantity);
}
