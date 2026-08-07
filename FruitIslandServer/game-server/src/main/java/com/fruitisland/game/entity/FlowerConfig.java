package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 花卉基础配置。
 */
@Data
@TableName("flower_config")
public class FlowerConfig {

    @TableId
    private String flowerId;

    private String name;

    /** GOLD / DIAMOND */
    private String currencyType;

    /** 种子（永久种植权）价格 */
    private Long seedPrice;

    /** 一级成熟秒数 */
    private Integer growSeconds;

    /** 一级产量 */
    private Integer yieldCount;

    /** 一级收获经验 */
    private Integer harvestExp;

    /** 蜂蜜系数：金币花=1 钻石花=2 */
    private Integer honeyCoefficient;

    /** 花卉最高等级 */
    private Integer maxLevel;

    private Integer enabled;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
