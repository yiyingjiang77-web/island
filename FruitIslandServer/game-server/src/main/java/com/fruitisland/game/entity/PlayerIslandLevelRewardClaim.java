package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("player_island_level_reward_claim")
public class PlayerIslandLevelRewardClaim {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long playerId;
    private Integer islandLevel;
    private LocalDateTime claimedAt;
    private LocalDateTime createTime;
}
