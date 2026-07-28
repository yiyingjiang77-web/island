package com.fruitisland.common.config;

import com.fruitisland.common.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置
 *
 * 注册 JWT 拦截器，保护 /game/** 路径
 * /auth/** 路径不拦截（登录接口）
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/game/**")          // 游戏接口需要认证
                .excludePathPatterns("/auth/**");      // 登录接口不需要认证
    }
}
