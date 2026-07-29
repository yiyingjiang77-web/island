package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.RecipeMaterial;
import java.util.List;

public interface RecipeMaterialService extends BaseServiceX<RecipeMaterial> {
    List<RecipeMaterial> listByRecipe(String recipeId);
}
