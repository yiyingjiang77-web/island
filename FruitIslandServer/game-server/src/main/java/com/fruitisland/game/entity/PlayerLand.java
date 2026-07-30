package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String cropId;

    /** 种下时的作物等级快照，之后升级不会改变本轮作物。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer cropLevel;

    /** 本轮成熟秒数快照。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer growSecondsSnapshot;

    /** 本轮收获数量快照。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer yieldCountSnapshot;

    /** 本轮收获经验快照；成长过程中升级作物不会改变本轮奖励。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer harvestExpSnapshot;

    /** 本轮种植权限来源：PERMANENT / TEMPORARY。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String accessType;

    /** 临时权限 ID；永久种植时为空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long accessGrantId;

    /** 种植时间 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime plantTime;

    /**
     * 成熟时间。
     * ALWAYS 很重要：重新播种时必须把上一轮 finish_time 显式清成 NULL，
     * 否则 MyBatis-Plus 默认忽略 null，旧时间会让新种子立即成熟。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime finishTime;

    /** 水分值 0-100 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer waterLevel;

    /** 上次浇水时间 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime lastWateredAt;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
