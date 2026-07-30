package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("decoration")
public class Decoration {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    private String itemId;

    private Integer positionX;

    private Integer positionY;

    private Integer rotation;

    private LocalDateTime createTime;
}
