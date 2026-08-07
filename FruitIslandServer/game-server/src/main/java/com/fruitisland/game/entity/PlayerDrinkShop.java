package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("player_drink_shop")
public class PlayerDrinkShop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long playerId;
    private Integer shopLevel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
