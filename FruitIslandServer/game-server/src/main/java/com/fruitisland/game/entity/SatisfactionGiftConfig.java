package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("satisfaction_gift_config")
public class SatisfactionGiftConfig {
    @TableId(type = IdType.AUTO) private Long id;
    private String tierCode;
    private Integer minimumPercent;
    private Integer minimumDeliveredQuantity;
    private Long rewardGold;
    private Integer configVersion;
    private LocalDate effectiveFrom;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
