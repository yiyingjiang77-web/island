package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家鸡舍状态。
 */
@Data
@TableName("player_coop")
public class PlayerCoop {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    /** 0 = 未解锁 */
    private Integer level;

    private Integer chickenCount;

    /** 当前生产周期开始时间 */
    private LocalDateTime cycleStartTime;

    /** 当前周期快照：周期秒数 */
    private Integer cycleSeconds;

    /** 当前周期快照：鸡数 */
    private Integer chickenCountSnapshot;

    /** 当前周期快照：额外鸡蛋 */
    private Integer bonusEggSnapshot;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
