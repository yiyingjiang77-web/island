package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("building_upgrade")
public class BuildingUpgrade {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long buildingId;

    private Integer oldLevel;

    private Integer newLevel;

    private Integer costGold;

    private LocalDateTime upgradeTime;
}
