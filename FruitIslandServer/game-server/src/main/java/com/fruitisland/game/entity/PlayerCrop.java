package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家永久作物权限。
 *
 * <p>记录存在即代表玩家永久拥有该品种，可无限次种植；种植不消耗背包物品。</p>
 */
@Data
@TableName("player_crop")
public class PlayerCrop {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 玩家角色 ID。 */
    private Long playerId;

    /** 永久拥有的作物编码。 */
    private String cropId;

    /** 当前作物等级，升级后永久生效。 */
    private Integer cropLevel;

    /** 永久种植权来源：INITIAL / GOLD_SHOP / DIAMOND_SHOP / LEVEL_REWARD 等。 */
    private String unlockSource;

    /** 获得永久种植权的时间。 */
    private LocalDateTime unlockTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
