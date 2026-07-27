package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crop_plant")
public class CropPlant {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long landId;
    private String cropId;
    private LocalDateTime plantTime;
    private LocalDateTime finishTime;
    private String status;
    private LocalDateTime createTime;
}
