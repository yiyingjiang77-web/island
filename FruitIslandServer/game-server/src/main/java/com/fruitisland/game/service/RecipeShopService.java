package com.fruitisland.game.service;

import com.fruitisland.game.dto.RecipeShopVO;

public interface RecipeShopService {

    /** 获取配方商店列表（含玩家购买状态） */
    RecipeShopVO listRecipes(Long playerId);

    /** 购买配方（扣金币 + grantPermanent） */
    RecipeShopVO.RecipeItem buyRecipe(Long playerId, String recipeId);
}
