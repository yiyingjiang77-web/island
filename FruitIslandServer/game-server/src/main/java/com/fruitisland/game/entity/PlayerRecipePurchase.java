package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("player_recipe_purchase")
public class PlayerRecipePurchase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long playerId;
    private String recipeId;
    private Integer pricePaid;
    private LocalDateTime purchasedAt;
}
