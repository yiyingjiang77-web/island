package com.fruitisland.game.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fruitisland.common.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Demo2.9 畜牧系统 HTTP 集成测试。
 *
 * 覆盖范围：
 *  - 牛棚配置加载、未解锁状态、解锁门槛（岛级/金币/首次奖励）
 *  - 牛棚逐级升级、跳级/金币/岛级不足拒绝
 *  - 牛奶惰性结算：599秒无产出、600秒结算、多周期离线、快照锁定
 *  - 鸡舍配置加载、解锁门槛、逐级升级
 *  - 鸡蛋惰性结算：周期变化（Lv4→570s）、额外鸡蛋（Lv7+）
 *  - 牛棚鸡舍互相独立
 *  - 玩家隔离、幂等性
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LivestockHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final long USER_A = 9101L;
    private static final long USER_B = 9102L;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM player_coop");
        jdbcTemplate.update("DELETE FROM player_barn");
        jdbcTemplate.update("DELETE FROM inventory WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (?, ?))", USER_A, USER_B);
        jdbcTemplate.update("DELETE FROM player_island_level_reward_claim");
        jdbcTemplate.update("DELETE FROM player_recipe");
        jdbcTemplate.update("DELETE FROM player_crop");
        jdbcTemplate.update("DELETE FROM island");
        jdbcTemplate.update("DELETE FROM game_player WHERE user_id IN (?, ?)", USER_A, USER_B);
    }

    private String initPlayer(long userId, int level, long gold) throws Exception {
        String token = jwtUtils.generateToken(userId, "livestock-test");
        // /game/init creates the player
        mockMvc.perform(get("/game/init").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        // Set island level and gold
        jdbcTemplate.update("UPDATE game_player SET level = ?, gold = ? WHERE user_id = ?", level, gold, userId);
        return token;
    }

    private Long getPlayerId(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = ?", Long.class, userId);
    }

    private int getInventoryCount(Long playerId, String itemId) {
        Integer count = jdbcTemplate.query(
                "SELECT count FROM inventory WHERE player_id = ? AND item_id = ?",
                (rs, rowNum) -> rs.getInt("count"), playerId, itemId).stream().findFirst().orElse(0);
        return count;
    }

    // ========================================================================
    // 配置加载与未解锁状态
    // ========================================================================

    @Test
    void statusReturnsUnlockedStateForNewPlayer() throws Exception {
        String token = initPlayer(USER_A, 1, 0);

        var result = mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.barn.unlocked").value(false))
                .andExpect(jsonPath("$.data.barn.level").value(0))
                .andExpect(jsonPath("$.data.coop.unlocked").value(false))
                .andExpect(jsonPath("$.data.coop.level").value(0))
                .andExpect(jsonPath("$.data.barn.canUnlock").value(false))
                .andExpect(jsonPath("$.data.coop.canUnlock").value(false))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertTrue(data.path("barn").has("allLevels"), "barn should have allLevels");
        assertEquals(10, data.path("barn").path("allLevels").size(), "barn should have 10 levels");
        assertEquals(10, data.path("coop").path("allLevels").size(), "coop should have 10 levels");
    }

    @Test
    void statusShowsCanUnlockAtLevel5ForBarnAndLevel8ForCoop() throws Exception {
        String token = initPlayer(USER_A, 5, 5000);

        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.barn.canUnlock").value(true))
                .andExpect(jsonPath("$.data.coop.canUnlock").value(false));
    }

    // ========================================================================
    // 牛棚解锁
    // ========================================================================

    @Test
    void barnUnlockFailsBelowLevel5() throws Exception {
        String token = initPlayer(USER_A, 4, 10000);

        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("岛屿等级不足")));
    }

    @Test
    void barnUnlockFailsWithInsufficientGold() throws Exception {
        String token = initPlayer(USER_A, 5, 500);

        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("金币不足")));
    }

    @Test
    void barnUnlockSucceedsAndGrantsCowAndMilk() throws Exception {
        String token = initPlayer(USER_A, 5, 5000);
        Long playerId = getPlayerId(USER_A);

        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(1))
                .andExpect(jsonPath("$.data.cowCount").value(1));

        long gold = jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        assertEquals(4000, gold, "Should deduct 1000 gold");

        assertEquals(10, getInventoryCount(playerId, "milk"), "Should have 10 milk in inventory");
    }

    @Test
    void barnUnlockIsIdempotent() throws Exception {
        String token = initPlayer(USER_A, 5, 5000);

        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Second unlock should fail
        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("已解锁")));

        long gold = jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        assertEquals(4000, gold, "Should only deduct once");
    }

    // ========================================================================
    // 牛棚升级
    // ========================================================================

    @Test
    void barnUpgradeFailsWhenNotUnlocked() throws Exception {
        String token = initPlayer(USER_A, 5, 50000);

        mockMvc.perform(post("/livestock/barn/upgrade")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("请先解锁")));
    }

    @Test
    void barnUpgradeFailsWithInsufficientGold() throws Exception {
        String token = initPlayer(USER_A, 6, 1000);
        // Unlock first
        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));
        // Now only 0 gold, need 2000 for Lv2
        mockMvc.perform(post("/livestock/barn/upgrade")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("金币不足")));
    }

    @Test
    void barnUpgradeFailsWithInsufficientIslandLevel() throws Exception {
        String token = initPlayer(USER_A, 5, 100000);
        // Unlock at Lv5 (costs 1000)
        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));
        // Try to upgrade to Lv2 which requires island level 6
        mockMvc.perform(post("/livestock/barn/upgrade")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("岛屿等级不足")));
    }

    @Test
    void barnUpgradeSucceedsLevelByLevel() throws Exception {
        String token = initPlayer(USER_A, 7, 100000);
        // Unlock (Lv5 requirement, costs 1000)
        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(1))
                .andExpect(jsonPath("$.data.cowCount").value(1));

        // Upgrade to Lv2 (Lv6 req, 2000 gold, +1 cow)
        jdbcTemplate.update("UPDATE game_player SET level = 6 WHERE user_id = ?", USER_A);
        mockMvc.perform(post("/livestock/barn/upgrade")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(2))
                .andExpect(jsonPath("$.data.cowCount").value(2));

        // Upgrade to Lv3 (Lv7 req, 3500 gold, +1 cow)
        jdbcTemplate.update("UPDATE game_player SET level = 7 WHERE user_id = ?", USER_A);
        mockMvc.perform(post("/livestock/barn/upgrade")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(3))
                .andExpect(jsonPath("$.data.cowCount").value(3));
    }

    // ========================================================================
    // 牛奶惰性结算
    // ========================================================================

    @Test
    void milkNotProducedBefore600Seconds() throws Exception {
        String token = initPlayer(USER_A, 5, 5000);
        Long playerId = getPlayerId(USER_A);

        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Set cycle start to 599 seconds ago
        jdbcTemplate.update("UPDATE player_barn SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(599), playerId);

        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        assertEquals(10, getInventoryCount(playerId, "milk"),
                "Should still have only the initial 10 milk");
    }

    @Test
    void milkProducedAfter600Seconds() throws Exception {
        String token = initPlayer(USER_A, 5, 5000);
        Long playerId = getPlayerId(USER_A);

        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Set cycle start to 600 seconds ago
        jdbcTemplate.update("UPDATE player_barn SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(600), playerId);

        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Lv1: 1 cow × 10 milk/cow = 10 milk per cycle
        assertEquals(20, getInventoryCount(playerId, "milk"),
                "Should have 10 (initial) + 10 (1 cycle) = 20 milk");
    }

    @Test
    void milkProducedForMultipleOfflineCycles() throws Exception {
        String token = initPlayer(USER_A, 5, 5000);
        Long playerId = getPlayerId(USER_A);

        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Set cycle start to 3 cycles ago (1800 seconds)
        jdbcTemplate.update("UPDATE player_barn SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(1800), playerId);

        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // 10 (initial) + 3 × 10 (3 cycles) = 40
        assertEquals(40, getInventoryCount(playerId, "milk"),
                "Should have 10 initial + 30 from 3 cycles = 40 milk");
    }

    @Test
    void milkSettleBeforeUpgradeDoesNotLoseProduction() throws Exception {
        String token = initPlayer(USER_A, 6, 100000);
        Long playerId = getPlayerId(USER_A);

        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Set cycle start to 600 seconds ago (1 complete cycle)
        jdbcTemplate.update("UPDATE player_barn SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(600), playerId);

        // Upgrade should settle first, then upgrade
        jdbcTemplate.update("UPDATE game_player SET level = 6 WHERE user_id = ?", USER_A);
        mockMvc.perform(post("/livestock/barn/upgrade")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(2));

        // 10 (initial) + 10 (1 cycle with Lv1 snapshot) = 20
        assertEquals(20, getInventoryCount(playerId, "milk"),
                "Should settle 1 cycle before upgrading");
    }

    @Test
    void upgradeMidCycleKeepsOldSnapshot() throws Exception {
        String token = initPlayer(USER_A, 6, 100000);
        Long playerId = getPlayerId(USER_A);

        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Wait 300 seconds (half a cycle), then upgrade
        jdbcTemplate.update("UPDATE player_barn SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(300), playerId);

        jdbcTemplate.update("UPDATE game_player SET level = 6 WHERE user_id = ?", USER_A);
        mockMvc.perform(post("/livestock/barn/upgrade")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(2))
                .andExpect(jsonPath("$.data.cowCount").value(2));

        // Now set cycle_start to 600s ago — old cycle (600s) completes with OLD snapshot
        jdbcTemplate.update("UPDATE player_barn SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(600), playerId);

        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // The cycle that just completed used OLD snapshot (1 cow × 10 = 10 milk)
        // 10 (initial) + 10 (1 cycle with Lv1 snapshot) = 20
        assertEquals(20, getInventoryCount(playerId, "milk"),
                "Current cycle should use old snapshot (1 cow × 10 = 10 milk)");
    }

    // ========================================================================
    // 鸡舍解锁
    // ========================================================================

    @Test
    void coopUnlockFailsBelowLevel8() throws Exception {
        String token = initPlayer(USER_A, 7, 10000);

        mockMvc.perform(post("/livestock/coop/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("岛屿等级不足")));
    }

    @Test
    void coopUnlockSucceedsAndGrantsChickenAndEggs() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);
        Long playerId = getPlayerId(USER_A);

        mockMvc.perform(post("/livestock/coop/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(1))
                .andExpect(jsonPath("$.data.chickenCount").value(1));

        long gold = jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        assertEquals(7000, gold, "Should deduct 3000 gold");

        assertEquals(5, getInventoryCount(playerId, "egg"), "Should have 5 eggs in inventory");
    }

    @Test
    void coopUnlockIsIdempotent() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);

        mockMvc.perform(post("/livestock/coop/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/livestock/coop/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("已解锁")));

        long gold = jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        assertEquals(7000, gold, "Should only deduct once");
    }

    // ========================================================================
    // 鸡蛋惰性结算
    // ========================================================================

    @Test
    void eggNotProducedBefore600Seconds() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);
        Long playerId = getPlayerId(USER_A);

        mockMvc.perform(post("/livestock/coop/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        jdbcTemplate.update("UPDATE player_coop SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(599), playerId);

        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        assertEquals(5, getInventoryCount(playerId, "egg"),
                "Should still have only the initial 5 eggs");
    }

    @Test
    void eggProducedAfter600Seconds() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);
        Long playerId = getPlayerId(USER_A);

        mockMvc.perform(post("/livestock/coop/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        jdbcTemplate.update("UPDATE player_coop SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(600), playerId);

        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Lv1: 1 chicken + 0 bonus = 1 egg per cycle
        assertEquals(6, getInventoryCount(playerId, "egg"),
                "Should have 5 (initial) + 1 (1 cycle) = 6 eggs");
    }

    @Test
    void eggBonusEggsAtHigherLevel() throws Exception {
        String token = initPlayer(USER_A, 14, 500000);
        Long playerId = getPlayerId(USER_A);

        // Unlock coop
        mockMvc.perform(post("/livestock/coop/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Upgrade to Lv7 (has bonus eggs +1)
        for (int targetLevel = 2; targetLevel <= 7; targetLevel++) {
            jdbcTemplate.update("UPDATE game_player SET level = ? WHERE user_id = ?", targetLevel + 7, USER_A);
            mockMvc.perform(post("/livestock/coop/upgrade")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.level").value(targetLevel));
        }

        // Verify Lv7: 5 chickens + 1 bonus = 6 eggs per cycle
        // Clear inventory to make counting easier
        jdbcTemplate.update("DELETE FROM inventory WHERE player_id = ? AND item_id = 'egg'", playerId);

        // First complete the initial 600s cycle to update snapshot to Lv7 config
        jdbcTemplate.update("UPDATE player_coop SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(600), playerId);
        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Clear inventory
        jdbcTemplate.update("DELETE FROM inventory WHERE player_id = ? AND item_id = 'egg'", playerId);

        // Now complete the next cycle — Lv7 uses 570s, snapshot: 5 chickens + 1 bonus = 6 eggs
        jdbcTemplate.update("UPDATE player_coop SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(570), playerId);

        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        assertEquals(6, getInventoryCount(playerId, "egg"),
                "Lv7: 5 chickens + 1 bonus = 6 eggs per cycle");
    }

    @Test
    void eggCycleChangesTo570SecondsAtLevel4() throws Exception {
        String token = initPlayer(USER_A, 11, 500000);
        Long playerId = getPlayerId(USER_A);

        // Unlock and upgrade to Lv4
        mockMvc.perform(post("/livestock/coop/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));
        for (int targetLevel = 2; targetLevel <= 4; targetLevel++) {
            jdbcTemplate.update("UPDATE game_player SET level = ? WHERE user_id = ?", targetLevel + 7, USER_A);
            mockMvc.perform(post("/livestock/coop/upgrade")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
        }

        // First complete the initial 600s cycle to update snapshot to Lv4 config (570s)
        jdbcTemplate.update("UPDATE player_coop SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(600), playerId);
        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Clear inventory
        jdbcTemplate.update("DELETE FROM inventory WHERE player_id = ? AND item_id = 'egg'", playerId);

        // Now set cycle to 570 seconds ago — with updated 570s snapshot, should complete
        jdbcTemplate.update("UPDATE player_coop SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(570), playerId);

        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Lv4: 3 chickens + 0 bonus = 3 eggs
        assertEquals(3, getInventoryCount(playerId, "egg"),
                "Lv4 with 570s cycle should produce 3 eggs after 570 seconds");
    }

    // ========================================================================
    // 牛棚鸡舍独立性
    // ========================================================================

    @Test
    void barnAndCoopAreIndependent() throws Exception {
        String token = initPlayer(USER_A, 8, 50000);
        Long playerId = getPlayerId(USER_A);

        // Unlock barn
        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Set barn cycle to 600 seconds ago
        jdbcTemplate.update("UPDATE player_barn SET cycle_start_time = ? WHERE player_id = ?",
                LocalDateTime.now().minusSeconds(600), playerId);

        // Unlock coop (should not affect barn)
        mockMvc.perform(post("/livestock/coop/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Check status - barn should have settled, coop just started
        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Barn: 10 (initial) + 10 (1 cycle) = 20 milk
        assertEquals(20, getInventoryCount(playerId, "milk"),
                "Barn should have produced 1 cycle of milk");
        // Coop: 5 (initial) + 0 (just started) = 5 eggs
        assertEquals(5, getInventoryCount(playerId, "egg"),
                "Coop should not have produced any eggs yet");
    }

    // ========================================================================
    // 玩家隔离
    // ========================================================================

    @Test
    void playerIsolation() throws Exception {
        String tokenA = initPlayer(USER_A, 5, 5000);
        String tokenB = initPlayer(USER_B, 5, 5000);

        // Player A unlocks barn
        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.code").value(0));

        // Player B checks status - should be unlocked=false
        mockMvc.perform(get("/livestock/status")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.barn.unlocked").value(false))
                .andExpect(jsonPath("$.data.barn.level").value(0));

        // Player B's gold should be unchanged
        long goldB = jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_B);
        assertEquals(5000, goldB, "Player B gold should be unchanged");
    }

    // ========================================================================
    // 重复升级幂等
    // ========================================================================

    @Test
    void barnUpgradeDoesNotSkipLevels() throws Exception {
        String token = initPlayer(USER_A, 14, 500000);

        // Unlock
        mockMvc.perform(post("/livestock/barn/unlock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(1));

        // Upgrade to Lv2
        mockMvc.perform(post("/livestock/barn/upgrade")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(2));

        // Try to upgrade again — should go to Lv3, not Lv4
        jdbcTemplate.update("UPDATE game_player SET level = 7 WHERE user_id = ?", USER_A);
        mockMvc.perform(post("/livestock/barn/upgrade")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(3));
    }
}
