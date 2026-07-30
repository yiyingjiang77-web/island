package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("drink_bar_batch")
public class DrinkBarBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;
    private Long barId;
    private String recipeId;
    private String itemId;
    private Integer listedQuantity;
    private Integer soldQuantity;
    private String status;
    private Integer activeMarker;
    private Integer unitGoldSnapshot;
    private Integer unitExpSnapshot;
    private Integer saleIntervalSecondsSnapshot;
    private LocalDateTime listedAt;
    private LocalDateTime soldOutAt;
    private LocalDateTime closedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
