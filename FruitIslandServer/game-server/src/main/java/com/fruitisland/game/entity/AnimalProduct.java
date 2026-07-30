package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("animal_product")
public class AnimalProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long animalId;

    private String itemId;

    private LocalDateTime finishTime;

    private String status;
}
