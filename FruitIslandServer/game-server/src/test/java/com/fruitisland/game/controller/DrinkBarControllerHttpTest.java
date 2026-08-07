package com.fruitisland.game.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fruitisland.common.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(DrinkBarControllerHttpTest.ClockTestConfig.class)
class DrinkBarControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUpPlayer() {
        clock.set(Instant.parse("2026-07-30T02:00:00Z"));
        jdbcTemplate.update("DELETE FROM drink_bar_batch");
        jdbcTemplate.update("DELETE FROM drink_bar");
        jdbcTemplate.update("DELETE FROM inventory");
        jdbcTemplate.update("DELETE FROM player_island_level_reward_claim WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (6101, 6102))");
        jdbcTemplate.update("DELETE FROM player_crop_grant WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (6101, 6102))");
        jdbcTemplate.update("DELETE FROM player_crop WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (6101, 6102))");
        jdbcTemplate.update("DELETE FROM player_recipe WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (6101, 6102))");
        jdbcTemplate.update("DELETE FROM player_drink_shop WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (6101, 6102))");
        jdbcTemplate.update("DELETE FROM game_player WHERE user_id IN (6101, 6102)");
        jdbcTemplate.update("DELETE FROM recipe_material WHERE recipe_id IN ('mushroom_tea','mushroom_milkshake','chanterelle_soup','truffle_cocoa')");
        jdbcTemplate.update("DELETE FROM recipe_config WHERE id IN ('mushroom_tea','mushroom_milkshake','chanterelle_soup','truffle_cocoa')");
        jdbcTemplate.update("DELETE FROM recipe_config WHERE id = 'strawberry_juice'");
        jdbcTemplate.update("""
                INSERT INTO recipe_config
                (id, name, output_item, sale_gold, sale_exp, bar_sale_interval_seconds, enabled)
                VALUES ('strawberry_juice', '草莓汁', 'strawberry_juice', 30, 10, 180, 1)
                """);
        jdbcTemplate.update("""
                INSERT INTO game_player (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (6101, 'fruit_island', '吧台测试玩家', 1, 0, 0, 500, 20)
                """);
        jdbcTemplate.update("""
                INSERT INTO game_player (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (6102, 'fruit_island', '另一位玩家', 1, 0, 0, 500, 20)
                """);
    }

    @Test
    void shopLevelControlsReadContractExplicitQuantityAndNewBatchInterval() throws Exception {
        JsonNode levelOne = readBars(6101L, "level-driven-bars");
        long levelTenPlayer = playerId(6101L);
        long levelOnePlayer = playerId(6102L);
        long levelTenBar = levelOne.path("data").path("bars").get(0).path("barId").asLong();
        jdbcTemplate.update("UPDATE player_drink_shop SET shop_level=10 WHERE player_id=?", levelTenPlayer);
        jdbcTemplate.update("INSERT INTO inventory (player_id,item_id,count) VALUES (?,'strawberry_juice',20)", levelTenPlayer);

        String levelTenToken = jwtUtils.generateToken(6101L, "level-ten-bars");
        mockMvc.perform(get("/drink-shop/bars").header("Authorization", "Bearer " + levelTenToken))
                .andExpect(jsonPath("$.data.shopLevel").value(10))
                .andExpect(jsonPath("$.data.listingLimit").value(20))
                .andExpect(jsonPath("$.data.saleIntervalSeconds").value(270))
                .andExpect(jsonPath("$.data.bars.length()").value(6));
        mockMvc.perform(post("/drink-shop/bars/{barId}/list", levelTenBar)
                        .header("Authorization", "Bearer " + levelTenToken)
                        .contentType("application/json")
                        .content("{\"recipeId\":\"strawberry_juice\",\"quantity\":17}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bar.batch.listedQuantity").value(17))
                .andExpect(jsonPath("$.data.bar.batch.saleIntervalSecondsSnapshot").value(270))
                .andExpect(jsonPath("$.data.remainingInventory").value(3));

        JsonNode otherBars = readBars(6102L, "level-one-bars");
        long levelOneBar = otherBars.path("data").path("bars").get(0).path("barId").asLong();
        jdbcTemplate.update("INSERT INTO inventory (player_id,item_id,count) VALUES (?,'strawberry_juice',11)", levelOnePlayer);
        mockMvc.perform(post("/drink-shop/bars/{barId}/list", levelOneBar)
                        .header("Authorization", "Bearer " + jwtUtils.generateToken(6102L, "level-one-bars"))
                        .contentType("application/json")
                        .content("{\"recipeId\":\"strawberry_juice\",\"quantity\":11}"))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("上架数量必须为 1–10"));
    }

    @Test
    void readLazilySellsFirstDrinkOnlyAfterAFullSnapshotInterval() throws Exception {
        JsonNode initial = readBars(6101L, "sale-boundary");
        long playerId = playerId(6101L);
        long barId = initial.path("data").path("bars").get(0).path("barId").asLong();
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 2)
                """, playerId);
        String token = jwtUtils.generateToken(6101L, "sale-boundary");
        listDrink(token, barId).andExpect(jsonPath("$.code").value(0));

        clock.advance(Duration.ofSeconds(299));
        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("SELLING"))
                .andExpect(jsonPath("$.data.bars[0].batch.soldQuantity").value(0))
                .andExpect(jsonPath("$.data.bars[0].batch.remainingQuantity").value(2))
                .andExpect(jsonPath("$.data.bars[0].batch.pendingGold").value(0))
                .andExpect(jsonPath("$.data.bars[0].batch.pendingExp").value(0))
                .andExpect(jsonPath("$.data.bars[0].batch.nextSaleInSeconds").value(1));

        clock.advance(Duration.ofSeconds(1));
        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("SELLING"))
                .andExpect(jsonPath("$.data.bars[0].batch.soldQuantity").value(1))
                .andExpect(jsonPath("$.data.bars[0].batch.remainingQuantity").value(1))
                .andExpect(jsonPath("$.data.bars[0].batch.pendingGold").value(30))
                .andExpect(jsonPath("$.data.bars[0].batch.pendingExp").value(5))
                .andExpect(jsonPath("$.data.bars[0].batch.nextSaleInSeconds").value(300));
    }

    @Test
    void jwtPlayerTakesDownPartialBatchReturningUnsoldAndSettlingSoldRewards() throws Exception {
        JsonNode initial = readBars(6101L, "partial-take-down");
        long playerId = playerId(6101L);
        long barId = initial.path("data").path("bars").get(0).path("barId").asLong();
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 3)
                """, playerId);
        jdbcTemplate.update(
                "UPDATE game_player SET cumulative_exp = 98 WHERE id = ?", playerId);
        String token = jwtUtils.generateToken(6101L, "partial-take-down");
        listDrink(token, barId).andExpect(jsonPath("$.code").value(0));
        clock.advance(Duration.ofSeconds(300));

        mockMvc.perform(post("/drink-shop/bars/{barId}/take-down", barId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerId":999999}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.returnedQuantity").value(2))
                .andExpect(jsonPath("$.data.settledGold").value(30))
                .andExpect(jsonPath("$.data.settledExp").value(5))
                .andExpect(jsonPath("$.data.currentGold").value(530))
                .andExpect(jsonPath("$.data.cumulativeExp").value(103))
                .andExpect(jsonPath("$.data.currentLevel").value(2))
                .andExpect(jsonPath("$.data.bar.state").value("EMPTY"));

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("EMPTY"))
                .andExpect(jsonPath("$.data.drinks[0].inventoryCount").value(2));
    }

    @Test
    void offlineReadsUseEachBatchSnapshotAndCapSalesAtItsListedQuantity() throws Exception {
        JsonNode initial = readBars(6101L, "offline-snapshots");
        long playerId = playerId(6101L);
        long bar1 = initial.path("data").path("bars").get(0).path("barId").asLong();
        long bar2 = initial.path("data").path("bars").get(1).path("barId").asLong();
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 12)
                """, playerId);
        String token = jwtUtils.generateToken(6101L, "offline-snapshots");
        listDrink(token, bar1).andExpect(jsonPath("$.code").value(0));

        jdbcTemplate.update("UPDATE player_drink_shop SET shop_level=9 WHERE player_id=?", playerId);
        listDrink(token, bar2).andExpect(jsonPath("$.code").value(0));
        clock.advance(Duration.ofSeconds(540));

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("SELLING"))
                .andExpect(jsonPath("$.data.bars[0].batch.soldQuantity").value(1))
                .andExpect(jsonPath("$.data.bars[0].batch.saleIntervalSecondsSnapshot").value(300))
                .andExpect(jsonPath("$.data.bars[1].state").value("SOLD_OUT"))
                .andExpect(jsonPath("$.data.bars[1].batch.soldQuantity").value(2))
                .andExpect(jsonPath("$.data.bars[1].batch.saleIntervalSecondsSnapshot").value(270));
        takeDown(token, bar2)
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message")
                        .value("已售罄批次不能下架，请收取收益"));

        clock.advance(Duration.ofDays(30));
        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("SOLD_OUT"))
                .andExpect(jsonPath("$.data.bars[0].batch.soldQuantity").value(10))
                .andExpect(jsonPath("$.data.bars[0].batch.remainingQuantity").value(0))
                .andExpect(jsonPath("$.data.bars[0].batch.pendingGold").value(300))
                .andExpect(jsonPath("$.data.bars[0].batch.pendingExp").value(50));
    }

    @Test
    void zeroSaleAndConcurrentTakeDownReturnAndRewardOnlyOnce() throws Exception {
        JsonNode initial = readBars(6101L, "concurrent-take-down");
        long playerId = playerId(6101L);
        long barId = initial.path("data").path("bars").get(0).path("barId").asLong();
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 3)
                """, playerId);
        String token = jwtUtils.generateToken(6101L, "concurrent-take-down");
        listDrink(token, barId).andExpect(jsonPath("$.code").value(0));

        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                start.await();
                return takeDown(token, barId).andReturn().getResponse().getContentAsString();
            });
            var second = executor.submit(() -> {
                start.await();
                return takeDown(token, barId).andReturn().getResponse().getContentAsString();
            });
            start.countDown();
            List<Integer> codes = List.of(
                            objectMapper.readTree(first.get()).path("code").asInt(),
                            objectMapper.readTree(second.get()).path("code").asInt())
                    .stream().sorted().toList();
            assertEquals(List.of(-1, 0), codes);
        } finally {
            executor.shutdownNow();
        }

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("EMPTY"))
                .andExpect(jsonPath("$.data.drinks[0].inventoryCount").value(3));
        assertEquals(500L, jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE id = ?", Long.class, playerId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT exp FROM game_player WHERE id = ?", Integer.class, playerId));
    }

    @Test
    void jwtPlayerCannotTakeDownAnotherPlayersBatch() throws Exception {
        JsonNode ownerBars = readBars(6101L, "take-down-owner");
        long ownerId = playerId(6101L);
        long ownerBar = ownerBars.path("data").path("bars").get(0).path("barId").asLong();
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 2)
                """, ownerId);
        listDrink(jwtUtils.generateToken(6101L, "take-down-owner"), ownerBar)
                .andExpect(jsonPath("$.code").value(0));

        takeDown(jwtUtils.generateToken(6102L, "take-down-other"), ownerBar)
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("吧台不存在或尚未开放"));
    }

    @Test
    void jwtPlayerCollectsOnlyASoldOutBarAndReceivesItsSnapshotRewards() throws Exception {
        JsonNode initial = readBars(6101L, "single-collect");
        long playerId = playerId(6101L);
        long soldOutBar = initial.path("data").path("bars").get(0).path("barId").asLong();
        long sellingBar = initial.path("data").path("bars").get(1).path("barId").asLong();
        insertBatch(playerId, soldOutBar, "SOLD_OUT", 1, 2, 2, 30, 5, 180);
        insertBatch(playerId, sellingBar, "SELLING", 1, 2, 1, 30, 5, 180);
        String token = jwtUtils.generateToken(6101L, "single-collect");

        mockMvc.perform(post("/drink-shop/bars/{barId}/collect", sellingBar)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerId":999999}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("吧台批次尚未售罄"));

        mockMvc.perform(post("/drink-shop/bars/{barId}/collect", soldOutBar)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.collectedBarCount").value(1))
                .andExpect(jsonPath("$.data.collectedBarIds[0]").value(soldOutBar))
                .andExpect(jsonPath("$.data.settledGold").value(60))
                .andExpect(jsonPath("$.data.settledExp").value(10))
                .andExpect(jsonPath("$.data.currentGold").value(560))
                .andExpect(jsonPath("$.data.cumulativeExp").value(10))
                .andExpect(jsonPath("$.data.currentLevel").value(1));

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("EMPTY"))
                .andExpect(jsonPath("$.data.bars[1].state").value("SELLING"));
    }

    @Test
    void collectAllSettlesBarsSoldOutAtCallTimeAndLeavesSellingBarsUntouched() throws Exception {
        JsonNode initial = readBars(6101L, "collect-all");
        long playerId = playerId(6101L);
        long alreadySoldOut = initial.path("data").path("bars").get(0).path("barId").asLong();
        long reachesSoldOut = initial.path("data").path("bars").get(1).path("barId").asLong();
        long remainsSelling = initial.path("data").path("bars").get(2).path("barId").asLong();
        insertBatch(playerId, alreadySoldOut, "SOLD_OUT", 1, 2, 2, 30, 5, 180);
        insertBatch(playerId, reachesSoldOut, "SELLING", 1, 1, 0, 90, 15, 180);
        insertBatch(playerId, remainsSelling, "SELLING", 1, 2, 0, 40, 7, 180);
        jdbcTemplate.update("UPDATE game_player SET cumulative_exp = 95 WHERE id = ?", playerId);
        clock.advance(Duration.ofSeconds(180));
        String token = jwtUtils.generateToken(6101L, "collect-all");

        mockMvc.perform(post("/drink-shop/bars/collect-all")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerId":999999}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.collectedBarCount").value(2))
                .andExpect(jsonPath("$.data.collectedBarIds[0]").value(alreadySoldOut))
                .andExpect(jsonPath("$.data.collectedBarIds[1]").value(reachesSoldOut))
                .andExpect(jsonPath("$.data.settledGold").value(150))
                .andExpect(jsonPath("$.data.settledExp").value(25))
                .andExpect(jsonPath("$.data.currentGold").value(650))
                .andExpect(jsonPath("$.data.cumulativeExp").value(120))
                .andExpect(jsonPath("$.data.currentLevel").value(2));

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("EMPTY"))
                .andExpect(jsonPath("$.data.bars[1].state").value("EMPTY"))
                .andExpect(jsonPath("$.data.bars[2].state").value("SELLING"))
                .andExpect(jsonPath("$.data.bars[2].batch.soldQuantity").value(1))
                .andExpect(jsonPath("$.data.bars[2].batch.remainingQuantity").value(1));
    }

    @Test
    void concurrentSingleAndCollectAllRequestsRewardASoldOutBatchOnlyOnce() throws Exception {
        JsonNode initial = readBars(6101L, "concurrent-collect");
        long playerId = playerId(6101L);
        long soldOutBar = initial.path("data").path("bars").get(0).path("barId").asLong();
        insertBatch(playerId, soldOutBar, "SOLD_OUT", 1, 2, 2, 30, 5, 180);
        String token = jwtUtils.generateToken(6101L, "concurrent-collect");
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var single = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/drink-shop/bars/{barId}/collect", soldOutBar)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{}"))
                        .andReturn().getResponse().getContentAsString();
            });
            var all = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/drink-shop/bars/collect-all")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{}"))
                        .andReturn().getResponse().getContentAsString();
            });
            start.countDown();
            List<Integer> codes = List.of(
                            objectMapper.readTree(single.get()).path("code").asInt(),
                            objectMapper.readTree(all.get()).path("code").asInt())
                    .stream().sorted().toList();
            assertEquals(List.of(-1, 0), codes);
        } finally {
            executor.shutdownNow();
        }

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("EMPTY"));
        assertEquals(560L, jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE id = ?", Long.class, playerId));
        assertEquals(10, jdbcTemplate.queryForObject(
                "SELECT exp FROM game_player WHERE id = ?", Integer.class, playerId));

        mockMvc.perform(post("/drink-shop/bars/collect-all")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("当前没有可收取的售罄吧台"));
    }

    @Test
    void jwtPlayerReceivesSixStableEmptyBarsOnFirstRead() throws Exception {
        String token = jwtUtils.generateToken(6101L, "bar-player");

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bars.length()").value(6))
                .andExpect(jsonPath("$.data.bars[0].slotNumber").value(1))
                .andExpect(jsonPath("$.data.bars[1].slotNumber").value(2))
                .andExpect(jsonPath("$.data.bars[2].slotNumber").value(3))
                .andExpect(jsonPath("$.data.bars[3].slotNumber").value(4))
                .andExpect(jsonPath("$.data.bars[4].slotNumber").value(5))
                .andExpect(jsonPath("$.data.bars[5].slotNumber").value(6))
                .andExpect(jsonPath("$.data.bars[0].state").value("EMPTY"))
                .andExpect(jsonPath("$.data.bars[5].state").value("EMPTY"));
    }

    @Test
    void repeatedReadsKeepTheSameSixBarIds() throws Exception {
        JsonNode first = readBars(6101L, "first-read");
        JsonNode second = readBars(6101L, "second-read");

        assertEquals(first.path("data").path("bars"), second.path("data").path("bars"));
    }

    @Test
    void jwtReadReturnsOnlyThatPlayersActiveBatchesAndTheirSnapshots() throws Exception {
        readBars(6101L, "owner");
        readBars(6102L, "other");
        long ownerId = playerId(6101L);
        long otherId = playerId(6102L);
        long ownerBar1 = barId(ownerId, 1);
        long ownerBar2 = barId(ownerId, 2);
        long otherBar1 = barId(otherId, 1);

        insertBatch(ownerId, ownerBar1, "CLOSED", null, 10, 10, 11, 2, 240);
        insertBatch(ownerId, ownerBar1, "SELLING", 1, 9, 3, 30, 5, 180);
        insertBatch(ownerId, ownerBar2, "SOLD_OUT", 1, 4, 4, 31, 6, 200);
        insertBatch(otherId, otherBar1, "SELLING", 1, 7, 1, 999, 999, 999);

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer "
                                + jwtUtils.generateToken(6101L, "owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bars.length()").value(6))
                .andExpect(jsonPath("$.data.bars[0].state").value("SELLING"))
                .andExpect(jsonPath("$.data.bars[0].batch.recipeId").value("strawberry_juice"))
                .andExpect(jsonPath("$.data.bars[0].batch.itemId").value("strawberry_juice"))
                .andExpect(jsonPath("$.data.bars[0].batch.listedQuantity").value(9))
                .andExpect(jsonPath("$.data.bars[0].batch.soldQuantity").value(3))
                .andExpect(jsonPath("$.data.bars[0].batch.unitGoldSnapshot").value(30))
                .andExpect(jsonPath("$.data.bars[0].batch.unitExpSnapshot").value(5))
                .andExpect(jsonPath("$.data.bars[0].batch.saleIntervalSecondsSnapshot").value(180))
                .andExpect(jsonPath("$.data.bars[1].state").value("SOLD_OUT"))
                .andExpect(jsonPath("$.data.bars[1].batch.unitGoldSnapshot").value(31))
                .andExpect(jsonPath("$.data.bars[1].batch.unitExpSnapshot").value(6))
                .andExpect(jsonPath("$.data.bars[1].batch.saleIntervalSecondsSnapshot").value(200))
                .andExpect(jsonPath("$.data.bars[2].state").value("EMPTY"));
    }

    @Test
    void unknownActiveBatchStatusFailsInsteadOfAppearingAsSelling() throws Exception {
        readBars(6101L, "invalid-status");
        long playerId = playerId(6101L);
        insertBatch(
                playerId,
                barId(playerId, 1),
                "PAUSED",
                1,
                2,
                0,
                30,
                5,
                180);

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer "
                                + jwtUtils.generateToken(6101L, "invalid-status")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("吧台批次状态无效：PAUSED"));
    }

    @Test
    void jwtPlayerListsAllNineDrinksAndReadsTheAuthoritativeBatchAndInventory() throws Exception {
        JsonNode bars = readBars(6101L, "listing-player");
        long playerId = playerId(6101L);
        long barId = bars.path("data").path("bars").get(0).path("barId").asLong();
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 9)
                """, playerId);
        String token = jwtUtils.generateToken(6101L, "listing-player");

        mockMvc.perform(post("/drink-shop/bars/{barId}/list", barId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"recipeId":"strawberry_juice","playerId":999999}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bar.state").value("SELLING"))
                .andExpect(jsonPath("$.data.bar.batch.listedQuantity").value(9))
                .andExpect(jsonPath("$.data.bar.batch.soldQuantity").value(0))
                .andExpect(jsonPath("$.data.bar.batch.unitGoldSnapshot").value(30))
                .andExpect(jsonPath("$.data.bar.batch.unitExpSnapshot").value(5))
                .andExpect(jsonPath("$.data.bar.batch.saleIntervalSecondsSnapshot").value(300))
                .andExpect(jsonPath("$.data.remainingInventory").value(0))
                .andExpect(jsonPath("$.data.expectedBatchGold").value(270))
                .andExpect(jsonPath("$.data.expectedBatchExp").value(45));

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bars[0].state").value("SELLING"))
                .andExpect(jsonPath("$.data.bars[0].batch.listedQuantity").value(9));
    }

    @Test
    void listingUsesExactlyOneDrinkAtTheMinimumAndTenAtTheBatchLimit() throws Exception {
        JsonNode bars = readBars(6101L, "listing-exact-boundaries");
        long playerId = playerId(6101L);
        long bar1 = bars.path("data").path("bars").get(0).path("barId").asLong();
        long bar2 = bars.path("data").path("bars").get(1).path("barId").asLong();
        String token = jwtUtils.generateToken(6101L, "listing-exact-boundaries");
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 1)
                """, playerId);

        listDrink(token, bar1)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bar.batch.listedQuantity").value(1))
                .andExpect(jsonPath("$.data.remainingInventory").value(0));

        jdbcTemplate.update("""
                UPDATE inventory SET count = 10
                WHERE player_id = ? AND item_id = 'strawberry_juice'
                """, playerId);
        listDrink(token, bar2)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bar.batch.listedQuantity").value(10))
                .andExpect(jsonPath("$.data.remainingInventory").value(0));
    }

    @Test
    void newRewardAndIntervalConfigurationOnlyAffectsLaterBatches() throws Exception {
        JsonNode bars = readBars(6101L, "listing-all-snapshots");
        long playerId = playerId(6101L);
        long bar1 = bars.path("data").path("bars").get(0).path("barId").asLong();
        long bar2 = bars.path("data").path("bars").get(1).path("barId").asLong();
        String token = jwtUtils.generateToken(6101L, "listing-all-snapshots");
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 12)
                """, playerId);

        listDrink(token, bar1)
                .andExpect(jsonPath("$.data.bar.batch.unitGoldSnapshot").value(30))
                .andExpect(jsonPath("$.data.bar.batch.unitExpSnapshot").value(5))
                .andExpect(jsonPath("$.data.bar.batch.saleIntervalSecondsSnapshot").value(300));
        jdbcTemplate.update("""
                UPDATE recipe_config
                SET sale_gold = 80, sale_exp = 20, bar_sale_interval_seconds = 60
                WHERE id = 'strawberry_juice'
                """);
        listDrink(token, bar2)
                .andExpect(jsonPath("$.data.bar.batch.unitGoldSnapshot").value(80))
                .andExpect(jsonPath("$.data.bar.batch.unitExpSnapshot").value(10))
                .andExpect(jsonPath("$.data.bar.batch.saleIntervalSecondsSnapshot").value(300))
                .andExpect(jsonPath("$.data.expectedBatchGold").value(160))
                .andExpect(jsonPath("$.data.expectedBatchExp").value(20));

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].batch.unitGoldSnapshot").value(30))
                .andExpect(jsonPath("$.data.bars[0].batch.unitExpSnapshot").value(5))
                .andExpect(jsonPath("$.data.bars[0].batch.saleIntervalSecondsSnapshot").value(300))
                .andExpect(jsonPath("$.data.bars[1].batch.unitGoldSnapshot").value(80))
                .andExpect(jsonPath("$.data.bars[1].batch.unitExpSnapshot").value(10))
                .andExpect(jsonPath("$.data.bars[1].batch.saleIntervalSecondsSnapshot").value(300));
    }

    @Test
    void barReadShowsAuthoritativeListingQuantityAndExpectedRewards() throws Exception {
        long playerId = playerId(6101L);
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 12)
                """, playerId);

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer "
                                + jwtUtils.generateToken(6101L, "listing-options")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.drinks[0].recipeId").value("strawberry_juice"))
                .andExpect(jsonPath("$.data.drinks[0].inventoryCount").value(12))
                .andExpect(jsonPath("$.data.drinks[0].listingQuantity").value(10))
                .andExpect(jsonPath("$.data.drinks[0].unitGold").value(30))
                .andExpect(jsonPath("$.data.drinks[0].unitExp").value(5))
                .andExpect(jsonPath("$.data.drinks[0].expectedBatchGold").value(300))
                .andExpect(jsonPath("$.data.drinks[0].expectedBatchExp").value(50))
                .andExpect(jsonPath("$.data.drinks[0].saleIntervalSeconds").value(300));
    }

    @Test
    void zeroInventoryIsRejectedAndTheSameDrinkCanFillTwoIndependentBars() throws Exception {
        JsonNode initial = readBars(6101L, "listing-boundaries");
        long playerId = playerId(6101L);
        long bar1 = initial.path("data").path("bars").get(0).path("barId").asLong();
        long bar2 = initial.path("data").path("bars").get(1).path("barId").asLong();
        String token = jwtUtils.generateToken(6101L, "listing-boundaries");

        listDrink(token, bar1)
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("成品库存不足"));
        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("EMPTY"))
                .andExpect(jsonPath("$.data.drinks[0].inventoryCount").value(0))
                .andExpect(jsonPath("$.data.drinks[0].listingQuantity").value(0));

        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 12)
                """, playerId);
        listDrink(token, bar1)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bar.batch.listedQuantity").value(10))
                .andExpect(jsonPath("$.data.remainingInventory").value(2));
        listDrink(token, bar1)
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("吧台当前不是空闲状态"));
        listDrink(token, bar2)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bar.batch.listedQuantity").value(2))
                .andExpect(jsonPath("$.data.remainingInventory").value(0));

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("SELLING"))
                .andExpect(jsonPath("$.data.bars[1].state").value("SELLING"))
                .andExpect(jsonPath("$.data.bars[0].batch.itemId").value("strawberry_juice"))
                .andExpect(jsonPath("$.data.bars[1].batch.itemId").value("strawberry_juice"))
                .andExpect(jsonPath("$.data.drinks[0].inventoryCount").value(0));
    }

    @Test
    void jwtPlayerCannotListOnAnotherPlayersBar() throws Exception {
        JsonNode ownerBars = readBars(6101L, "owner");
        long ownerBar = ownerBars.path("data").path("bars").get(0).path("barId").asLong();
        long otherPlayerId = playerId(6102L);
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 10)
                """, otherPlayerId);

        listDrink(jwtUtils.generateToken(6102L, "other"), ownerBar)
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("吧台不存在或尚未开放"));
    }

    @Test
    void concurrentRequestsOnOneBarDeductOnlyOneBatch() throws Exception {
        JsonNode initial = readBars(6101L, "concurrent-listing");
        long playerId = playerId(6101L);
        long barId = initial.path("data").path("bars").get(0).path("barId").asLong();
        jdbcTemplate.update("""
                INSERT INTO inventory (player_id, item_id, count)
                VALUES (?, 'strawberry_juice', 12)
                """, playerId);
        String token = jwtUtils.generateToken(6101L, "concurrent-listing");
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                start.await();
                return listDrink(token, barId).andReturn().getResponse().getContentAsString();
            });
            var second = executor.submit(() -> {
                start.await();
                return listDrink(token, barId).andReturn().getResponse().getContentAsString();
            });
            start.countDown();
            List<Integer> codes = List.of(
                            objectMapper.readTree(first.get()).path("code").asInt(),
                            objectMapper.readTree(second.get()).path("code").asInt())
                    .stream().sorted().toList();
            assertEquals(List.of(-1, 0), codes);
        } finally {
            executor.shutdownNow();
        }

        mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.bars[0].state").value("SELLING"))
                .andExpect(jsonPath("$.data.bars[0].batch.listedQuantity").value(10))
                .andExpect(jsonPath("$.data.drinks[0].inventoryCount").value(2));
    }

    private JsonNode readBars(long userId, String username) throws Exception {
        String json = mockMvc.perform(get("/drink-shop/bars")
                        .header("Authorization", "Bearer "
                                + jwtUtils.generateToken(userId, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json);
    }

    private org.springframework.test.web.servlet.ResultActions listDrink(
            String token, long barId
    ) throws Exception {
        return mockMvc.perform(post("/drink-shop/bars/{barId}/list", barId)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                        {"recipeId":"strawberry_juice"}
                        """));
    }

    private org.springframework.test.web.servlet.ResultActions takeDown(
            String token, long barId
    ) throws Exception {
        return mockMvc.perform(post("/drink-shop/bars/{barId}/take-down", barId)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{}"));
    }

    private long playerId(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = ?", Long.class, userId);
    }

    private long barId(long playerId, int slotNumber) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM drink_bar WHERE player_id = ? AND slot_number = ?",
                Long.class, playerId, slotNumber);
    }

    private void insertBatch(
            long playerId,
            long barId,
            String status,
            Integer activeMarker,
            int listed,
            int sold,
            int gold,
            int exp,
            int interval
    ) {
        jdbcTemplate.update("""
                        INSERT INTO drink_bar_batch
                        (player_id, bar_id, recipe_id, item_id, listed_quantity, sold_quantity,
                         status, active_marker, unit_gold_snapshot, unit_exp_snapshot,
                         sale_interval_seconds_snapshot, listed_at, sold_out_at, closed_at)
                        VALUES (?, ?, 'strawberry_juice', 'strawberry_juice', ?, ?, ?, ?, ?, ?, ?,
                                ?,
                                CASE WHEN ? = 'SOLD_OUT' THEN ? ELSE NULL END,
                                CASE WHEN ? = 'CLOSED' THEN ? ELSE NULL END)
                        """,
                playerId, barId, listed, sold, status, activeMarker, gold, exp, interval,
                LocalDateTime.ofInstant(
                        clock.instant().minusSeconds((long) sold * interval),
                        ZoneOffset.UTC),
                status, LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
                status, LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    @TestConfiguration
    static class ClockTestConfig {

        @Bean
        @Primary
        MutableClock mutableDrinkBarClock() {
            return new MutableClock(Instant.parse("2026-07-30T02:00:00Z"));
        }
    }

    static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        void set(Instant instant) {
            current = instant;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
