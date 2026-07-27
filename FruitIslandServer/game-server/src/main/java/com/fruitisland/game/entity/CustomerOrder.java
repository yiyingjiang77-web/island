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

    private String itemId;

    private Integer rewardGold;

    private String status;

    private LocalDateTime createTime;
}
