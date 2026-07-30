package com.fruitisland.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 一次经验结算结果，供收获和后续制作系统共用。 */
@Data
@AllArgsConstructor
public class ExpGainResult implements Serializable {
    private Integer gainedExp;
    private Integer beforeLevel;
    private Integer afterLevel;
    private Integer cumulativeExp;
    private Integer nextLevelThreshold;
    private Integer levelsGained;
    private List<IslandLevelRewardVO> levelRewards;
}
