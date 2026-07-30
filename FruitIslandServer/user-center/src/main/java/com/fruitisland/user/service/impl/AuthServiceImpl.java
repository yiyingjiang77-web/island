package com.fruitisland.user.service.impl;

import com.fruitisland.common.utils.JwtUtils;
import com.fruitisland.user.entity.User;
import com.fruitisland.user.entity.UserLogin;
import com.fruitisland.user.service.AuthService;
import com.fruitisland.user.service.UserLoginService;
import com.fruitisland.user.service.UserService;
import com.fruitisland.user.service.UserTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserLoginService userLoginService;
    private final UserTokenService userTokenService;
    private final JwtUtils jwtUtils;

    @Override
    public Map<String, Object> wechatLogin(String code) {
        // Demo1: 开发环境使用 mock code，后续接真实微信API
        String platformUid = resolveWechatUid(code);
        String platform = "wechat";

        // 1. 查找已有登录记录
        UserLogin userLogin = userLoginService.findByPlatformAndUid(platform, platformUid);
        User user;

        if (userLogin != null) {
            // 老用户
            user = userService.getById(userLogin.getUserId());
            log.info("老用户登录: userId={}", user.getId());
        } else {
            // 新用户：创建账号 + 登录记录
            user = userService.createUser("岛主", "");
            userLogin = userLoginService.createLogin(user.getId(), platform, platformUid);
            log.info("新用户注册: userId={}", user.getId());
        }

        // 2. 生成 JWT
        String token = jwtUtils.generateToken(user.getId(), user.getNickname());

        // 3. 保存 Token
        LocalDateTime expireTime = LocalDateTime.now().plusDays(1);
        userTokenService.createToken(user.getId(), token, expireTime);

        // 4. 返回
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        result.put("expireTime", expireTime.toString());
        return result;
    }

    /**
     * 解析微信 code → platformUid
     *
     * Demo1: 简化处理
     * - mock code → 固定测试用户
     * - 真实微信 code → 调用微信API换取openid
     */
    private String resolveWechatUid(String code) {
        if (code == null || code.isEmpty()) {
            throw new RuntimeException("登录code不能为空");
        }
        // 开发阶段：直接使用 code 作为 platformUid（后续替换为真实微信API）
        if (code.startsWith("mock_") || code.startsWith("dev_")) {
            return code;
        }
        // TODO Demo2+: 调用微信API换取 openid
        // String openid = wechatApi.code2Session(code);
        return "wx_" + code;
    }
}
