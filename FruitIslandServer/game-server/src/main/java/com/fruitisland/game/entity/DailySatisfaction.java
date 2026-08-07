package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_satisfaction")
public class DailySatisfaction {
    @TableId(type = IdType.AUTO) private Long id;
    private Long playerId;
    private LocalDate businessDate;
    private Integer deliveredOrders;
    private Integer rejectedOrders;
    private Integer closedOrders;
    private Integer deliveredQuantity;
    private Integer satisfactionPercent;
    private String giftTierSnapshot;
    private Long rewardGoldSnapshot;
    private String rewardStatus;
    private LocalDateTime settledAt;
    private LocalDateTime createTime;
}
