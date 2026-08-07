package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 鸡舍等级配置。
 */
@Data
@TableName("coop_config")
public class CoopConfig {

    @TableId(type = IdType.INPUT)
    private Integer level;

    private Integer requiredIslandLevel;

    private Integer upgradeGold;

    private Integer animalCapacity;

    private Integer animalAdded;

    private Integer produceCycleSeconds;

    private Integer bonusEggs;

    private Integer enabled;
}
