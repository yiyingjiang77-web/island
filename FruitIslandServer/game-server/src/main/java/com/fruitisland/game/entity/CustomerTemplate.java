package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("customer_template")
public class CustomerTemplate {

    @TableId
    private String id;

    private String name;

    private String avatar;

    private String type;
}
