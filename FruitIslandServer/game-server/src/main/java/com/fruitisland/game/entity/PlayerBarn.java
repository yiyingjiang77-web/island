package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家牛棚状态。
 */
@Data
@TableName("player_barn")
public class PlayerBarn {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    /** 0 = 未解锁 */
    private Integer level;

    private Integer cowCount;

    /** 当前生产周期开始时间 */
    private LocalDateTime cycleStartTime;

    /** 当前周期快照：周期秒数 */
    private Integer cycleSeconds;

    /** 当前周期快照：奶牛数 */
    private Integer cowCountSnapshot;

    /** 当前周期快照：单头产量 */
    private Integer milkPerCowSnapshot;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
