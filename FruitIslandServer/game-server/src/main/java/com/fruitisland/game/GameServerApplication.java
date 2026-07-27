package com.fruitisland.game;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Game Server Application
 */
@SpringBootApplication(scanBasePackages = "com.fruitisland")
@MapperScan("com.fruitisland.**.mapper")
public class GameServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameServerApplication.class, args);
    }
}
