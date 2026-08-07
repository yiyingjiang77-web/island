package com.fruitisland.game.service;

import com.fruitisland.game.dto.DrinkBarListingResultVO;
import com.fruitisland.game.dto.DrinkBarCollectionResultVO;
import com.fruitisland.game.dto.DrinkBarStateVO;
import com.fruitisland.game.dto.DrinkBarTakeDownResultVO;

public interface DrinkBarService {

    DrinkBarStateVO getBars(Long playerId);

    DrinkBarListingResultVO listDrink(Long playerId, Long barId, String recipeId);

    DrinkBarListingResultVO listDrink(Long playerId, Long barId, String recipeId, int quantity);

    DrinkBarTakeDownResultVO takeDown(Long playerId, Long barId);

    DrinkBarCollectionResultVO collect(Long playerId, Long barId);

    DrinkBarCollectionResultVO collectAll(Long playerId);
}
