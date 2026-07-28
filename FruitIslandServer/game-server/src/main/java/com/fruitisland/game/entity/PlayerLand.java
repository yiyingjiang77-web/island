package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家土地表 — 只记录已购买的土地
 *
 * LOCKED / UNPURCHASED 状态不存储，由 API 层根据 land_config + player level 动态计算
 */
@Data
@TableName("player_land")
public class PlayerLand {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 玩家 ID */
    private Long playerId;

    /** → land_config.id */
    private Long landConfigId;

    /** EMPTY / PLANTED / READY */
    private String status;

    /** 当前种植的作物 ID（null = 空地） */
    private String cropId;

    /** 种植时间 */
    private LocalDateTime plantTime;

    /** 成熟时间 */
    private LocalDateTime finishTime;

    /** 水分值 0-100 */
    private Integer waterLevel;

    /** 上次浇水时间 */
    private LocalDateTime lastWateredAt;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
