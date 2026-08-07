package com.fruitisland.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class CustomerQueueVO {
    private List<CustomerView> customers;
    private LocalDateTime nextCustomerArrivalAt;
    private int arrivalIntervalSeconds;
    private int shopLevel;
    private int capacity;

    @Data
    @AllArgsConstructor
    public static class CustomerView {
        private Long orderId;
        private int queuePosition;
        private String customerId;
        private String customerName;
        private String customerAvatar;
        private String recipeId;
        private String itemId;
        private int quantity;
        private int unitGold;
        private int unitExp;
        private int expectedGold;
        private int expectedExp;
        private String status;
        private LocalDateTime createdAt;
    }
}
