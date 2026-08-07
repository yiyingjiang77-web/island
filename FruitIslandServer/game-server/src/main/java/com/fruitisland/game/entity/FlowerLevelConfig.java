package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 花卉等级数值配置。 */
@Data
@TableName("flower_level_config")
public class FlowerLevelConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String flowerId;
    private Integer flowerLevel;
    private Integer growSeconds;
    private Integer yieldCount;
    private Integer harvestExp;
    private Long upgradeGold;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
