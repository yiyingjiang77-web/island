package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.PlayerRecipe;

import java.util.List;

public interface PlayerRecipeService extends BaseServiceX<PlayerRecipe> {
    PlayerRecipe findPermanent(Long playerId, String recipeId);
    PlayerRecipe findActive(Long playerId, String recipeId);
    PlayerRecipe grantPermanent(Long playerId, String recipeId, String source);
    List<PlayerRecipe> listByPlayer(Long playerId);
}
