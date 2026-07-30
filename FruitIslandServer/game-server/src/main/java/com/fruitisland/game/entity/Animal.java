package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("animal")
public class Animal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    private Long areaId;

    private String type;

    private Integer level;

    private LocalDateTime createTime;
}
