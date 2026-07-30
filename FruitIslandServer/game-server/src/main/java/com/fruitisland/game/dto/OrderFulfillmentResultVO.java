package com.fruitisland.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderFulfillmentResultVO {
    private Long orderId;
    private String status;
    private int deliveredQuantity;
    private int craftedQuantity;
    private int excessQuantity;
    private int earnedGold;
    private int earnedExp;
    private Integer playerLevel;
    private Integer playerExp;
    private Long playerGold;
}
