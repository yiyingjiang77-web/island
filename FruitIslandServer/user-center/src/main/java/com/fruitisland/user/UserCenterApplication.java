package com.fruitisland.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User Center Application
 */
@SpringBootApplication(scanBasePackages = "com.fruitisland")
@MapperScan("com.fruitisland.**.mapper")
public class UserCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserCenterApplication.class, args);
    }
}
