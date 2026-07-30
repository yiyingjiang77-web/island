package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("player_quest")
public class PlayerQuest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    private String questId;

    private Integer progress;

    private String status;

    private LocalDateTime createTime;
}
