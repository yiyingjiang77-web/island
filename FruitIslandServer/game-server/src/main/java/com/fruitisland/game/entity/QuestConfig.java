package com.fruitisland.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("quest_config")
public class QuestConfig {

    @TableId
    private String id;

    private String type;

    private String title;

    private String conditionJson;

    private String rewardJson;
}
