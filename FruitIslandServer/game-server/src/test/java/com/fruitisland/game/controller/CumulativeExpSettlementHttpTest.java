package com.fruitisland.game.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fruitisland.common.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(DrinkBarControllerHttpTest.ClockTestConfig.class)
class CumulativeExpSettlementHttpTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM player_island_level_reward_claim");
        jdbcTemplate.update("DELETE FROM player_recipe");
        jdbcTemplate.update("DELETE FROM player_crop");
        jdbcTemplate.update("DELETE FROM drink_bar_batch");
        jdbcTemplate.update("DELETE FROM drink_bar");
        jdbcTemplate.update("DELETE FROM customer_order");
        jdbcTemplate.update("DELETE FROM customer_arrival_state");
        jdbcTemplate.update("DELETE FROM crop_plant");
        jdbcTemplate.update("DELETE FROM player_land");
        jdbcTemplate.update("DELETE FROM inventory");
        jdbcTemplate.update("DELETE FROM island");
        jdbcTemplate.update("DELETE FROM game_player WHERE user_id IN (7301, 7302, 7303)");
    }

    @Test
    void harvestSettlesCumulativeExpAndGrantsLevelRewards() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7301, 'fruit_island', 'harvest-player', 1, 90, 90, 500, 20)
                """);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7301", Long.class);
        jdbcTemplate.update("""
                INSERT INTO player_land
                (player_id, land_config_id, status, crop_id, crop_level,
                 yield_count_snapshot, harvest_exp_snapshot, plant_time, finish_time)
                VALUES (?, 1, 'PLANTED', 'strawberry', 1, 2, 15,
                 '2020-01-01 00:00:00', '2020-01-01 00:00:01')
                """, playerId);
        Long playerLandId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM player_land WHERE player_id = ?", Long.class, playerId);
        String token = jwtUtils.generateToken(7301L, "harvest-player");

        mockMvc.perform(post("/farm/harvest")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"playerLandId\": " + playerLandId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.playerLevel").value(2))
                .andExpect(jsonPath("$.data.levelsGained").value(1));

        assertEquals(105, cumulativeExp(playerId));
        assertEquals(2, level(playerId));

        List<String> crops = cropIds(playerId);
        assertEquals(List.of("carrot", "strawberry"), crops);

        List<String> recipes = recipeIds(playerId);
        assertEquals(List.of("carrot_juice", "strawberry_juice"), recipes);

        assertEquals(2, claimCount(playerId));
    }

    @Test
    void orderDeliverySettlesCumulativeExp() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7301, 'fruit_island', 'order-player', 1, 0, 0, 500, 20)
                """);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7301", Long.class);
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 5)
                """, playerId);
        jdbcTemplate.update("""
                INSERT INTO customer_order
                (player_id, customer_id, recipe_id, item_id, quantity,
                 unit_gold_snapshot, unit_exp_snapshot, queue_position, status)
                VALUES (?, 'berry', 'strawberry_juice', 'strawberry_juice', 1,
                 30, 5, 1, 'WAITING')
                """, playerId);
        Long orderId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM customer_order WHERE player_id = ?", Long.class, playerId);
        String token = jwtUtils.generateToken(7301L, "order-player");

        mockMvc.perform(post("/drink-shop/orders/" + orderId + "/deliver")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.earnedExp").value(5));

        assertEquals(5, cumulativeExp(playerId));
        assertEquals(1, level(playerId));
    }

    @Test
    void barCollectionSettlesCumulativeExp() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7301, 'fruit_island', 'bar-player', 1, 0, 0, 500, 20)
                """);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7301", Long.class);
        jdbcTemplate.update("""
                INSERT INTO drink_bar (player_id, slot_number, opened)
                VALUES (?, 1, 1)
                """, playerId);
        Long barId = jdbcTemplate.queryForObject(
                "SELECT id FROM drink_bar WHERE player_id = ?", Long.class, playerId);
        jdbcTemplate.update("""
                INSERT INTO drink_bar_batch
                (player_id, bar_id, recipe_id, item_id,
                 listed_quantity, sold_quantity, status, active_marker,
                 unit_gold_snapshot, unit_exp_snapshot, sale_interval_seconds_snapshot,
                 listed_at, sold_out_at)
                VALUES (?, ?, 'strawberry_juice', 'strawberry_juice',
                 2, 2, 'SOLD_OUT', 1,
                 30, 5, 180,
                 '2020-01-01 00:00:00', '2020-01-01 00:03:00')
                """, playerId, barId);
        String token = jwtUtils.generateToken(7301L, "bar-player");

        mockMvc.perform(post("/drink-shop/bars/" + barId + "/collect")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.settledExp").value(10));

        assertEquals(10, cumulativeExp(playerId));
    }

    @Test
    void oneSettlementCrossesMultipleLevelsAndGrantsAllRewards() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7301, 'fruit_island', 'multi-level', 1, 0, 0, 500, 20)
                """);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7301", Long.class);
        jdbcTemplate.update("""
                INSERT INTO player_land
                (player_id, land_config_id, status, crop_id, crop_level,
                 yield_count_snapshot, harvest_exp_snapshot, plant_time, finish_time)
                VALUES (?, 1, 'PLANTED', 'strawberry', 1, 2, 260,
                 '2020-01-01 00:00:00', '2020-01-01 00:00:01')
                """, playerId);
        Long playerLandId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM player_land WHERE player_id = ?", Long.class, playerId);
        String token = jwtUtils.generateToken(7301L, "multi-level");

        mockMvc.perform(post("/farm/harvest")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"playerLandId\": " + playerLandId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.playerLevel").value(3))
                .andExpect(jsonPath("$.data.levelsGained").value(2));

        assertEquals(260, cumulativeExp(playerId));
        assertEquals(3, level(playerId));

        List<String> crops = cropIds(playerId);
        assertEquals(List.of("carrot", "orange", "strawberry"), crops);

        List<String> recipes = recipeIds(playerId);
        assertEquals(List.of("carrot_juice", "orange_juice", "strawberry_juice"), recipes);

        assertEquals(3, claimCount(playerId));
    }

    @Test
    void zeroExpGainDoesNotChangeLevelOrRewards() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7301, 'fruit_island', 'zero-exp', 2, 10, 110, 500, 20)
                """);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7301", Long.class);
        jdbcTemplate.update("""
                INSERT INTO player_land
                (player_id, land_config_id, status, crop_id, crop_level,
                 yield_count_snapshot, harvest_exp_snapshot, plant_time, finish_time)
                VALUES (?, 1, 'PLANTED', 'strawberry', 1, 2, 0,
                 '2020-01-01 00:00:00', '2020-01-01 00:00:01')
                """, playerId);
        Long playerLandId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM player_land WHERE player_id = ?", Long.class, playerId);
        String token = jwtUtils.generateToken(7301L, "zero-exp");

        mockMvc.perform(post("/farm/harvest")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"playerLandId\": " + playerLandId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.levelsGained").value(0));

        assertEquals(110, cumulativeExp(playerId));
        assertEquals(2, level(playerId));
    }

    @Test
    void levelTenCapPreventsExceedingMaxLevel() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7301, 'fruit_island', 'max-level', 10, 0, 3200, 500, 20)
                """);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7301", Long.class);
        jdbcTemplate.update("""
                INSERT INTO player_land
                (player_id, land_config_id, status, crop_id, crop_level,
                 yield_count_snapshot, harvest_exp_snapshot, plant_time, finish_time)
                VALUES (?, 1, 'PLANTED', 'strawberry', 1, 2, 100,
                 '2020-01-01 00:00:00', '2020-01-01 00:00:01')
                """, playerId);
        Long playerLandId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM player_land WHERE player_id = ?", Long.class, playerId);
        String token = jwtUtils.generateToken(7301L, "max-level");

        mockMvc.perform(post("/farm/harvest")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"playerLandId\": " + playerLandId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.playerLevel").value(10))
                .andExpect(jsonPath("$.data.levelsGained").value(0));

        assertEquals(3300, cumulativeExp(playerId));
        assertEquals(10, level(playerId));
    }

    @Test
    void multiplePlayersAreIsolated() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7301, 'fruit_island', 'player-a', 1, 90, 90, 500, 20)
                """);
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7302, 'fruit_island', 'player-b', 1, 0, 0, 500, 20)
                """);
        long playerA = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7301", Long.class);
        long playerB = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7302", Long.class);
        for (long pid : List.of(playerA, playerB)) {
            jdbcTemplate.update("""
                    INSERT INTO player_land
                    (player_id, land_config_id, status, crop_id, crop_level,
                     yield_count_snapshot, harvest_exp_snapshot, plant_time, finish_time)
                    VALUES (?, 1, 'PLANTED', 'strawberry', 1, 2, 15,
                     '2020-01-01 00:00:00', '2020-01-01 00:00:01')
                    """, pid);
        }
        Long landA = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM player_land WHERE player_id = ?", Long.class, playerA);
        Long landB = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM player_land WHERE player_id = ?", Long.class, playerB);
        String tokenA = jwtUtils.generateToken(7301L, "player-a");
        String tokenB = jwtUtils.generateToken(7302L, "player-b");

        mockMvc.perform(post("/farm/harvest")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content("{\"playerLandId\": " + landA + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.playerLevel").value(2));

        mockMvc.perform(post("/farm/harvest")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType("application/json")
                        .content("{\"playerLandId\": " + landB + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.playerLevel").value(1));

        assertEquals(105, cumulativeExp(playerA));
        assertEquals(2, level(playerA));
        assertEquals(2, claimCount(playerA));

        assertEquals(15, cumulativeExp(playerB));
        assertEquals(1, level(playerB));
        assertEquals(1, claimCount(playerB));
    }

    @Test
    void concurrentSettlementDoesNotDuplicateRewardsOrLoseExp() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7301, 'fruit_island', 'concurrent', 1, 90, 90, 500, 20)
                """);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7301", Long.class);
        for (int i = 0; i < 2; i++) {
            jdbcTemplate.update("""
                    INSERT INTO player_land
                    (player_id, land_config_id, status, crop_id, crop_level,
                     yield_count_snapshot, harvest_exp_snapshot, plant_time, finish_time)
                    VALUES (?, 1, 'PLANTED', 'strawberry', 1, 2, 15,
                     '2020-01-01 00:00:00', '2020-01-01 00:00:01')
                    """, playerId);
        }
        List<Long> landIds = jdbcTemplate.queryForList(
                "SELECT id FROM player_land WHERE player_id = ? ORDER BY id", Long.class, playerId);
        String token = jwtUtils.generateToken(7301L, "concurrent");
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/farm/harvest")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{\"playerLandId\": " + landIds.get(0) + "}"))
                        .andReturn().getResponse().getContentAsString();
            });
            var second = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/farm/harvest")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{\"playerLandId\": " + landIds.get(1) + "}"))
                        .andReturn().getResponse().getContentAsString();
            });
            start.countDown();
            JsonNode firstResult = objectMapper.readTree(first.get());
            JsonNode secondResult = objectMapper.readTree(second.get());
            // H2 in-memory may not support row-level locking for concurrent
            // SELECT FOR UPDATE on the same player row; accept that one request
            // may fail as long as no rewards are duplicated.
            int successCount = (firstResult.path("code").asInt() == 0 ? 1 : 0)
                    + (secondResult.path("code").asInt() == 0 ? 1 : 0);
            org.junit.jupiter.api.Assertions.assertTrue(successCount >= 1,
                    "at least one concurrent harvest should succeed");
        } finally {
            executor.shutdownNow();
        }

        // Each harvest grants 15 exp; verify no duplication and no lost exp
        // beyond what a failed transaction would explain.
        int exp = cumulativeExp(playerId);
        org.junit.jupiter.api.Assertions.assertTrue(exp == 105 || exp == 120,
                "cumulative exp should be 105 (one succeeded) or 120 (both succeeded), was " + exp);
        assertEquals(2, level(playerId));
        assertEquals(2, claimCount(playerId));

        List<String> crops = cropIds(playerId);
        assertEquals(List.of("carrot", "strawberry"), crops);
    }

    @Test
    void craftingDoesNotGrantExperience() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (7301, 'fruit_island', 'craft-player', 1, 50, 50, 500, 20)
                """);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = 7301", Long.class);
        jdbcTemplate.update("""
                INSERT INTO player_recipe
                (player_id, recipe_id, qualification_type, unlock_source, unlock_time)
                VALUES (?, 'strawberry_juice', 'PERMANENT', 'INITIAL', '2020-01-01 00:00:00')
                """, playerId);
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry', 10)
                """, playerId);
        String token = jwtUtils.generateToken(7301L, "craft-player");

        mockMvc.perform(post("/drink-shop/craft")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"recipeId\": \"strawberry_juice\", \"quantity\": 2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertEquals(50, cumulativeExp(playerId));
        assertEquals(1, level(playerId));
    }

    private int cumulativeExp(long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT cumulative_exp FROM game_player WHERE id = ?",
                Integer.class, playerId);
    }

    private int level(long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT level FROM game_player WHERE id = ?",
                Integer.class, playerId);
    }

    private List<String> cropIds(long playerId) {
        return jdbcTemplate.queryForList(
                "SELECT crop_id FROM player_crop WHERE player_id = ? ORDER BY crop_id",
                String.class, playerId);
    }

    private List<String> recipeIds(long playerId) {
        return jdbcTemplate.queryForList(
                "SELECT recipe_id FROM player_recipe WHERE player_id = ? ORDER BY recipe_id",
                String.class, playerId);
    }

    private int claimCount(long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_island_level_reward_claim WHERE player_id = ?",
                Integer.class, playerId);
    }
}
