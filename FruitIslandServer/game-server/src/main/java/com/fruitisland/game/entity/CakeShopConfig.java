package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 蛋糕店等级配置。
 */
@Data
@TableName("cake_shop_config")
public class CakeShopConfig {

    @TableId(type = IdType.INPUT)
    private Integer level;

    private Integer requiredIslandLevel;

    private Integer upgradeGold;

    private Integer rackCapacity;

    private Integer saleIntervalSeconds;

    private Integer enabled;
}
