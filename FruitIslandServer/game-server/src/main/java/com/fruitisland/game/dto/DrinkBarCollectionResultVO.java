package com.fruitisland.game.dto;

import java.util.List;

public record DrinkBarCollectionResultVO(
        List<Long> collectedBarIds,
        int collectedBarCount,
        int settledGold,
        int settledExp,
        long currentGold,
        int cumulativeExp,
        int currentLevel
) {
}
