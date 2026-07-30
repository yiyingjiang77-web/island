package com.fruitisland.user.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.user.entity.UserLogin;
import com.fruitisland.user.mapper.UserLoginMapper;
import com.fruitisland.user.service.UserLoginService;
import org.springframework.stereotype.Service;

@Service
public class UserLoginServiceImpl extends BaseServiceImplX<UserLoginMapper, UserLogin> implements UserLoginService {

    @Override
    public UserLogin findByPlatformAndUid(String platform, String platformUid) {
        return baseMapper.selectByPlatformAndUid(platform, platformUid);
    }

    @Override
    public UserLogin createLogin(Long userId, String platform, String platformUid) {
        UserLogin login = new UserLogin();
        login.setUserId(userId);
        login.setPlatform(platform);
        login.setPlatformUid(platformUid);
        save(login);
        return login;
    }
}
