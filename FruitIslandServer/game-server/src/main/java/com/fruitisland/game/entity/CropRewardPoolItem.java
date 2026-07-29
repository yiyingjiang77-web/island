package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 稀有作物随机奖励池明细。
 */
@Data
@TableName("crop_reward_pool_item")
public class CropRewardPoolItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 奖励池编码，例如 DAILY_GIFT、EVENT_SUMMER。 */
    private String poolCode;

    /** 稀有作物编码；业务层会再次校验其 reward_eligible。 */
    private String cropId;

    /** 发放后的固定作物等级。 */
    private Integer grantCropLevel;

    /** 权重，数值越大越容易被抽中。 */
    private Integer weight;

    /** 奖励有效秒数。 */
    private Long durationSeconds;

    /** 是否启用：0 否，1 是。 */
    private Integer enabled;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
