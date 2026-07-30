package com.fruitisland.game.entity;

public enum DrinkBarBatchStatus {
    SELLING,
    SOLD_OUT,
    CLOSED;

    public static DrinkBarBatchStatus fromValue(String value) {
        try {
            return DrinkBarBatchStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("吧台批次状态无效：" + value);
        }
    }
}
