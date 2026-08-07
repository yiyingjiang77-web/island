package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家蛋糕店状态。
 */
@Data
@TableName("player_cake_shop")
public class PlayerCakeShop {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    /** 0 = 未解锁 */
    private Integer level;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
