package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.RecipeMaterial;
import com.fruitisland.game.mapper.RecipeMaterialMapper;
import com.fruitisland.game.service.RecipeMaterialService;
import org.springframework.stereotype.Service;

@Service
public class RecipeMaterialServiceImpl extends BaseServiceImplX<RecipeMaterialMapper, RecipeMaterial> implements RecipeMaterialService {
    @Override
    public java.util.List<RecipeMaterial> listByRecipe(String recipeId) {
        return lambdaQuery().eq(RecipeMaterial::getRecipeId, recipeId).list();
    }
}
