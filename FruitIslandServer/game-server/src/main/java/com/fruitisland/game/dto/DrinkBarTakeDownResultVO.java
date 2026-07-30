package com.fruitisland.game.dto;

public record DrinkBarTakeDownResultVO(
        DrinkBarStateVO.BarView bar,
        int returnedQuantity,
        int settledGold,
        int settledExp,
        long currentGold,
        int currentExp,
        int currentLevel
) {
}
