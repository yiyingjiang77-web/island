package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("island_level_config")
public class IslandLevelConfig {

    @TableId
    private Integer level;
    private Integer cumulativeExp;
    private String cropId;
    private String recipeId;
    private String materialSourceHint;
    private String shopCapabilityHint;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
