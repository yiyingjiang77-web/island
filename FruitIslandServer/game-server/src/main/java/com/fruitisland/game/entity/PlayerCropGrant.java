package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家限时稀有作物权限。
 *
 * <p>有效期内可以无限次种植，但不可升级。权限到期不影响已经种下的作物。</p>
 */
@Data
@TableName("player_crop_grant")
public class PlayerCropGrant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 获得奖励的玩家角色 ID。 */
    private Long playerId;

    /** 限时可种植的稀有作物编码。 */
    private String cropId;

    /** 奖励指定的固定作物等级。 */
    private Integer grantCropLevel;

    /** 奖励来源，例如 QUEST / EVENT / GIFT / REWARD_POOL。 */
    private String grantSource;

    /** 外部来源编号，例如任务 ID 或活动 ID，便于追踪。 */
    private String sourceRefId;

    /** 权限生效时间。 */
    private LocalDateTime validFrom;

    /** 权限失效时间。 */
    private LocalDateTime validUntil;

    /** 状态：ACTIVE / EXPIRED / REVOKED。 */
    private String status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
