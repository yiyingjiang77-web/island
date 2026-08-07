package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家永久花卉种植权。
 */
@Data
@TableName("player_flower_right")
public class PlayerFlowerRight {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    private String flowerId;

    private Integer flowerLevel;

    private String unlockSource;

    private LocalDateTime unlockTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
