package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_player")
public class GamePlayer {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String gameId;
    private String nickname;
    private Integer level;
    private Integer exp;
    private Integer cumulativeExp;
    private Long gold;
    private Integer diamond;
    private String avatarId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
