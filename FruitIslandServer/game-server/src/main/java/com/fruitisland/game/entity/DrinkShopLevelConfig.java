package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("drink_shop_level_config")
public class DrinkShopLevelConfig {
    @TableId
    private Long id;
    private Integer level;
    private Integer requiredIslandLevel;
    private Long renovationGold;
    private Integer queueCapacity;
    private Integer barCapacity;
    private Integer saleIntervalSeconds;
    private Integer arrivalIntervalSeconds;
    private Integer iceCreamEnabled;
    private Integer advancedRecipeEnabled;
    private Integer configVersion;
    private LocalDateTime effectiveFrom;
    private Integer enabled;
    private String improvementText;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
