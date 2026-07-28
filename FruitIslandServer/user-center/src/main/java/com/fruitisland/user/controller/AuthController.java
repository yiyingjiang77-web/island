package com.fruitisland.user.controller;

import com.fruitisland.common.result.Result;
import com.fruitisland.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证 Controller
 *
 * POST /auth/wechat/login  — 微信登录
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 微信登录
     *
     * 请求: {"code": "wx_xxx"}
     * 响应: {"code": 0, "message": "success", "data": {"token": "...", "userId": 1, "nickname": "岛主"}}
     */
    @PostMapping("/wechat/login")
    public Result<Map<String, Object>> wechatLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        log.info("微信登录请求: code={}", code);

        Map<String, Object> result = authService.wechatLogin(code);
        return Result.ok(result);
    }
}
