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
}
