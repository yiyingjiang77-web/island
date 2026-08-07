package com.fruitisland.game.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class LivestockStatusVO implements Serializable {

    private long playerGold;
    private int islandLevel;

    private BuildingStatus barn;
    private BuildingStatus coop;

    @Data
    public static class BuildingStatus implements Serializable {
        private boolean unlocked;
        private int level;
        private int animalCount;
        private int capacity;
        private int cycleSeconds;
        private Long remainingSeconds;
        private int productionPerCycle;

        /** 下一等级配置（null 表示已满级）。 */
        private Map<String, Object> nextLevel;

        /** 是否可解锁（仅未解锁时有意义）。 */
        private boolean canUnlock;
        private String unlockHint;

        /** 当前等级配置。 */
        private Map<String, Object> currentConfig;

        /** 所有已启用等级配置（供客户端展示升级路径）。 */
        private List<Map<String, Object>> allLevels;
    }
}
