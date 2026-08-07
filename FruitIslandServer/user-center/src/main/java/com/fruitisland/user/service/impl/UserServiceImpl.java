package com.fruitisland.user.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.user.entity.User;
import com.fruitisland.user.mapper.UserMapper;
import com.fruitisland.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends BaseServiceImplX<UserMapper, User> implements UserService {

    @Override
    public User createUser(String nickname, String avatar) {
        User user = new User();
        user.setNickname(nickname != null ? nickname : "岛主");
        user.setAvatar(avatar != null ? avatar : "");
        user.setStatus(1);
        save(user);
        return user;
    }
}
