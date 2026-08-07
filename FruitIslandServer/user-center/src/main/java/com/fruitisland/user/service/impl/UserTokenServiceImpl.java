package com.fruitisland.user.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.user.entity.UserToken;
import com.fruitisland.user.mapper.UserTokenMapper;
import com.fruitisland.user.service.UserTokenService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserTokenServiceImpl extends BaseServiceImplX<UserTokenMapper, UserToken> implements UserTokenService {

    @Override
    public UserToken findByToken(String token) {
        return baseMapper.selectByToken(token);
    }

    @Override
    public UserToken createToken(Long userId, String token, LocalDateTime expireTime) {
        // 先清除旧 Token
        clearUserTokens(userId);

        UserToken userToken = new UserToken();
        userToken.setUserId(userId);
        userToken.setToken(token);
        userToken.setExpireTime(expireTime);
        save(userToken);
        return userToken;
    }

    @Override
    public void clearUserTokens(Long userId) {
        baseMapper.deleteByUserId(userId);
    }
}
