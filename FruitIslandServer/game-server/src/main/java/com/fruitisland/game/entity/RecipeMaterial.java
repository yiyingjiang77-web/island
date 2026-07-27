package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("recipe_material")
public class RecipeMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recipeId;

    private String itemId;

    private Integer count;
}
