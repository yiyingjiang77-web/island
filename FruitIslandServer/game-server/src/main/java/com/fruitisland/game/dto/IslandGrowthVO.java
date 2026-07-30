package com.fruitisland.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IslandGrowthVO implements Serializable {
    private Integer cumulativeExp;
    private Integer currentLevel;
    private Integer nextLevelThreshold;
    private List<IslandLevelRewardVO> rewards;
}
