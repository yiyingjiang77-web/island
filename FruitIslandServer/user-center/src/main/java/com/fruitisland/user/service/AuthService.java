package com.fruitisland.user.service;

import java.util.Map;

public interface AuthService {

    /**
     * 微信登录
     *
     * @param code 微信登录 code（开发环境可传 mock code）
     * @return {token, userId, expireTime}
     */
    Map<String, Object> wechatLogin(String code);
}
