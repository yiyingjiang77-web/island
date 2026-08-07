package com.fruitisland.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IslandLevelRewardVO implements Serializable {
    private Integer level;
    private Integer cumulativeExp;
    private String cropId;
    private String recipeId;
    private boolean claimed;
    private String materialSourceHint;
    private String shopCapabilityHint;
}
