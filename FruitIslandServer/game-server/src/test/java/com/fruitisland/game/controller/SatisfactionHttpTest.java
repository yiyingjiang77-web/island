package com.fruitisland.game.controller;

import com.fruitisland.common.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SatisfactionHttpTest.ClockTestConfig.class)
class SatisfactionHttpTest {
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.set(Instant.parse("2026-08-05T04:00:00Z"));
        jdbcTemplate.update("DELETE FROM daily_satisfaction WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (7501, 7502))");
        jdbcTemplate.update("DELETE FROM customer_order WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (7501, 7502))");
        jdbcTemplate.update("DELETE FROM customer_arrival_state WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (7501, 7502))");
        jdbcTemplate.update("DELETE FROM inventory WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (7501, 7502))");
        jdbcTemplate.update("DELETE FROM player_drink_shop WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (7501, 7502))");
        jdbcTemplate.update("DELETE FROM game_player WHERE user_id IN (7501, 7502)");
        jdbcTemplate.update("DELETE FROM satisfaction_gift_config WHERE config_version > 1");
        insertPlayer(7501, "满意度玩家");
        insertPlayer(7502, "隔离玩家");
    }

    @Test
    void todayUsesOnlyClosedOrdersFromTheJwtPlayer() throws Exception {
        LocalDate today = LocalDate.of(2026, 8, 5);
        long owner = playerId(7501);
        long other = playerId(7502);
        insertClosed(owner, today, 3, 1, 7);
        insertWaiting(owner, 9);
        insertClosed(other, today, 1, 0, 20);

        mockMvc.perform(get("/drink-shop/satisfaction").header("Authorization", bearer(7501)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.today.businessDate").value("2026-08-05"))
                .andExpect(jsonPath("$.data.today.deliveredOrders").value(3))
                .andExpect(jsonPath("$.data.today.rejectedOrders").value(1))
                .andExpect(jsonPath("$.data.today.closedOrders").value(4))
                .andExpect(jsonPath("$.data.today.deliveredQuantity").value(21))
                .andExpect(jsonPath("$.data.today.satisfactionPercent").value(75))
                .andExpect(jsonPath("$.data.today.expectedTier").value("S70"))
                .andExpect(jsonPath("$.data.today.expectedGold").value(200))
                .andExpect(jsonPath("$.data.today.quantityNeeded").value(0))
                .andExpect(jsonPath("$.data.today.nextTier").value("S80"))
                .andExpect(jsonPath("$.data.today.nextTierPercentNeeded").value(5))
                .andExpect(jsonPath("$.data.giftRules.length()").value(5))
                .andExpect(jsonPath("$.data.giftRules[0].tierCode").value("S60"))
                .andExpect(jsonPath("$.data.giftRules[0].minimumPercent").value(60))
                .andExpect(jsonPath("$.data.giftRules[0].minimumDeliveredQuantity").value(20))
                .andExpect(jsonPath("$.data.giftRules[0].rewardGold").value(100))
                .andExpect(jsonPath("$.data.recentHistory.length()").value(0));

        mockMvc.perform(get("/drink-shop/satisfaction").header("Authorization", bearer(7502)))
                .andExpect(jsonPath("$.data.today.deliveredOrders").value(1))
                .andExpect(jsonPath("$.data.today.deliveredQuantity").value(20))
                .andExpect(jsonPath("$.data.today.satisfactionPercent").value(100));
    }

    @Test
    void fulfillmentEndpointsRecordTheActualBeijingCloseDate() throws Exception {
        long player = playerId(7501);
        jdbcTemplate.update("INSERT INTO inventory(player_id,item_id,count) VALUES (?,'strawberry_juice',10)", player);
        jdbcTemplate.update("INSERT INTO inventory(player_id,item_id,count) VALUES (?,'strawberry',22)", player);
        long inventoryOrder = insertWaiting(player, 10);
        long madeOrder = insertWaiting(player, 11);
        long rejectedOrder = insertWaiting(player, 1);
        clock.set(Instant.parse("2026-08-04T15:59:00Z"));

        mockMvc.perform(post("/drink-shop/orders/{id}/deliver", inventoryOrder)
                        .header("Authorization", bearer(7501)))
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        clock.set(Instant.parse("2026-08-04T16:01:00Z"));
        mockMvc.perform(post("/drink-shop/orders/{id}/make-and-deliver", madeOrder)
                        .header("Authorization", bearer(7501))
                        .contentType("application/json").content("{\"quantity\":11}"))
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));
        mockMvc.perform(post("/drink-shop/orders/{id}/out-of-stock", rejectedOrder)
                        .header("Authorization", bearer(7501)))
                .andExpect(jsonPath("$.data.status").value("OUT_OF_STOCK"));

        assertEquals(LocalDateTime.of(2026, 8, 4, 23, 59), closeTime(inventoryOrder));
        assertEquals(LocalDateTime.of(2026, 8, 5, 0, 1), closeTime(madeOrder));
        mockMvc.perform(get("/drink-shop/satisfaction").header("Authorization", bearer(7501)))
                .andExpect(jsonPath("$.data.today.deliveredOrders").value(1))
                .andExpect(jsonPath("$.data.today.rejectedOrders").value(1))
                .andExpect(jsonPath("$.data.today.deliveredQuantity").value(11))
                .andExpect(jsonPath("$.data.today.satisfactionPercent").value(50))
                .andExpect(jsonPath("$.data.recentHistory[0].businessDate").value("2026-08-04"));
    }

    @Test
    void settlesAllPastDaysOnceAndKeepsRewardSnapshots() throws Exception {
        long player = playerId(7501);
        insertClosed(player, LocalDate.of(2026, 8, 4), 8, 2, 3);
        insertClosed(player, LocalDate.of(2026, 6, 26), 1, 0, 20);
        jdbcTemplate.update("""
                INSERT INTO satisfaction_gift_config
                (tier_code,minimum_percent,minimum_delivered_quantity,reward_gold,
                 config_version,effective_from,enabled)
                VALUES ('S80',80,20,333,2,'2026-08-04',1)
                """);

        mockMvc.perform(get("/drink-shop/satisfaction").header("Authorization", bearer(7501)))
                .andExpect(jsonPath("$.data.autoSettledRewards.length()").value(2))
                .andExpect(jsonPath("$.data.giftRules[2].tierCode").value("S80"))
                .andExpect(jsonPath("$.data.giftRules[2].rewardGold").value(333))
                .andExpect(jsonPath("$.data.recentHistory.length()").value(1))
                .andExpect(jsonPath("$.data.recentHistory[0].giftTier").value("S80"))
                .andExpect(jsonPath("$.data.recentHistory[0].rewardGold").value(333));
        assertEquals(1333L, gold(player));
        assertEquals(2, countDaily(player));

        jdbcTemplate.update("UPDATE satisfaction_gift_config SET reward_gold=999 WHERE tier_code='S80' AND config_version=2");
        mockMvc.perform(get("/drink-shop/satisfaction").header("Authorization", bearer(7501)))
                .andExpect(jsonPath("$.data.autoSettledRewards.length()").value(0))
                .andExpect(jsonPath("$.data.recentHistory[0].rewardGold").value(333));
        assertEquals(1333L, gold(player));
        assertEquals(2, countDaily(player));
    }

    @Test
    void appliesAllSatisfactionTierBoundaries() throws Exception {
        long player = playerId(7501);
        List<Integer> percents = List.of(59, 60, 69, 70, 79, 80, 89, 90, 99, 100);
        List<String> tiers = java.util.Arrays.asList(null, "S60", "S60", "S70", "S70",
                "S80", "S80", "S90", "S90", "S100");
        List<Long> rewards = List.of(0L, 100L, 100L, 200L, 200L, 300L, 300L, 400L, 400L, 500L);
        for (int index = 0; index < percents.size(); index++) {
            jdbcTemplate.update("DELETE FROM daily_satisfaction WHERE player_id=?", player);
            jdbcTemplate.update("DELETE FROM customer_order WHERE player_id=?", player);
            jdbcTemplate.update("UPDATE game_player SET gold=500 WHERE id=?", player);
            int delivered = percents.get(index);
            insertClosed(player, LocalDate.of(2026, 8, 4), delivered, 100 - delivered, 1);

            mockMvc.perform(get("/drink-shop/satisfaction").header("Authorization", bearer(7501)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));

            assertEquals(percents.get(index), dailyPercent(player));
            assertEquals(tiers.get(index), dailyTier(player));
            assertEquals(rewards.get(index), dailyReward(player));
            assertEquals(500L + rewards.get(index), gold(player));
        }
    }

    @Test
    void requiresTwentyDeliveredItemsButStillShowsTheExpectedTier() throws Exception {
        long player = playerId(7501);
        insertClosed(player, LocalDate.of(2026, 8, 5), 1, 0, 19);
        mockMvc.perform(get("/drink-shop/satisfaction").header("Authorization", bearer(7501)))
                .andExpect(jsonPath("$.data.today.expectedTier").value("S100"))
                .andExpect(jsonPath("$.data.today.expectedGold").value(500))
                .andExpect(jsonPath("$.data.today.quantityNeeded").value(1))
                .andExpect(jsonPath("$.data.today.nextTier").doesNotExist())
                .andExpect(jsonPath("$.data.today.nextTierPercentNeeded").value(0));

        clock.set(Instant.parse("2026-08-06T04:00:00Z"));
        mockMvc.perform(get("/drink-shop/satisfaction").header("Authorization", bearer(7501)))
                .andExpect(jsonPath("$.data.recentHistory[0].rewardStatus").value("NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.data.recentHistory[0].rewardGold").value(0));
        assertEquals(500L, gold(player));

        jdbcTemplate.update("DELETE FROM daily_satisfaction WHERE player_id=?", player);
        jdbcTemplate.update("DELETE FROM customer_order WHERE player_id=?", player);
        insertClosed(player, LocalDate.of(2026, 8, 5), 1, 0, 20);
        mockMvc.perform(get("/drink-shop/satisfaction").header("Authorization", bearer(7501)))
                .andExpect(jsonPath("$.data.recentHistory[0].rewardStatus").value("GRANTED"))
                .andExpect(jsonPath("$.data.recentHistory[0].rewardGold").value(500));
        assertEquals(1000L, gold(player));
    }

    @Test
    void concurrentReadsGrantOneDailyRewardOnlyOnce() throws Exception {
        long player = playerId(7501);
        insertClosed(player, LocalDate.of(2026, 8, 4), 20, 0, 1);
        String authorization = bearer(7501);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                start.await();
                return mockMvc.perform(get("/drink-shop/satisfaction")
                                .header("Authorization", authorization))
                        .andReturn().getResponse().getContentAsString();
            });
            var second = executor.submit(() -> {
                start.await();
                return mockMvc.perform(get("/drink-shop/satisfaction")
                                .header("Authorization", authorization))
                        .andReturn().getResponse().getContentAsString();
            });
            start.countDown();
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, countDaily(player));
        assertEquals(1000L, gold(player));
    }

    @Test
    void gameLoginReturnsTheAutomaticallyGrantedGiftNotification() throws Exception {
        long player = playerId(7501);
        insertClosed(player, LocalDate.of(2026, 8, 4), 20, 0, 1);

        mockMvc.perform(get("/game/init").header("Authorization", bearer(7501)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoSettledSatisfactionRewards.length()").value(1))
                .andExpect(jsonPath("$.data.autoSettledSatisfactionRewards[0].businessDate").value("2026-08-04"))
                .andExpect(jsonPath("$.data.autoSettledSatisfactionRewards[0].giftTier").value("S100"))
                .andExpect(jsonPath("$.data.autoSettledSatisfactionRewards[0].rewardGold").value(500));
        mockMvc.perform(get("/game/init").header("Authorization", bearer(7501)))
                .andExpect(jsonPath("$.data.autoSettledSatisfactionRewards.length()").value(0));
        assertEquals(1000L, gold(player));
    }

    private void insertPlayer(long userId, String nickname) {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id,game_id,nickname,level,exp,cumulative_exp,gold,diamond)
                VALUES (?,'fruit_island',?,1,0,0,500,20)
                """, userId, nickname);
    }

    private void insertClosed(long playerId, LocalDate date, int delivered, int rejected, int quantity) {
        for (int i = 0; i < delivered; i++) insertOrder(playerId, quantity, "DELIVERED", date.atTime(12, 0));
        for (int i = 0; i < rejected; i++) insertOrder(playerId, 1, "OUT_OF_STOCK", date.atTime(12, 0));
    }

    private long insertWaiting(long playerId, int quantity) {
        return insertOrder(playerId, quantity, "WAITING", null);
    }

    private long insertOrder(long playerId, int quantity, String status, LocalDateTime closeTime) {
        jdbcTemplate.update("""
                INSERT INTO customer_order
                (player_id,customer_id,recipe_id,item_id,quantity,unit_gold_snapshot,
                 unit_exp_snapshot,queue_position,status,create_time,close_time,close_reason)
                VALUES (?,'berry','strawberry_juice','strawberry_juice',?,30,5,1,?,?,?,?)
                """, playerId, quantity, status,
                closeTime == null ? LocalDateTime.ofInstant(clock.instant(), BEIJING) : closeTime.minusMinutes(1),
                closeTime, closeTime == null ? null : status);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM customer_order WHERE player_id=?", Long.class, playerId);
    }

    private long playerId(long userId) {
        return jdbcTemplate.queryForObject("SELECT id FROM game_player WHERE user_id=?", Long.class, userId);
    }

    private String bearer(long userId) {
        return "Bearer " + jwtUtils.generateToken(userId, "satisfaction-" + userId);
    }

    private LocalDateTime closeTime(long orderId) {
        return jdbcTemplate.queryForObject("SELECT close_time FROM customer_order WHERE id=?", LocalDateTime.class, orderId);
    }

    private long gold(long playerId) {
        return jdbcTemplate.queryForObject("SELECT gold FROM game_player WHERE id=?", Long.class, playerId);
    }

    private int countDaily(long playerId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM daily_satisfaction WHERE player_id=?", Integer.class, playerId);
    }

    private int dailyPercent(long playerId) {
        return jdbcTemplate.queryForObject("SELECT satisfaction_percent FROM daily_satisfaction WHERE player_id=?", Integer.class, playerId);
    }

    private String dailyTier(long playerId) {
        return jdbcTemplate.queryForObject("SELECT gift_tier_snapshot FROM daily_satisfaction WHERE player_id=?", String.class, playerId);
    }

    private long dailyReward(long playerId) {
        return jdbcTemplate.queryForObject("SELECT reward_gold_snapshot FROM daily_satisfaction WHERE player_id=?", Long.class, playerId);
    }

    @TestConfiguration
    static class ClockTestConfig {
        @Bean
        @Primary
        MutableClock satisfactionClock() {
            return new MutableClock(Instant.parse("2026-08-05T04:00:00Z"));
        }
    }

    static final class MutableClock extends Clock {
        private volatile Instant current;

        private MutableClock(Instant current) { this.current = current; }
        void set(Instant instant) { current = instant; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(current, zone); }
        @Override public Instant instant() { return current; }
    }
}
