package com.fruitisland.game.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/** 收获结算：产物与经验一次返回，客户端无需猜测奖励。 */
@Data
@Builder
public class HarvestResultVO implements Serializable {
    private Long playerLandId;
    private String cropId;
    private Integer cropLevel;
    private Integer yieldCount;
    private Integer expGained;
    private Integer playerLevel;
    private Integer playerExp;
    private Integer nextLevelExp;
    private Integer levelsGained;
    private Long levelRewardGold;
}
