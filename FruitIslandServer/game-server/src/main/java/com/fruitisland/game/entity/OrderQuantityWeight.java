package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("order_quantity_weight")
public class OrderQuantityWeight {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer quantity;
    private Integer weight;
    private Integer enabled;
}
