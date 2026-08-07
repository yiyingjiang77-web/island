package com.fruitisland.game.dto;

import java.time.LocalDate;
import java.util.List;

public record SatisfactionStatusVO(Today today, List<GiftRule> giftRules,
                                   List<History> recentHistory, List<History> autoSettledRewards) {
    public record Today(LocalDate businessDate, int deliveredOrders, int rejectedOrders,
                        int closedOrders, int deliveredQuantity, int satisfactionPercent,
                        String expectedTier, long expectedGold, int quantityNeeded,
                        String nextTier, int nextTierPercentNeeded) {}
    public record GiftRule(String tierCode, int minimumPercent,
                           int minimumDeliveredQuantity, long rewardGold) {}
    public record History(LocalDate businessDate, int deliveredOrders, int rejectedOrders,
                          int closedOrders, int deliveredQuantity, int satisfactionPercent,
                          String giftTier, long rewardGold, String rewardStatus) {}
}
