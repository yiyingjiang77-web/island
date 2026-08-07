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
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Demo2.10 蛋糕店系统 HTTP 集成测试。
 *
 * 覆盖范围：
 *  - 配置加载：10级配置、字段验证
 *  - 未解锁状态：canUnlock、unlockHint
 *  - 解锁门槛：岛屿等级不足、金币不足
 *  - 解锁成功：扣金币、设等级
 *  - 逐级升级：正常升级、跳级拒绝、金币/岛级不足
 *  - 满级不可升级
 *  - 重复解锁拒绝
 *  - 玩家隔离
 *  - recipe_config 蛋糕类配方存在
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CakeShopHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final long USER_A = 9201L;
    private static final long USER_B = 9202L;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM player_cake_rack");
        jdbcTemplate.update("DELETE FROM player_cake_shop");
        jdbcTemplate.update("DELETE FROM inventory WHERE player_id IN (SELECT id FROM game_player WHERE user_id IN (?, ?))", USER_A, USER_B);
        jdbcTemplate.update("DELETE FROM player_island_level_reward_claim");
        jdbcTemplate.update("DELETE FROM player_recipe");
        jdbcTemplate.update("DELETE FROM player_crop");
        jdbcTemplate.update("DELETE FROM island");
        jdbcTemplate.update("DELETE FROM game_player WHERE user_id IN (?, ?)", USER_A, USER_B);
    }

    private String initPlayer(long userId, int level, long gold) throws Exception {
        String token = jwtUtils.generateToken(userId, "cake-shop-test");
        // /game/init creates the player
        mockMvc.perform(get("/game/init").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        // Set island level and gold
        jdbcTemplate.update("UPDATE game_player SET level = ?, gold = ? WHERE user_id = ?", level, gold, userId);
        // Re-call /game/init to trigger IslandGrowthService.initialize() for the new level
        // (grants recipes and crops for the updated island level, idempotent)
        mockMvc.perform(get("/game/init").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return token;
    }

    private Long getPlayerId(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = ?", Long.class, userId);
    }

    // ========== 配置加载 ==========

    @Test
    void cakeShopConfigHas10Levels() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cake_shop_config WHERE enabled = 1", Integer.class);
        assertEquals(10, count);
    }

    @Test
    void cakeShopConfigLevel1RequiresIsland8And5000Gold() {
        var row = jdbcTemplate.queryForMap(
                "SELECT * FROM cake_shop_config WHERE level = 1");
        assertEquals(8, row.get("REQUIRED_ISLAND_LEVEL"));
        assertEquals(5000, row.get("UPGRADE_GOLD"));
        assertEquals(8, row.get("RACK_CAPACITY"));
        assertEquals(480, row.get("SALE_INTERVAL_SECONDS"));
    }

    @Test
    void cakeShopConfigLevel10MaxValues() {
        var row = jdbcTemplate.queryForMap(
                "SELECT * FROM cake_shop_config WHERE level = 10");
        assertEquals(17, row.get("REQUIRED_ISLAND_LEVEL"));
        assertEquals(70000, row.get("UPGRADE_GOLD"));
        assertEquals(15, row.get("RACK_CAPACITY"));
        assertEquals(360, row.get("SALE_INTERVAL_SECONDS"));
    }

    @Test
    void recipeConfigHasCakeShopRecipes() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe_config WHERE craft_station = 'cake_shop' AND enabled = 1", Integer.class);
        // 1 existing (strawberry_cake) + 16 new = 17
        assertEquals(17, count);
    }

    @Test
    void strawberryCakeUpdatedMaterials() {
        Integer matCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe_material WHERE recipe_id = 'strawberry_cake'", Integer.class);
        assertEquals(4, matCount); // strawberry, wheat, egg, milk

        Integer milkCount = jdbcTemplate.queryForObject(
                "SELECT count FROM recipe_material WHERE recipe_id = 'strawberry_cake' AND item_id = 'milk'", Integer.class);
        assertEquals(1, milkCount);

        Integer eggCount = jdbcTemplate.queryForObject(
                "SELECT count FROM recipe_material WHERE recipe_id = 'strawberry_cake' AND item_id = 'egg'", Integer.class);
        assertEquals(2, eggCount);

        Integer strawberryCount = jdbcTemplate.queryForObject(
                "SELECT count FROM recipe_material WHERE recipe_id = 'strawberry_cake' AND item_id = 'strawberry'", Integer.class);
        assertEquals(3, strawberryCount);
    }

    @Test
    void strawberryCakeUpdatedPricing() {
        var row = jdbcTemplate.queryForMap(
                "SELECT sale_gold, sale_exp, craft_station FROM recipe_config WHERE id = 'strawberry_cake'");
        assertEquals(150, row.get("SALE_GOLD"));
        assertEquals(30, row.get("SALE_EXP"));
        assertEquals("cake_shop", row.get("CRAFT_STATION"));
    }

    // ========== 未解锁状态 ==========

    @Test
    void newPlayerSeesLockedCakeShop() throws Exception {
        String token = initPlayer(USER_A, 5, 10000);
        mockMvc.perform(get("/cake-shop/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.shop.unlocked").value(false))
                .andExpect(jsonPath("$.data.shop.level").value(0))
                .andExpect(jsonPath("$.data.shop.canUnlock").value(false))
                .andExpect(jsonPath("$.data.shop.unlockHint").exists());
    }

    @Test
    void islandLevel7CannotUnlock() throws Exception {
        String token = initPlayer(USER_A, 7, 10000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("岛屿等级不足")));
    }

    @Test
    void islandLevel8ButGoldNotEnough() throws Exception {
        String token = initPlayer(USER_A, 8, 4000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("金币不足")));
    }

    // ========== 解锁成功 ==========

    @Test
    void unlockSuccessDeductsGoldAndSetsLevel() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(1));

        Long gold = jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        assertEquals(5000L, gold);

        Integer level = jdbcTemplate.queryForObject(
                "SELECT level FROM player_cake_shop WHERE player_id = ?", Integer.class, getPlayerId(USER_A));
        assertEquals(1, level);
    }

    @Test
    void unlockStatusShowsCorrectInfo() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cake-shop/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shop.unlocked").value(true))
                .andExpect(jsonPath("$.data.shop.level").value(1))
                .andExpect(jsonPath("$.data.shop.rackCapacity").value(8))
                .andExpect(jsonPath("$.data.shop.saleIntervalSeconds").value(480))
                .andExpect(jsonPath("$.data.shop.nextLevel.level").value(2))
                .andExpect(jsonPath("$.data.shop.nextLevel.upgradeGold").value(8000));
    }

    // ========== 重复解锁 ==========

    @Test
    void duplicateUnlockFails() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("已解锁")));

        // Gold only deducted once
        Long gold = jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        assertEquals(5000L, gold);
    }

    // ========== 升级 ==========

    @Test
    void upgradeFromLevel1To2() throws Exception {
        String token = initPlayer(USER_A, 9, 20000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/cake-shop/upgrade").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.level").value(2));

        Long gold = jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        // 20000 - 5000 (unlock) - 8000 (upgrade) = 7000
        assertEquals(7000L, gold);
    }

    @Test
    void upgradeIslandLevelNotEnough() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Level 9 requires island level 9
        mockMvc.perform(post("/cake-shop/upgrade").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("岛屿等级不足")));
    }

    @Test
    void upgradeGoldNotEnough() throws Exception {
        String token = initPlayer(USER_A, 9, 12000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 12000 - 5000 = 7000, upgrade needs 8000
        mockMvc.perform(post("/cake-shop/upgrade").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("金币不足")));
    }

    @Test
    void upgradeWithoutUnlockFails() throws Exception {
        String token = initPlayer(USER_A, 10, 100000);
        mockMvc.perform(post("/cake-shop/upgrade").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("请先解锁")));
    }

    @Test
    void maxLevelCannotUpgrade() throws Exception {
        // Set player to island level 17 with plenty of gold
        String token = initPlayer(USER_A, 17, 1000000);

        // Unlock and upgrade to level 10 directly via DB for efficiency
        Long playerId = getPlayerId(USER_A);
        jdbcTemplate.update("DELETE FROM player_cake_shop WHERE player_id = ?", playerId);
        jdbcTemplate.update("INSERT INTO player_cake_shop (player_id, level) VALUES (?, 10)", playerId);

        mockMvc.perform(post("/cake-shop/upgrade").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("最高等级")));
    }

    @Test
    void upgradePathLevel1To10AllLevels() throws Exception {
        String token = initPlayer(USER_A, 17, 1000000);

        // Unlock
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.level").value(1));

        // Upgrade 2 through 10
        for (int lv = 2; lv <= 10; lv++) {
            mockMvc.perform(post("/cake-shop/upgrade").header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.level").value(lv));
        }

        // Verify final level
        Integer level = jdbcTemplate.queryForObject(
                "SELECT level FROM player_cake_shop WHERE player_id = ?", Integer.class, getPlayerId(USER_A));
        assertEquals(10, level);

        // Verify final rack capacity and sale interval
        mockMvc.perform(get("/cake-shop/status").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.shop.rackCapacity").value(15))
                .andExpect(jsonPath("$.data.shop.saleIntervalSeconds").value(360));
    }

    @Test
    void allLevelsReturnedInStatus() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);
        mockMvc.perform(get("/cake-shop/status").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.shop.allLevels.length()").value(10))
                .andExpect(jsonPath("$.data.shop.allLevels[0].level").value(1))
                .andExpect(jsonPath("$.data.shop.allLevels[9].level").value(10));
    }

    // ========== 玩家隔离 ==========

    @Test
    void playerIsolation() throws Exception {
        String tokenA = initPlayer(USER_A, 8, 10000);
        String tokenB = initPlayer(USER_B, 8, 10000);

        // A unlocks
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data.level").value(1));

        // B is still locked
        mockMvc.perform(get("/cake-shop/status").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data.shop.unlocked").value(false))
                .andExpect(jsonPath("$.data.shop.level").value(0));

        // B unlocks independently
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data.level").value(1));

        // A upgrades
        jdbcTemplate.update("UPDATE game_player SET level = 10, gold = 100000 WHERE user_id = ?", USER_A);
        mockMvc.perform(post("/cake-shop/upgrade").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data.level").value(2));

        // B still level 1
        mockMvc.perform(get("/cake-shop/status").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data.shop.level").value(1));
    }

    // ========== canUnlock ==========

    @Test
    void canUnlockTrueAtIslandLevel8() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);
        mockMvc.perform(get("/cake-shop/status").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.shop.canUnlock").value(true))
                .andExpect(jsonPath("$.data.shop.unlockHint").value(org.hamcrest.Matchers.containsString("5000")));
    }

    // ========== 蛋糕制作 ==========

    @Test
    void craftWithoutUnlockFails() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);
        String body = "{\"recipeId\":\"strawberry_cake\",\"quantity\":1}";
        mockMvc.perform(post("/cake-shop/craft")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("请先解锁")));
    }

    @Test
    void craftWithoutRecipeQualificationFails() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        // Unlock cake shop
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        // Delete recipe qualifications (game/init grants them based on island level)
        jdbcTemplate.update("DELETE FROM player_recipe WHERE player_id = ?", getPlayerId(USER_A));

        String body = "{\"recipeId\":\"strawberry_cake\",\"quantity\":1}";
        mockMvc.perform(post("/cake-shop/craft")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("尚未获得该配方")));
    }

    @Test
    void craftWithInsufficientMaterialsFails() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Player has strawberry_cake recipe (from island level 8 reward) but no materials
        String body = "{\"recipeId\":\"strawberry_cake\",\"quantity\":1}";
        mockMvc.perform(post("/cake-shop/craft")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("材料不足")));
    }

    @Test
    void craftSuccessDeductsMaterialsAndAddsOutput() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        Long playerId = getPlayerId(USER_A);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Give player materials: strawberry x3, wheat x3, egg x2, milk x1
        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry', 10)", playerId);
        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'wheat', 10)", playerId);
        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'egg', 5)", playerId);
        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'milk', 5)", playerId);

        String body = "{\"recipeId\":\"strawberry_cake\",\"quantity\":2}";
        mockMvc.perform(post("/cake-shop/craft")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.recipeId").value("strawberry_cake"))
                .andExpect(jsonPath("$.data.outputItem").value("strawberry_cake"))
                .andExpect(jsonPath("$.data.outputCount").value(2));

        // Verify materials deducted
        Integer strawberryCount = jdbcTemplate.queryForObject(
                "SELECT count FROM inventory WHERE player_id = ? AND item_id = 'strawberry'", Integer.class, playerId);
        assertEquals(4, strawberryCount); // 10 - 3*2 = 4

        Integer wheatCount = jdbcTemplate.queryForObject(
                "SELECT count FROM inventory WHERE player_id = ? AND item_id = 'wheat'", Integer.class, playerId);
        assertEquals(4, wheatCount); // 10 - 3*2 = 4

        Integer eggCount = jdbcTemplate.queryForObject(
                "SELECT count FROM inventory WHERE player_id = ? AND item_id = 'egg'", Integer.class, playerId);
        assertEquals(1, eggCount); // 5 - 2*2 = 1

        Integer milkCount = jdbcTemplate.queryForObject(
                "SELECT count FROM inventory WHERE player_id = ? AND item_id = 'milk'", Integer.class, playerId);
        assertEquals(3, milkCount); // 5 - 1*2 = 3

        // Verify output added
        Integer cakeCount = jdbcTemplate.queryForObject(
                "SELECT count FROM inventory WHERE player_id = ? AND item_id = 'strawberry_cake'", Integer.class, playerId);
        assertEquals(2, cakeCount);
    }

    @Test
    void craftDrinkRecipeFails() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String body = "{\"recipeId\":\"strawberry_juice\",\"quantity\":1}";
        mockMvc.perform(post("/cake-shop/craft")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("不在蛋糕店制作")));
    }

    @Test
    void craftQuantityBoundary() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // quantity 0
        String body0 = "{\"recipeId\":\"strawberry_cake\",\"quantity\":0}";
        mockMvc.perform(post("/cake-shop/craft")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body0))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("1-99")));

        // quantity 100
        String body100 = "{\"recipeId\":\"strawberry_cake\",\"quantity\":100}";
        mockMvc.perform(post("/cake-shop/craft")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body100))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("1-99")));
    }

    @Test
    void recipesEndpointReturnsCraftableList() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        Long playerId = getPlayerId(USER_A);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Give some materials
        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry', 6)", playerId);
        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'wheat', 6)", playerId);
        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'egg', 4)", playerId);
        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'milk', 2)", playerId);

        mockMvc.perform(get("/cake-shop/recipes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].recipeId").value("strawberry_cake"))
                .andExpect(jsonPath("$.data[0].maxCraftable").value(2)); // 6/3=2, 4/2=2, 2/1=2
    }

    // ========== 蛋糕架 ==========

    @Test
    void racksInitiallyEmpty() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);
        mockMvc.perform(get("/cake-shop/racks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].slot").value(1))
                .andExpect(jsonPath("$.data[0].status").value("EMPTY"))
                .andExpect(jsonPath("$.data[1].slot").value(2))
                .andExpect(jsonPath("$.data[1].status").value("EMPTY"));
    }

    @Test
    void listCakeWithoutUnlockFails() throws Exception {
        String token = initPlayer(USER_A, 8, 10000);
        String body = "{\"recipeId\":\"strawberry_cake\",\"quantity\":1}";
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("请先解锁")));
    }

    @Test
    void listCakeSuccess() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        Long playerId = getPlayerId(USER_A);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Give player cakes
        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry_cake', 5)", playerId);

        String body = "{\"recipeId\":\"strawberry_cake\",\"quantity\":3}";
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.slot").value(1))
                .andExpect(jsonPath("$.data.status").value("SELLING"))
                .andExpect(jsonPath("$.data.recipeId").value("strawberry_cake"))
                .andExpect(jsonPath("$.data.quantity").value(3))
                .andExpect(jsonPath("$.data.sold").value(0))
                .andExpect(jsonPath("$.data.remaining").value(3));

        // Verify inventory deducted
        Integer cakeCount = jdbcTemplate.queryForObject(
                "SELECT count FROM inventory WHERE player_id = ? AND item_id = 'strawberry_cake'", Integer.class, playerId);
        assertEquals(2, cakeCount); // 5 - 3 = 2
    }

    @Test
    void listCakeExceedsCapacityFails() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        Long playerId = getPlayerId(USER_A);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry_cake', 100)", playerId);

        // Lv1 capacity = 8
        String body = "{\"recipeId\":\"strawberry_cake\",\"quantity\":9}";
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("超过单架上限")));
    }

    @Test
    void listOnOccupiedRackFails() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        Long playerId = getPlayerId(USER_A);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry_cake', 10)", playerId);

        // List on slot 1
        String body = "{\"recipeId\":\"strawberry_cake\",\"quantity\":3}";
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(jsonPath("$.code").value(0));

        // Try to list again on slot 1
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("使用中")));

        // Slot 2 should still work
        mockMvc.perform(post("/cake-shop/racks/2/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void listCakeInsufficientInventoryFails() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        Long playerId = getPlayerId(USER_A);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Only 2 cakes in inventory
        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry_cake', 2)", playerId);

        String body = "{\"recipeId\":\"strawberry_cake\",\"quantity\":3}";
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("库存不足")));

        // Verify nothing was deducted
        Integer cakeCount = jdbcTemplate.queryForObject(
                "SELECT count FROM inventory WHERE player_id = ? AND item_id = 'strawberry_cake'", Integer.class, playerId);
        assertEquals(2, cakeCount);
    }

    @Test
    void rackStatusShowsSellingInfo() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        Long playerId = getPlayerId(USER_A);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry_cake', 5)", playerId);

        String body = "{\"recipeId\":\"strawberry_cake\",\"quantity\":3}";
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk());

        // Check rack status
        mockMvc.perform(get("/cake-shop/racks").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].status").value("SELLING"))
                .andExpect(jsonPath("$.data[0].quantity").value(3))
                .andExpect(jsonPath("$.data[0].sold").value(0))
                .andExpect(jsonPath("$.data[0].remaining").value(3))
                .andExpect(jsonPath("$.data[0].saleGoldPerItem").value(150))
                .andExpect(jsonPath("$.data[0].saleIntervalSeconds").value(480))
                .andExpect(jsonPath("$.data[0].totalGoldReward").value(450)); // 150*3
    }

    // ========== 下架与收取 ==========

    @Test
    void takeDownReturnsUnsoldAndSettlesEarned() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        Long playerId = getPlayerId(USER_A);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry_cake', 5)", playerId);

        // List 3 cakes
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"recipeId\":\"strawberry_cake\",\"quantity\":3}"))
                .andExpect(status().isOk());

        // Simulate time: 500s passed (1 sold at 480s interval)
        jdbcTemplate.update("UPDATE player_cake_rack SET list_time = TIMESTAMPADD(SECOND, -500, CURRENT_TIMESTAMP), last_settle_time = TIMESTAMPADD(SECOND, -500, CURRENT_TIMESTAMP) WHERE player_id = ? AND slot = 1", playerId);

        // Take down
        long goldBefore = jdbcTemplate.queryForObject("SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        mockMvc.perform(post("/cake-shop/racks/1/takedown").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("EMPTY"));

        // 1 cake sold = 150 gold earned
        long goldAfter = jdbcTemplate.queryForObject("SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        assertEquals(150, goldAfter - goldBefore);

        // 2 unsold cakes returned to inventory: 5 - 3 + 2 = 4
        Integer cakeCount = jdbcTemplate.queryForObject(
                "SELECT count FROM inventory WHERE player_id = ? AND item_id = 'strawberry_cake'", Integer.class, playerId);
        assertEquals(4, cakeCount);
    }

    @Test
    void collectSoldOutRack() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        Long playerId = getPlayerId(USER_A);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry_cake', 3)", playerId);

        // List 2 cakes
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"recipeId\":\"strawberry_cake\",\"quantity\":2}"))
                .andExpect(status().isOk());

        // Simulate enough time for both to sell (2 * 480 + 100 = 1060s)
        jdbcTemplate.update("UPDATE player_cake_rack SET list_time = TIMESTAMPADD(SECOND, -1060, CURRENT_TIMESTAMP), last_settle_time = TIMESTAMPADD(SECOND, -1060, CURRENT_TIMESTAMP) WHERE player_id = ? AND slot = 1", playerId);

        // Check it's SOLD_OUT
        mockMvc.perform(get("/cake-shop/racks").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].status").value("SOLD_OUT"))
                .andExpect(jsonPath("$.data[0].sold").value(2));

        // Collect
        long goldBefore = jdbcTemplate.queryForObject("SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        mockMvc.perform(post("/cake-shop/racks/1/collect").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("EMPTY"));

        // 2 cakes sold = 150 * 2 = 300 gold earned
        long goldAfter = jdbcTemplate.queryForObject("SELECT gold FROM game_player WHERE user_id = ?", Long.class, USER_A);
        assertEquals(300, goldAfter - goldBefore);
    }

    @Test
    void collectNotSoldOutFails() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        Long playerId = getPlayerId(USER_A);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry_cake', 5)", playerId);

        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"recipeId\":\"strawberry_cake\",\"quantity\":3}"))
                .andExpect(status().isOk());

        // No time passed, should still be SELLING
        mockMvc.perform(post("/cake-shop/racks/1/collect").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("尚未售罄")));
    }

    @Test
    void takeDownEmptyRackFails() throws Exception {
        String token = initPlayer(USER_A, 8, 100000);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/cake-shop/racks/1/takedown").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("为空")));
    }

    @Test
    void snapshotLockedAfterUpgrade() throws Exception {
        // List at Lv1 (480s interval), then upgrade to Lv3 (450s interval)
        // The existing batch should keep 480s, new batch should use 450s
        String token = initPlayer(USER_A, 10, 1000000);
        Long playerId = getPlayerId(USER_A);
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry_cake', 20)", playerId);

        // List at Lv1 → snapshot 480s
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"recipeId\":\"strawberry_cake\",\"quantity\":3}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cake-shop/racks").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].saleIntervalSeconds").value(480));

        // Upgrade to Lv2 and Lv3
        mockMvc.perform(post("/cake-shop/upgrade").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/cake-shop/upgrade").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Existing batch still uses 480s
        mockMvc.perform(get("/cake-shop/racks").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].saleIntervalSeconds").value(480));

        // Take down and re-list → new batch uses 450s
        jdbcTemplate.update("UPDATE player_cake_rack SET list_time = TIMESTAMPADD(SECOND, -10000, CURRENT_TIMESTAMP), last_settle_time = TIMESTAMPADD(SECOND, -10000, CURRENT_TIMESTAMP) WHERE player_id = ? AND slot = 1", playerId);
        mockMvc.perform(post("/cake-shop/racks/1/takedown").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"recipeId\":\"strawberry_cake\",\"quantity\":3}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cake-shop/racks").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].saleIntervalSeconds").value(450)); // Lv3 interval
    }

    @Test
    void rackPlayerIsolation() throws Exception {
        String tokenA = initPlayer(USER_A, 8, 100000);
        String tokenB = initPlayer(USER_B, 8, 100000);
        Long playerAId = getPlayerId(USER_A);
        Long playerBId = getPlayerId(USER_B);

        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(post("/cake-shop/unlock").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry_cake', 5)", playerAId);
        jdbcTemplate.update("INSERT INTO inventory (player_id, item_id, count) VALUES (?, 'strawberry_cake', 5)", playerBId);

        // A lists on slot 1
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json").content("{\"recipeId\":\"strawberry_cake\",\"quantity\":3}"))
                .andExpect(status().isOk());

        // B's rack 1 is still empty
        mockMvc.perform(get("/cake-shop/racks").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data[0].status").value("EMPTY"));

        // B can list on its own slot 1
        mockMvc.perform(post("/cake-shop/racks/1/list")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType("application/json").content("{\"recipeId\":\"strawberry_cake\",\"quantity\":2}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.quantity").value(2));
    }
}
