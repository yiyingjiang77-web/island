package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("island_area")
public class IslandArea {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long islandId;
    private String areaType;
    private String areaName;
    private Integer unlockLevel;
    private Long unlockCost;
    private Integer status;
    private Integer positionX;
    private Integer positionY;
    private LocalDateTime createTime;
}
