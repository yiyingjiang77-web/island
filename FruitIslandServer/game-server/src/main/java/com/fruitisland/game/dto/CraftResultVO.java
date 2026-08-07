package com.fruitisland.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CraftResultVO {
    private String recipeId;
    private String outputItem;
    private int outputCount;
    private List<MaterialResult> materials;

    @Data
    @AllArgsConstructor
    public static class MaterialResult {
        private String itemId;
        private int consumed;
        private int remaining;
    }
}
