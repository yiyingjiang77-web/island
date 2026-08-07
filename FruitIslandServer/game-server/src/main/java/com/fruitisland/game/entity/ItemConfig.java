package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("item_config")
public class ItemConfig {

    @TableId
    private String id;
    private String name;
    private String type;
    private String icon;
    private Integer sellPrice;
    private LocalDateTime createTime;
}
