package com.fruitisland.game.dto;

import com.fruitisland.game.entity.DrinkShopLevelConfig;

public record DrinkShopProgressVO(
        int currentLevel,
        long currentGold,
        DrinkShopLevelConfig currentConfig,
        DrinkShopLevelConfig nextConfig,
        long missingGold,
        boolean islandLevelMet,
        boolean maxLevel) {}
