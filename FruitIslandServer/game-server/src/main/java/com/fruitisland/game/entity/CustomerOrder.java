package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_order")
public class CustomerOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    private String customerId;

    private String recipeId;

    private String itemId;

    private Integer quantity;

    private Integer unitGoldSnapshot;

    private Integer unitExpSnapshot;

    private Integer queuePosition;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime closeTime;

    private String closeReason;
}
