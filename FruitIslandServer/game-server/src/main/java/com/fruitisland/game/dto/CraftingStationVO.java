package com.fruitisland.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CraftingStationVO {
    private List<RecipeView> recipes;
    private List<CustomerQueueVO.CustomerView> customers;
    private LocalDateTime nextCustomerArrivalAt;
    private int arrivalIntervalSeconds;
    private int shopLevel;
    private int capacity;

    public CraftingStationVO(List<RecipeView> recipes) {
        this.recipes = recipes;
        this.customers = List.of();
    }

    public void includeQueue(CustomerQueueVO queue) {
        this.customers = queue.getCustomers();
        this.nextCustomerArrivalAt = queue.getNextCustomerArrivalAt();
        this.arrivalIntervalSeconds = queue.getArrivalIntervalSeconds();
        this.shopLevel = queue.getShopLevel();
        this.capacity = queue.getCapacity();
    }

    @Data
    @AllArgsConstructor
    public static class RecipeView {
        private String recipeId;
        private String name;
        private String outputItem;
        private int outputCount;
        private int maxCraftable;
        private List<MaterialView> materials;
    }

    @Data
    @AllArgsConstructor
    public static class MaterialView {
        private String itemId;
        private int requiredCount;
        private int inventoryCount;
    }
}
