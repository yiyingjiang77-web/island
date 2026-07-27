package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("building")
public class Building {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long areaId;

    private Long playerId;

    private String type;

    private Integer level;

    private Integer positionX;

    private Integer positionY;

    private Integer rotation;

    private Integer status;

    private LocalDateTime createTime;
}
