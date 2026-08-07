package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 花卉基础配置；花卉只能种在 FLOWER 类型土地。 */
@Data
@TableName("flower_config")
public class FlowerConfig {
    @TableId
    private String flowerId;
    private String name;
    private String purchaseCurrency;
    private Long purchasePrice;
    private Integer honeyCoefficient;
    private Integer maxFlowerLevel;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
