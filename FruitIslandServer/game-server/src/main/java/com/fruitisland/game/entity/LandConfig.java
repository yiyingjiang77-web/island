package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 土地配置表 — 全局静态配置
 *
 * 96 块土地的位置、所属Block、解锁等级、购买价格
 * 与玩家数据分离，只维护一份
 */
@Data
@TableName("land_config")
public class LandConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** FARM / FLOWER */
    private String areaType;

    /** Farm-A, Farm-B, Flower-A 等 */
    private String blockId;

    /** Block 内 X 坐标 (0-3) */
    private Integer gridX;

    /** Block 内 Y 坐标 (0-3) */
    private Integer gridY;

    /** 解锁所需等级 */
    private Integer unlockLevel;

    /** 购买价格（星币） */
    private Long buyPrice;

    private LocalDateTime createTime;
}
