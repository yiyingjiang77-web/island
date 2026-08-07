package com.fruitisland.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class RecipeShopVO {

    private long playerGold;
    private List<RecipeItem> recipes;

    @Data
    @AllArgsConstructor
    public static class RecipeItem {
        private String recipeId;
        private String recipeName;
        private String shopType;
        private int price;
        private String category;
        private boolean purchased;
    }
}
