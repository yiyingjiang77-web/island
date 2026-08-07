package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 牛棚等级配置。
 */
@Data
@TableName("barn_config")
public class BarnConfig {

    @TableId(type = IdType.INPUT)
    private Integer level;

    private Integer requiredIslandLevel;

    private Integer upgradeGold;

    private Integer animalCapacity;

    private Integer animalAdded;

    private Integer produceCycleSeconds;

    private Integer milkPerCow;

    private Integer enabled;
}
