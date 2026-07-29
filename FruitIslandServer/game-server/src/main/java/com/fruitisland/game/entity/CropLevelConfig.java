package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作物等级数值配置。
 *
 * <p>成熟秒数、收获数量、升级金币都从数据库读取。修改平衡数值不需要改代码。</p>
 */
@Data
@TableName("crop_level_config")
public class CropLevelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作物编码，对应 crop_config.crop_id。 */
    private String cropId;

    /** 作物等级，从 1 开始。 */
    private Integer cropLevel;

    /** 浇水后到成熟所需秒数。 */
    private Integer growSeconds;

    /** 本等级单次收获数量。 */
    private Integer yieldCount;

    /** 从上一等级升级到本等级所需金币；1 级固定为 0。 */
    private Long upgradeGold;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
