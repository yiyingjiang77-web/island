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
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Demo2.8 花卉与蜂蜜系统 HTTP 集成测试。
 *
 * 覆盖范围：
 *  - 作物目录迁移验证（删除4种、10级配置、单一解锁渠道）
 *  - 花卉购买（金币/钻石）、升级、种植、浇水、收获全链路
 *  - 蜂箱购买（递增价格、上限3）、惰性产蜜结算、收取蜂蜜
 *  - 玩家隔离
 *  - 并发幂等
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlowerBeehiveHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final long USER_A = 8101L;
    private static final long USER_B = 8102L;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM player_beehive");
        jdbcTemplate.update("DELETE FROM player_flower_right");
        jdbcTemplate.update("DELETE FROM crop_plant");
        jdbcTemplate.update("DELETE FROM player_land");
        jdbcTemplate.update("DELETE FROM inventory");
        jdbcTemplate.update("DELETE FROM player_island_level_reward_claim");
        jdbcTemplate.update("DELETE FROM player_recipe");
        jdbcTemplate.update("DELETE FROM player_crop");
        jdbcTemplate.update("DELETE FROM island");
        jdbcTemplate.update("DELETE FROM game_player WHERE user_id IN (?, ?)", USER_A, USER_B);
    }

    // ========================================================================
    // 作物目录迁移验证
    // ========================================================================

    @Test
    void gameInitReturnsMigratedCropCatalogWithoutDeletedCrops() throws Exception {
        String token = jwtUtils.generateToken(USER_A, "crop-catalog");

        mockMvc.perform(get("/game/init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                // 10 permanent crops + 1 rare temporary (moonberry) = 11 total
                .andExpect(jsonPath("$.data.cropConfigs.length()").value(11))
                .andExpect(jsonPath("$.data.cropConfigs[*].cropId",
                        containsInAnyOrder(
                                "strawberry", "carrot", "orange", "tomato", "blueberry",
                                "apple", "watermelon", "wheat", "lemon", "cucumber",
                                "moonberry")))
                // Deleted crops must NOT appear
                .andExpect(jsonPath("$.data.cropConfigs[?(@.cropId == 'cabbage')]").doesNotExist())
                .andExpect(jsonPath("$.data.cropConfigs[?(@.cropId == 'potato')]").doesNotExist())
                .andExpect(jsonPath("$.data.cropConfigs[?(@.cropId == 'chili')]").doesNotExist())
                .andExpect(jsonPath("$.data.cropConfigs[?(@.cropId == 'corn')]").doesNotExist())
                // 10 permanent crops × 10 levels = 100 level configs
                // moonberry has 1 level, so total = 101
                .andExpect(jsonPath("$.data.cropLevelConfigs.length()").value(101))
                // All permanent crops have max_crop_level = 10
                .andExpect(jsonPath("$.data.cropConfigs[?(@.cropId == 'strawberry')].maxCropLevel").value(10))
                .andExpect(jsonPath("$.data.cropConfigs[?(@.cropId == 'cucumber')].maxCropLevel").value(10));
    }

    // ========================================================================
    // 花卉配置验证
    // ========================================================================

    @Test
    void gameInitReturnsEightFlowerConfigsWithEightyLevelConfigs() throws Exception {
        String token = jwtUtils.generateToken(USER_A, "flower-configs");

        mockMvc.perform(get("/game/init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.flowerConfigs.length()").value(8))
                .andExpect(jsonPath("$.data.flowerConfigs[*].flowerId",
                        containsInAnyOrder(
                                "rose", "chrysanthemum", "jasmine", "osmanthus",
                                "lavender", "hibiscus", "chamomile", "sakura")))
                // 8 flowers × 10 levels = 80 level configs
                .andExpect(jsonPath("$.data.flowerLevelConfigs.length()").value(80))
                // Sakura is the diamond flower with coefficient 2
                .andExpect(jsonPath("$.data.flowerConfigs[?(@.flowerId == 'sakura')].currencyType").value("DIAMOND"))
                .andExpect(jsonPath("$.data.flowerConfigs[?(@.flowerId == 'sakura')].honeyCoefficient").value(2))
                // Rose is a gold flower with coefficient 1
                .andExpect(jsonPath("$.data.flowerConfigs[?(@.flowerId == 'rose')].currencyType").value("GOLD"))
                .andExpect(jsonPath("$.data.flowerConfigs[?(@.flowerId == 'rose')].honeyCoefficient").value(1))
                // New player has no flower rights yet
                .andExpect(jsonPath("$.data.playerFlowerRights.length()").value(0))
                // New player beehive is initialized with 0 beehives
                .andExpect(jsonPath("$.data.playerBeehive.beehiveCount").value(0))
                .andExpect(jsonPath("$.data.playerBeehive.honeyStored").value(0));
    }

    // ========================================================================
    // 花卉购买与升级
    // ========================================================================

    @Test
    void playerPurchasesGoldFlowerAndUpgradesToLevelTwo() throws Exception {
        createPlayer(USER_A, 10000, 100);
        String token = jwtUtils.generateToken(USER_A, "flower-buy-upgrade");

        // Purchase rose (GOLD, 500)
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.flowerId").value("rose"))
                .andExpect(jsonPath("$.data.flowerLevel").value(1))
                .andExpect(jsonPath("$.data.unlockSource").value("GOLD_SHOP"));

        // Verify gold deducted: 10000 - 500 = 9500
        long gold = jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        assertEquals(9500L, gold);

        // Upgrade rose to level 2 (costs 500 gold per level config)
        mockMvc.perform(post("/flower/upgrade")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.flowerId").value("rose"))
                .andExpect(jsonPath("$.data.flowerLevel").value(2));

        // Verify gold deducted: 9500 - 500 = 9000
        gold = jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        assertEquals(9000L, gold);

        // Duplicate purchase fails
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("已拥有该花卉种植权"));
    }

    @Test
    void playerPurchasesDiamondFlowerSakura() throws Exception {
        createPlayer(USER_A, 5000, 50);
        String token = jwtUtils.generateToken(USER_A, "diamond-flower");

        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"sakura"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.flowerId").value("sakura"))
                .andExpect(jsonPath("$.data.flowerLevel").value(1))
                .andExpect(jsonPath("$.data.unlockSource").value("DIAMOND_SHOP"));

        int diamond = jdbcTemplate.queryForObject(
                "SELECT diamond FROM game_player WHERE user_id = ?", Integer.class, USER_A);
        assertEquals(40, diamond); // 50 - 10 = 40
    }

    @Test
    void insufficientGoldOrDiamondIsRejected() throws Exception {
        createPlayer(USER_A, 100, 0);
        String token = jwtUtils.generateToken(USER_A, "poor-player");

        // Cannot afford rose (500 gold)
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("金币不足，需要 500"));

        // Cannot afford sakura (10 diamond)
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"sakura"}
                                """))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("钻石不足，需要 10"));
    }

    // ========================================================================
    // 花卉种植、浇水、收获全链路
    // ========================================================================

    @Test
    void playerPlantsWatersAndHarvestsFlower() throws Exception {
        createPlayer(USER_A, 5000, 20);
        String token = jwtUtils.generateToken(USER_A, "flower-lifecycle");
        long playerId = playerId(USER_A);

        // Purchase rose
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        // Get the free land (land_config_id=1, the first one)
        long landId = jdbcTemplate.queryForObject(
                "SELECT id FROM player_land WHERE player_id = ?", Long.class, playerId);

        // Plant rose on the land
        mockMvc.perform(post("/farm/plant")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d,"cropId":"rose"}
                                """.formatted(landId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PLANTED"))
                .andExpect(jsonPath("$.data.cropId").value("rose"))
                .andExpect(jsonPath("$.data.cropLevel").value(1))
                // Rose Lv1: grow_seconds=300, yield_count=2, harvest_exp=4
                .andExpect(jsonPath("$.data.yieldCount").value(2))
                .andExpect(jsonPath("$.data.harvestExp").value(4))
                .andExpect(jsonPath("$.data.finishTime").doesNotExist());

        // Water the flower
        mockMvc.perform(post("/farm/water")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d}
                                """.formatted(landId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.waterLevel").value(100))
                .andExpect(jsonPath("$.data.finishTime").exists());

        // Simulate time passage: set finish_time to the past so the crop becomes READY
        jdbcTemplate.update(
                "UPDATE player_land SET finish_time = ? WHERE id = ?",
                LocalDateTime.now().minusSeconds(600), landId);

        // Trigger READY status by calling /farm/lands (listByPlayer checks finish_time)
        mockMvc.perform(get("/farm/lands")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].status").value("READY"));

        // Harvest the flower
        mockMvc.perform(post("/farm/harvest")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d}
                                """.formatted(landId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cropId").value("rose"))
                .andExpect(jsonPath("$.data.yieldCount").value(2))
                .andExpect(jsonPath("$.data.expGained").value(4));

        // Verify rose is in inventory
        int roseCount = jdbcTemplate.queryForObject(
                "SELECT count FROM inventory WHERE player_id = ? AND item_id = 'rose'",
                Integer.class, playerId);
        assertEquals(2, roseCount);

        // Land is empty again
        mockMvc.perform(get("/farm/lands")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].status").value("EMPTY"));
    }

    @Test
    void playerCannotPlantFlowerWithoutPurchase() throws Exception {
        createPlayer(USER_A, 5000, 20);
        String token = jwtUtils.generateToken(USER_A, "no-flower-right");
        long playerId = playerId(USER_A);

        long landId = jdbcTemplate.queryForObject(
                "SELECT id FROM player_land WHERE player_id = ?", Long.class, playerId);

        mockMvc.perform(post("/farm/plant")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d,"cropId":"rose"}
                                """.formatted(landId)))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("尚未获得该花卉的种植权限"));
    }

    // ========================================================================
    // 花卉升级后种植使用新等级快照
    // ========================================================================

    @Test
    void upgradedFlowerUsesHigherLevelSnapshotWhenPlanted() throws Exception {
        createPlayer(USER_A, 20000, 20);
        String token = jwtUtils.generateToken(USER_A, "flower-upgrade-plant");
        long playerId = playerId(USER_A);

        // Purchase and upgrade rose to level 3
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.code").value(0));
        // Upgrade to level 2 (500 gold)
        mockMvc.perform(post("/flower/upgrade")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.data.flowerLevel").value(2));
        // Upgrade to level 3 (1000 gold)
        mockMvc.perform(post("/flower/upgrade")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.data.flowerLevel").value(3));

        long landId = jdbcTemplate.queryForObject(
                "SELECT id FROM player_land WHERE player_id = ?", Long.class, playerId);

        // Plant rose at level 3
        // Rose Lv3: grow_seconds=270, yield_count=3, harvest_exp=5
        mockMvc.perform(post("/farm/plant")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d,"cropId":"rose"}
                                """.formatted(landId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cropLevel").value(3))
                .andExpect(jsonPath("$.data.yieldCount").value(3))
                .andExpect(jsonPath("$.data.harvestExp").value(5));
    }

    // ========================================================================
    // 蜂箱购买
    // ========================================================================

    @Test
    void playerPurchasesThreeBeehivesWithIncrementalPrices() throws Exception {
        createPlayer(USER_A, 10000, 20);
        String token = jwtUtils.generateToken(USER_A, "beehive-purchase");

        // First beehive: 1000 gold
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.beehiveCount").value(1));

        assertEquals(9000L, jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A));

        // Second beehive: 2000 gold
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.beehiveCount").value(2));

        assertEquals(7000L, jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A));

        // Third beehive: 3000 gold
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.beehiveCount").value(3));

        assertEquals(4000L, jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A));

        // Fourth beehive fails
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("蜂箱数量已达上限"));
    }

    // ========================================================================
    // 蜂箱产蜜与收取
    // ========================================================================

    @Test
    void beehiveProducesHoneyFromReadyFlowersAndPlayerCollects() throws Exception {
        createPlayer(USER_A, 5000, 20);
        String token = jwtUtils.generateToken(USER_A, "honey-produce");
        long playerId = playerId(USER_A);

        // Buy beehive (1000 gold)
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.beehiveCount").value(1));

        // Buy rose (500 gold) — coefficient 1, level 1
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        // Plant rose on land
        long landId = jdbcTemplate.queryForObject(
                "SELECT id FROM player_land WHERE player_id = ?", Long.class, playerId);
        mockMvc.perform(post("/farm/plant")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d,"cropId":"rose"}
                                """.formatted(landId)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/farm/water")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d}
                                """.formatted(landId)))
                .andExpect(jsonPath("$.code").value(0));

        // Simulate flower becoming READY
        jdbcTemplate.update(
                "UPDATE player_land SET status = 'READY', finish_time = ? WHERE id = ?",
                LocalDateTime.now().minusSeconds(600), landId);

        // Simulate 2+ hours of beehive production time
        // honey per cycle = floor(1 rose × coefficient 1 × multiplier 1.0) = 1
        // 1 beehive storage cap = 20
        jdbcTemplate.update(
                "UPDATE player_beehive SET last_produce_time = ? WHERE player_id = ?",
                LocalDateTime.now().minus(3, ChronoUnit.HOURS), playerId);

        // Call /beehive/status to trigger lazy settlement
        mockMvc.perform(get("/beehive/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.beehiveCount").value(1))
                .andExpect(jsonPath("$.data.honeyStored").value(1));

        // Collect honey
        mockMvc.perform(post("/beehive/collect")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.honeyCollected").value(1));

        // Verify honey is in inventory
        int honeyCount = jdbcTemplate.queryForObject(
                "SELECT count FROM inventory WHERE player_id = ? AND item_id = 'honey'",
                Integer.class, playerId);
        assertEquals(1, honeyCount);

        // Beehive honey is now 0
        mockMvc.perform(get("/beehive/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.honeyStored").value(0));
    }

    @Test
    void beehiveWithSakuraProducesMoreHoneyDueToHigherCoefficient() throws Exception {
        createPlayer(USER_A, 5000, 50);
        String token = jwtUtils.generateToken(USER_A, "sakura-honey");
        long playerId = playerId(USER_A);

        // Buy beehive
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Buy sakura (10 diamond, coefficient 2)
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"sakura"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        // Plant sakura
        long landId = jdbcTemplate.queryForObject(
                "SELECT id FROM player_land WHERE player_id = ?", Long.class, playerId);
        mockMvc.perform(post("/farm/plant")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d,"cropId":"sakura"}
                                """.formatted(landId)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/farm/water")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d}
                                """.formatted(landId)))
                .andExpect(jsonPath("$.code").value(0));

        // Make sakura READY
        jdbcTemplate.update(
                "UPDATE player_land SET status = 'READY', finish_time = ? WHERE id = ?",
                LocalDateTime.now().minusSeconds(600), landId);

        // Simulate 2h+ production
        // honey per cycle = floor(1 sakura × coefficient 2 × multiplier 1.0) = 2
        jdbcTemplate.update(
                "UPDATE player_beehive SET last_produce_time = ? WHERE player_id = ?",
                LocalDateTime.now().minus(3, ChronoUnit.HOURS), playerId);

        mockMvc.perform(get("/beehive/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.honeyStored").value(2));
    }

    @Test
    void beehiveStorageCapLimitsAccumulatedHoney() throws Exception {
        createPlayer(USER_A, 20000, 50);
        String token = jwtUtils.generateToken(USER_A, "honey-cap");
        long playerId = playerId(USER_A);

        // Buy 2 beehives (storage cap = 40)
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.beehiveCount").value(1));
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.beehiveCount").value(2));

        // Buy and plant 3 roses
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        // Plant rose on land 1 and make READY
        long landId = jdbcTemplate.queryForObject(
                "SELECT id FROM player_land WHERE player_id = ?", Long.class, playerId);
        mockMvc.perform(post("/farm/plant")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d,"cropId":"rose"}
                                """.formatted(landId)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/farm/water")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d}
                                """.formatted(landId)))
                .andExpect(jsonPath("$.code").value(0));
        jdbcTemplate.update(
                "UPDATE player_land SET status = 'READY', finish_time = ? WHERE id = ?",
                LocalDateTime.now().minusSeconds(600), landId);

        // Simulate 100 hours of production (way more than cap)
        // honey per cycle = 1, cycles = 100*3600/7200 = 50
        // raw production = 50, but cap = 40
        jdbcTemplate.update(
                "UPDATE player_beehive SET last_produce_time = ? WHERE player_id = ?",
                LocalDateTime.now().minus(100, ChronoUnit.HOURS), playerId);

        mockMvc.perform(get("/beehive/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.honeyStored").value(40));
    }

    @Test
    void beehiveProducesNothingWithoutReadyFlowers() throws Exception {
        createPlayer(USER_A, 5000, 20);
        String token = jwtUtils.generateToken(USER_A, "no-flowers");
        long playerId = playerId(USER_A);

        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        // Simulate time passage with no flowers planted
        jdbcTemplate.update(
                "UPDATE player_beehive SET last_produce_time = ? WHERE player_id = ?",
                LocalDateTime.now().minus(10, ChronoUnit.HOURS), playerId);

        mockMvc.perform(get("/beehive/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.honeyStored").value(0));
    }

    // ========================================================================
    // 玩家隔离
    // ========================================================================

    @Test
    void playerAFlowerRightsAndBeehiveDoNotAppearInPlayerBInit() throws Exception {
        createPlayer(USER_A, 5000, 20);
        createPlayer(USER_B, 5000, 20);
        String tokenA = jwtUtils.generateToken(USER_A, "player-a");
        String tokenB = jwtUtils.generateToken(USER_B, "player-b");

        // Player A buys rose and a beehive
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.code").value(0));

        // Player B's init should show no flower rights and 0 beehives
        mockMvc.perform(get("/game/init")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.playerFlowerRights.length()").value(0))
                .andExpect(jsonPath("$.data.playerBeehive.beehiveCount").value(0));

        // Player A's init should show 1 flower right and 1 beehive
        mockMvc.perform(get("/game/init")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data.playerFlowerRights.length()").value(1))
                .andExpect(jsonPath("$.data.playerFlowerRights[0].flowerId").value("rose"))
                .andExpect(jsonPath("$.data.playerBeehive.beehiveCount").value(1));
    }

    // ========================================================================
    // 幂等性验证
    // ========================================================================

    @Test
    void duplicateFlowerPurchaseIsRejected() throws Exception {
        createPlayer(USER_A, 100000, 1000);
        String token = jwtUtils.generateToken(USER_A, "dup-flower");

        // First purchase succeeds
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        // Second purchase of the same flower is rejected
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("已拥有该花卉种植权"));

        // Only one flower right record exists
        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_flower_right WHERE player_id = ?",
                Integer.class, playerId(USER_A));
        assertEquals(1, count);

        // Gold was deducted only once
        long gold = jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        assertEquals(99500L, gold); // 100000 - 500 = 99500
    }

    @Test
    void exceedingBeehiveMaxIsRejected() throws Exception {
        createPlayer(USER_A, 100000, 20);
        String token = jwtUtils.generateToken(USER_A, "max-beehive");

        // Purchase 3 beehives (the maximum)
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.beehiveCount").value(1));
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.beehiveCount").value(2));
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.beehiveCount").value(3));

        // 4th purchase is rejected
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("蜂箱数量已达上限"));

        // Beehive count must be exactly 3
        int beehiveCount = jdbcTemplate.queryForObject(
                "SELECT beehive_count FROM player_beehive WHERE player_id = ?",
                Integer.class, playerId(USER_A));
        assertEquals(3, beehiveCount);
    }

    // ========================================================================
    // /game/init 蜂箱惰性结算验证
    // ========================================================================

    @Test
    void gameInitTriggersBeehiveLazySettlement() throws Exception {
        createPlayer(USER_A, 5000, 20);
        String token = jwtUtils.generateToken(USER_A, "init-settlement");
        long playerId = playerId(USER_A);

        // Buy beehive and rose, plant and make READY
        mockMvc.perform(post("/beehive/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/flower/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"flowerId":"rose"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        long landId = jdbcTemplate.queryForObject(
                "SELECT id FROM player_land WHERE player_id = ?", Long.class, playerId);
        mockMvc.perform(post("/farm/plant")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d,"cropId":"rose"}
                                """.formatted(landId)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/farm/water")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"playerLandId":%d}
                                """.formatted(landId)))
                .andExpect(jsonPath("$.code").value(0));

        jdbcTemplate.update(
                "UPDATE player_land SET status = 'READY', finish_time = ? WHERE id = ?",
                LocalDateTime.now().minusSeconds(600), landId);
        jdbcTemplate.update(
                "UPDATE player_beehive SET last_produce_time = ? WHERE player_id = ?",
                LocalDateTime.now().minus(3, ChronoUnit.HOURS), playerId);

        // /game/init should settle production and show honey
        mockMvc.perform(get("/game/init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.playerBeehive.honeyStored").value(1))
                .andExpect(jsonPath("$.data.playerBeehive.beehiveCount").value(1));
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private void createPlayer(long userId, long gold, int diamond) {
        jdbcTemplate.update("""
                INSERT INTO game_player
                (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (?, 'fruit_island', ?, 1, 0, 0, ?, ?)
                """, userId, "test-player-" + userId, gold, diamond);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = ?", Long.class, userId);
        // Create island
        jdbcTemplate.update(
                "INSERT INTO island (player_id, island_name, level) VALUES (?, ?, 1)",
                playerId, "test-island-" + userId);
        // Give the first free land (land_config_id = 1)
        jdbcTemplate.update("""
                INSERT INTO player_land (player_id, land_config_id, status)
                VALUES (?, 1, 'EMPTY')
                """, playerId);
    }

    private long playerId(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = ?", Long.class, userId);
    }
}
