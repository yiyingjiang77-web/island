package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家蜂箱状态。
 */
@Data
@TableName("player_beehive")
public class PlayerBeehive {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    /** 蜂箱数量（0-3） */
    private Integer beehiveCount;

    /** 当前存储蜂蜜量 */
    private Integer honeyStored;

    /** 上次产蜜结算时间 */
    private LocalDateTime lastProduceTime;

    /** 上次收取时间 */
    private LocalDateTime lastCollectTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
