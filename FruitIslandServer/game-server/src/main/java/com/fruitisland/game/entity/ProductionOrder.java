package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("production_order")
public class ProductionOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    private Long buildingId;

    private String recipeId;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private String status;

    private LocalDateTime createTime;
}
