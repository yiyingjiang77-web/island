package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("land")
public class Land {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long areaId;
    private Integer positionX;
    private Integer positionY;
    private String state;
    private Integer unlockLevel;
    private LocalDateTime createTime;
}
