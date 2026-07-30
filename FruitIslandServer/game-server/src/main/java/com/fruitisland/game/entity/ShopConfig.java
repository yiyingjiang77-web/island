package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("shop_config")
public class ShopConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String itemId;

    private Integer price;

    private Integer buyLimit;
}
