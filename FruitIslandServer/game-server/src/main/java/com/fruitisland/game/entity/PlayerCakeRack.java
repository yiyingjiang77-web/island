package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家蛋糕架批次。
 */
@Data
@TableName("player_cake_rack")
public class PlayerCakeRack {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    /** 1 或 2 */
    private Integer slot;

    private String recipeId;

    private String cakeItem;

    /** 上架总数 */
    private Integer quantity;

    /** 已售数量 */
    private Integer sold;

    /** EMPTY / SELLING / SOLD_OUT */
    private String status;

    /** 快照：单份金币 */
    private Integer saleGoldSnapshot;

    /** 快照：单份经验 */
    private Integer saleExpSnapshot;

    /** 快照：销售间隔秒数 */
    private Integer saleIntervalSnapshot;

    private LocalDateTime listTime;

    private LocalDateTime lastSettleTime;

    private LocalDateTime closeTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
