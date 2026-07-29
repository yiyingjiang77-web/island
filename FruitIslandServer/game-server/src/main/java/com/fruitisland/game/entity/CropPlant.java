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
    private Long playerLandId;
    private String cropId;
    /** 种植时作物等级快照。 */
    private Integer cropLevel;
    /** 种植时成熟秒数快照。 */
    private Integer growSecondsSnapshot;
    /** 种植时收获数量快照。 */
    private Integer yieldCountSnapshot;
    /** PERMANENT / TEMPORARY。 */
    private String accessType;
    /** 临时种植权限 ID，永久权限为空。 */
    private Long accessGrantId;
    private LocalDateTime plantTime;
    private LocalDateTime finishTime;
    private String status;
    private LocalDateTime createTime;
}
