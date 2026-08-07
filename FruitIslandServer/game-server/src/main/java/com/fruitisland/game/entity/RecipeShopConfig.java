package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recipe_shop_config")
public class RecipeShopConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recipeId;
    private String recipeName;
    private String shopType;
    private Integer price;
    private String category;
    private Integer sortOrder;
    private Integer enabled;
    private LocalDateTime createTime;
}
