package com.fruitisland.game.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.random.RandomGenerator;

@Configuration
public class DrinkShopRuntimeConfig {
    @Bean
    public Clock drinkShopClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public RandomGenerator drinkShopRandomGenerator() {
        return RandomGenerator.getDefault();
    }
}
