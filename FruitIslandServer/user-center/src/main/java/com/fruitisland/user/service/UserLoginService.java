package com.fruitisland.user.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.user.entity.UserLogin;

public interface UserLoginService extends BaseServiceX<UserLogin> {

    /** 根据平台和平台UID查找登录记录 */
    UserLogin findByPlatformAndUid(String platform, String platformUid);

    /** 创建登录记录 */
    UserLogin createLogin(Long userId, String platform, String platformUid);
}
