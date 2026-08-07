package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("recipe_config")
public class RecipeConfig {

    @TableId
    private String id;

    private String name;

    private String outputItem;

    private Integer makeTime;

    private Integer unlockLevel;

    private Integer saleGold;
    private Integer saleExp;
    private Integer barSaleIntervalSeconds;
    private Integer orderWeight;
    private Integer enabled;

    /** 制作入口：drink_bar / cake_shop */
    private String craftStation;

    /** 获取渠道：island_level / exchange_shop / npc_reward */
    private String obtainChannel;
}
