package com.fruitisland.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 土地视图 VO — 合并 land_config + player_land + 动态状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandVO implements Serializable {

    /** land_config.id */
    private Long landConfigId;

    /** FARM / FLOWER */
    private String areaType;

    /** Farm-A, Farm-B 等 */
    private String blockId;

    /** Block 内坐标 */
    private Integer gridX;
    private Integer gridY;

    /** 动态状态: LOCKED / UNPURCHASED / EMPTY / PLANTED / READY */
    private String status;

    /** 解锁等级 */
    private Integer unlockLevel;

    /** 购买价格（仅 UNPURCHASED 时有意义） */
    private Long buyPrice;

    // --- 以下字段仅在 PLANTED / READY 时有值 ---

    /** player_land.id（已购买才有） */
    private Long playerLandId;

    /** 当前种植的作物 ID */
    private String cropId;

    /** 种植时间 */
    private LocalDateTime plantTime;

    /** 成熟时间 */
    private LocalDateTime finishTime;

    /** 水分值 0-100（动态计算） */
    private Integer waterLevel;
}
