package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_arrival_state")
public class CustomerArrivalState {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long playerId;
    private LocalDateTime nextArrivalAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
