package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家等级成长配置。
 *
 * <p>{@code requiredExp} 表示从当前 level 升到下一级需要的经验，
 * {@code rewardGold} 在升级成功时自动发放。</p>
 */
@Data
@TableName("player_level_config")
public class PlayerLevelConfig {

    @TableId
    private Integer level;

    /** 从当前等级升到下一级所需经验。 */
    private Integer requiredExp;

    /** 升到下一级时奖励的金币。 */
    private Long rewardGold;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
