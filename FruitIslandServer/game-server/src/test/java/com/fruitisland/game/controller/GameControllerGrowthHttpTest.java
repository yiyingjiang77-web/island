package com.fruitisland.game.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fruitisland.common.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(DrinkBarControllerHttpTest.ClockTestConfig.class)
class GameControllerGrowthHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanPlayers() {
        jdbcTemplate.update("DELETE FROM player_island_level_reward_claim");
        jdbcTemplate.update("DELETE FROM player_recipe");
        jdbcTemplate.update("DELETE FROM player_land");
        jdbcTemplate.update("DELETE FROM island");
        jdbcTemplate.update("DELETE FROM player_crop");
        jdbcTemplate.update("DELETE FROM inventory");
        jdbcTemplate.update("DELETE FROM game_player WHERE user_id IN (7201, 7202, 7203)");
    }

    @Test
    void existingPlayerKeepsProgressAndReceivesEveryMissingRewardThroughCurrentLevel() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7202, 'fruit_island', '老岛主', 5, 30, NULL, 777, 20)
                """);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7202", Long.class);
        jdbcTemplate.update("""
                INSERT INTO player_crop
                (player_id, crop_id, crop_level, unlock_source, unlock_time)
                VALUES (?, 'tomato', 1, 'GOLD_SHOP', CURRENT_TIMESTAMP)
                """, playerId);
        String token = jwtUtils.generateToken(7202L, "existing-growth-player");

        mockMvc.perform(get("/game/init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.player.level").value(5))
                .andExpect(jsonPath("$.data.player.gold").value(777))
                .andExpect(jsonPath("$.data.islandGrowth.currentLevel").value(5))
                .andExpect(jsonPath("$.data.islandGrowth.cumulativeExp").value(800))
                .andExpect(jsonPath("$.data.islandGrowth.nextLevelThreshold").value(1000))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[0:5].claimed",
                        contains(true, true, true, true, true)))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[5].claimed").value(false))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[4].recipeId")
                        .value("milk_ice_cream"))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[4].materialSourceHint")
                        .value("解锁牛棚后可获得牛奶材料"))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[4].shopCapabilityHint")
                        .value("饮品店达到5级后可制作冰淇淋"))
                .andExpect(jsonPath("$.data.playerCrops.length()").value(5));

        mockMvc.perform(get("/game/init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.islandGrowth.cumulativeExp").value(800))
                .andExpect(jsonPath("$.data.playerCrops.length()").value(5))
                .andExpect(jsonPath("$.data.playerCrops[*].cropId",
                        containsInAnyOrder("strawberry", "carrot", "orange", "tomato", "blueberry")))
                .andExpect(jsonPath("$.data.playerCrops[?(@.cropId == 'tomato')].unlockSource",
                        contains("GOLD_SHOP")));
    }

    @Test
    void jwtInitializationReturnsTheFixedTenLevelConfigurationOnlyForItsPlayer() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7202, 'fruit_island', '三级岛主', 3, 0, 250, 500, 20)
                """);
        String levelThreeToken = jwtUtils.generateToken(7202L, "level-three-player");
        String levelOneToken = jwtUtils.generateToken(7203L, "level-one-player");

        mockMvc.perform(get("/game/init")
                        .header("Authorization", "Bearer " + levelThreeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.islandGrowth.rewards[*].cumulativeExp",
                        contains(0, 100, 250, 450, 700, 1000, 1400, 1900, 2500, 3200)))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[*].cropId",
                        contains("strawberry", "carrot", "orange", "tomato", "blueberry",
                                "apple", "watermelon", "wheat", "lemon", "cucumber")))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[*].recipeId",
                        contains("strawberry_juice", "carrot_juice", "orange_juice",
                                "tomato_juice", "milk_ice_cream", "apple_carrot_juice",
                                "watermelon_milk_ice_cream", "strawberry_cake",
                                "lemon_milk_ice_cream", "cucumber_apple_juice")))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[0:3].claimed",
                        contains(true, true, true)))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[3].claimed").value(false));

        mockMvc.perform(get("/game/init")
                        .queryParam("userId", "7202")
                        .header("Authorization", "Bearer " + levelOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.player.userId").value(7203))
                .andExpect(jsonPath("$.data.islandGrowth.currentLevel").value(1))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[0].claimed").value(true))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[1].claimed").value(false))
                .andExpect(jsonPath("$.data.playerCrops.length()").value(1));
    }

    @Test
    void concurrentInitializationBackfillsEachReachedRewardOnlyOnce() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7202, 'fruit_island', '并发岛主', 5, 0, 700, 500, 20)
                """);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7202", Long.class);
        jdbcTemplate.update("""
                INSERT INTO island (player_id, island_name, level)
                VALUES (?, '并发小岛', 1)
                """, playerId);
        String token = jwtUtils.generateToken(7202L, "concurrent-growth-player");
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                start.await();
                return mockMvc.perform(get("/game/init")
                                .header("Authorization", "Bearer " + token))
                        .andReturn().getResponse().getContentAsString();
            });
            var second = executor.submit(() -> {
                start.await();
                return mockMvc.perform(get("/game/init")
                                .header("Authorization", "Bearer " + token))
                        .andReturn().getResponse().getContentAsString();
            });
            start.countDown();
            List<Integer> codes = List.of(
                    objectMapper.readTree(first.get()).path("code").asInt(),
                    objectMapper.readTree(second.get()).path("code").asInt());
            assertEquals(List.of(0, 0), codes);
        } finally {
            executor.shutdownNow();
        }

        mockMvc.perform(get("/game/init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.playerCrops.length()").value(5))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[0:5].claimed",
                        contains(true, true, true, true, true)));
    }

    @Test
    void levelTenPlayerKeepsAllQualificationsEvenWhenFutureCapabilitiesAreMissing() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7202, 'fruit_island', '十级岛主', 10, 0, 3200, 500, 20)
                """);
        String token = jwtUtils.generateToken(7202L, "level-ten-player");

        mockMvc.perform(get("/game/init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.islandGrowth.currentLevel").value(10))
                .andExpect(jsonPath("$.data.islandGrowth.nextLevelThreshold").doesNotExist())
                .andExpect(jsonPath("$.data.islandGrowth.rewards[*].claimed",
                        contains(true, true, true, true, true, true, true, true, true, true)))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[7].recipeId")
                        .value("strawberry_cake"))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[7].materialSourceHint")
                        .value("解锁鸡舍后可获得鸡蛋材料"))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[7].shopCapabilityHint")
                        .value("解锁蛋糕店后可制作蛋糕"))
                .andExpect(jsonPath("$.data.playerCrops.length()").value(10));
    }

    @Test
    void jwtInitializationCreatesLevelOneGrowthAndClaimsItsFixedRewards() throws Exception {
        String token = jwtUtils.generateToken(7201L, "new-growth-player");

        mockMvc.perform(get("/game/init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.player.userId").value(7201))
                .andExpect(jsonPath("$.data.islandGrowth.cumulativeExp").value(0))
                .andExpect(jsonPath("$.data.islandGrowth.currentLevel").value(1))
                .andExpect(jsonPath("$.data.islandGrowth.nextLevelThreshold").value(100))
                .andExpect(jsonPath("$.data.islandGrowth.rewards.length()").value(10))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[0].level").value(1))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[0].cropId").value("strawberry"))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[0].recipeId").value("strawberry_juice"))
                .andExpect(jsonPath("$.data.islandGrowth.rewards[0].claimed").value(true))
                .andExpect(jsonPath("$.data.playerCrops.length()").value(1))
                .andExpect(jsonPath("$.data.playerCrops[0].cropId").value("strawberry"));
    }
}
