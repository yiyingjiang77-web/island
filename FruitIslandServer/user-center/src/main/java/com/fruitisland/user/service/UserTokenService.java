package com.fruitisland.user.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.user.entity.UserToken;

public interface UserTokenService extends BaseServiceX<UserToken> {

    /** 根据 token 字符串查找 */
    UserToken findByToken(String token);

    /** 创建登录 Token */
    UserToken createToken(Long userId, String token, java.time.LocalDateTime expireTime);

    /** 清除用户所有 Token */
    void clearUserTokens(Long userId);
}
