package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.dto.CraftResultVO;
import com.fruitisland.game.entity.PlayerCakeShop;

import java.util.List;
import java.util.Map;

public interface PlayerCakeShopService extends BaseServiceX<PlayerCakeShop> {

    PlayerCakeShop getOrCreate(Long playerId);

    PlayerCakeShop unlockCakeShop(Long playerId);

    PlayerCakeShop upgradeCakeShop(Long playerId);

    /** 制作蛋糕 */
    CraftResultVO craft(Long playerId, String recipeId, int quantity);

    /** 列出可制作的蛋糕配方（含材料库存和最大可制作数） */
    List<Map<String, Object>> listCraftableRecipes(Long playerId);
}
