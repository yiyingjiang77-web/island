package com.fruitisland.user.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.user.entity.User;

public interface UserService extends BaseServiceX<User> {

    /** 创建新用户 */
    User createUser(String nickname, String avatar);
}
