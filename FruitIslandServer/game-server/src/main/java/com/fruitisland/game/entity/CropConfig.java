package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作物基础配置。
 *
 * <p>这里只保存“品种本身”的属性；不同作物等级的成熟时间、产量和升级费用
 * 统一放在 {@code crop_level_config}，避免一个作物重复保存多行基础资料。</p>
 */
@Data
@TableName("crop_config")
public class CropConfig {

    /** 作物唯一编码，同时也是收获后进入背包的 item_config.id，例如 strawberry。 */
    @TableId
    private String cropId;

    /** 作物显示名称。 */
    private String name;

    /** 稀有度：COMMON / RARE / EPIC / LEGENDARY。 */
    private String rarity;

    /** 是否允许进入随机奖励池：0 否，1 是；普通作物必须为 0。 */
    private Integer rewardEligible;

    /** 是否允许通过普通渠道获得永久种植权：0 否，1 是。 */
    private Integer permanentUnlockEnabled;

    /** 永久拥有后是否可以花金币升级：0 否，1 是。 */
    private Integer upgradeEnabled;

    /** 玩家达到该等级后，才允许种植或永久解锁该品种。 */
    private Integer playerUnlockLevel;

    /** 作物可升级到的最高等级。 */
    private Integer maxCropLevel;

    /** 是否启用该作物：0 停用，1 启用。 */
    private Integer enabled;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
