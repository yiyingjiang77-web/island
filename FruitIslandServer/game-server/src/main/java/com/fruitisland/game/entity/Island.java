package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("island")
public class Island {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long playerId;
    private String islandName;
    private Integer level;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
