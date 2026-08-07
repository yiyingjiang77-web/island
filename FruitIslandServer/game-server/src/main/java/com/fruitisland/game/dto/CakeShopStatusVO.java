package com.fruitisland.game.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class CakeShopStatusVO implements Serializable {

    private long playerGold;
    private int islandLevel;

    private ShopStatus shop;

    @Data
    public static class ShopStatus implements Serializable {
        private boolean unlocked;
        private int level;
        private int rackCapacity;
        private int saleIntervalSeconds;

        /** 下一等级配置（null 表示已满级） */
        private Map<String, Object> nextLevel;

        /** 是否可解锁（仅未解锁时有意义） */
        private boolean canUnlock;
        private String unlockHint;

        /** 当前等级配置 */
        private Map<String, Object> currentConfig;

        /** 所有已启用等级配置 */
        private List<Map<String, Object>> allLevels;
    }
}
