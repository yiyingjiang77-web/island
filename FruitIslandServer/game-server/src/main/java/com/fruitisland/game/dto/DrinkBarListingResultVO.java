package com.fruitisland.game.dto;

public record DrinkBarListingResultVO(
        DrinkBarStateVO.BarView bar,
        int remainingInventory,
        int expectedBatchGold,
        int expectedBatchExp
) {
}
