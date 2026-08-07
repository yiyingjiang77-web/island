package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作物永久种植权获得渠道配置。
 *
 * <p>例如金币商店、元宝商店、玩家等级奖励。奖励池中的限时稀有种子不放在本表。</p>
 */
@Data
@TableName("crop_unlock_source")
public class CropUnlockSource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作物编码。 */
    private String cropId;

    /** 获得渠道：INITIAL / GOLD_SHOP / DIAMOND_SHOP / LEVEL_REWARD。 */
    private String sourceType;

    /** 支付货币：NONE / GOLD / DIAMOND。 */
    private String currencyType;

    /** 获得永久种植权所需价格；免费渠道为 0。 */
    private Long price;

    /** 玩家达到该等级后才能使用此渠道。 */
    private Integer requiredPlayerLevel;

    /** 外部配置编号，例如商店商品 ID 或等级奖励 ID。 */
    private String sourceRefId;

    /** 是否启用：0 否，1 是。 */
    private Integer enabled;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
