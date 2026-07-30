package com.fruitisland.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_device")
public class UserDevice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String deviceType;

    private String deviceId;

    private String appVersion;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;
}
