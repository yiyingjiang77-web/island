package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("player_recipe")
public class PlayerRecipe {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long playerId;
    private String recipeId;
    private String qualificationType;
    private String unlockSource;
    private LocalDateTime unlockTime;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
}
