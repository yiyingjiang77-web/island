package com.fruitisland.game.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DrinkBarStateVO(List<BarView> bars, List<DrinkView> drinks,
                              int shopLevel, int listingLimit, int saleIntervalSeconds) {

    public record DrinkView(
            String recipeId,
            String name,
            String itemId,
            int inventoryCount,
            int listingQuantity,
            int unitGold,
            int unitExp,
            int expectedBatchGold,
            int expectedBatchExp,
            int saleIntervalSeconds
    ) {
    }

    public record BarView(
            Long barId,
            Integer slotNumber,
            boolean opened,
            String state,
            BatchView batch
    ) {
    }

    public record BatchView(
            Long batchId,
            String recipeId,
            String itemId,
            Integer listedQuantity,
            Integer soldQuantity,
            Integer remainingQuantity,
            Integer pendingGold,
            Integer pendingExp,
            Long nextSaleInSeconds,
            String status,
            Integer unitGoldSnapshot,
            Integer unitExpSnapshot,
            Integer saleIntervalSecondsSnapshot,
            LocalDateTime listedAt,
            LocalDateTime soldOutAt
    ) {
    }
}
