package com.fruitisland.game.controller;

import com.fruitisland.common.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Demo3.0 配方商店 HTTP 集成测试。
 *
 * 覆盖范围：
 *  - 获取配方列表（含购买状态）
 *  - 购买配方（扣金币 + 授予永久使用权）
 *  - 重复购买报错
 *  - 金币不足报错
 *  - 购买后配方进入 player_recipe（制作台可见）
 *  - 未登录拦截
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeShopHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final long USER_A = 8401L;
    private static final long USER_B = 8402L;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM player_recipe_purchase WHERE player_id IN " +
                "(SELECT id FROM game_player WHERE user_id IN (?, ?))", USER_A, USER_B);
        jdbcTemplate.update("DELETE FROM player_recipe WHERE player_id IN " +
                "(SELECT id FROM game_player WHERE user_id IN (?, ?))", USER_A, USER_B);
        jdbcTemplate.update("DELETE FROM game_player WHERE user_id IN (?, ?)", USER_A, USER_B);

        // Player A: 5000 gold (enough for all recipes except truffle_cake=2000 and truffle_cocoa=1500)
        jdbcTemplate.update("""
                INSERT INTO game_player (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (?, 'fruit_island', 'recipe-buyer', 1, 0, 0, 5000, 0)
                """, USER_A);

        // Player B: 100 gold (not enough for any recipe)
        jdbcTemplate.update("""
                INSERT INTO game_player (user_id, game_id, nickname, level, exp, cumulative_exp, gold, diamond)
                VALUES (?, 'fruit_island', 'poor-player', 1, 0, 0, 100, 0)
                """, USER_B);
    }

    private long playerId(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM game_player WHERE user_id = ?", Long.class, userId);
    }

    private long gold(long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT gold FROM game_player WHERE id = ?", Long.class, playerId);
    }

    // ========================================================================
    // 配方列表
    // ========================================================================

    @Test
    void listRecipesReturnsAllShopRecipes() throws Exception {
        String token = jwtUtils.generateToken(USER_A, "recipe-buyer");

        mockMvc.perform(get("/recipe-shop/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.playerGold").value(5000))
                .andExpect(jsonPath("$.data.recipes.length()").value(8))
                .andExpect(jsonPath("$.data.recipes[0].recipeId").value("mushroom_tea"))
                .andExpect(jsonPath("$.data.recipes[0].price").value(200))
                .andExpect(jsonPath("$.data.recipes[0].purchased").value(false))
                .andExpect(jsonPath("$.data.recipes[3].recipeId").value("truffle_cocoa"))
                .andExpect(jsonPath("$.data.recipes[3].price").value(1500));
    }

    @Test
    void listRecipesReflectsPurchaseStatus() throws Exception {
        long pid = playerId(USER_A);
        // Manually insert a purchase record
        jdbcTemplate.update(
                "INSERT INTO player_recipe_purchase (player_id, recipe_id, price_paid) VALUES (?, 'mushroom_tea', 200)",
                pid);
        // Also grant the recipe
        jdbcTemplate.update(
                "INSERT INTO player_recipe (player_id, recipe_id, qualification_type, unlock_source, unlock_time) " +
                        "VALUES (?, 'mushroom_tea', 'PERMANENT', 'RECIPE_SHOP', CURRENT_TIMESTAMP)", pid);

        String token = jwtUtils.generateToken(USER_A, "recipe-buyer");

        mockMvc.perform(get("/recipe-shop/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.recipes[0].recipeId").value("mushroom_tea"))
                .andExpect(jsonPath("$.data.recipes[0].purchased").value(true))
                .andExpect(jsonPath("$.data.recipes[1].purchased").value(false));
    }

    // ========================================================================
    // 购买配方
    // ========================================================================

    @Test
    void buyRecipeSuccess() throws Exception {
        String token = jwtUtils.generateToken(USER_A, "recipe-buyer");
        long pid = playerId(USER_A);

        mockMvc.perform(post("/recipe-shop/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"recipeId\":\"mushroom_tea\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.recipeId").value("mushroom_tea"))
                .andExpect(jsonPath("$.data.purchased").value(true));

        // Verify gold deducted
        assertEqualsLong(4800, gold(pid)); // 5000 - 200

        // Verify purchase record exists
        Long purchaseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_recipe_purchase WHERE player_id = ? AND recipe_id = 'mushroom_tea'",
                Long.class, pid);
        assertEqualsLong(1, purchaseCount);

        // Verify recipe granted to player_recipe
        Long recipeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_recipe WHERE player_id = ? AND recipe_id = 'mushroom_tea'",
                Long.class, pid);
        assertEqualsLong(1, recipeCount);
    }

    @Test
    void buyRecipeDeductsCorrectAmount() throws Exception {
        String token = jwtUtils.generateToken(USER_A, "recipe-buyer");
        long pid = playerId(USER_A);

        mockMvc.perform(post("/recipe-shop/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"recipeId\":\"truffle_cocoa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 5000 - 1500 = 3500
        assertEqualsLong(3500, gold(pid));
    }

    @Test
    void duplicatePurchaseFails() throws Exception {
        String token = jwtUtils.generateToken(USER_A, "recipe-buyer");

        // First purchase succeeds
        mockMvc.perform(post("/recipe-shop/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"recipeId\":\"mushroom_tea\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Second purchase fails
        mockMvc.perform(post("/recipe-shop/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"recipeId\":\"mushroom_tea\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("已购买过该配方"));
    }

    @Test
    void insufficientGoldFails() throws Exception {
        String token = jwtUtils.generateToken(USER_B, "poor-player");

        mockMvc.perform(post("/recipe-shop/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"recipeId\":\"mushroom_tea\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("金币不足，需要 200"));
    }

    @Test
    void buyNonExistentRecipeFails() throws Exception {
        String token = jwtUtils.generateToken(USER_A, "recipe-buyer");

        mockMvc.perform(post("/recipe-shop/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"recipeId\":\"nonexistent_recipe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1));
    }

    // ========================================================================
    // 玩家隔离
    // ========================================================================

    @Test
    void purchaseIsPlayerSpecific() throws Exception {
        String tokenA = jwtUtils.generateToken(USER_A, "recipe-buyer");
        String tokenB = jwtUtils.generateToken(USER_B, "poor-player");

        // Player A buys mushroom_tea
        mockMvc.perform(post("/recipe-shop/buy")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content("{\"recipeId\":\"mushroom_tea\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Player B sees mushroom_tea as not purchased
        mockMvc.perform(get("/recipe-shop/list")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes[0].recipeId").value("mushroom_tea"))
                .andExpect(jsonPath("$.data.recipes[0].purchased").value(false));
    }

    // ========================================================================
    // 未登录拦截
    // ========================================================================

    @Test
    void unauthorizedRequestBlocked() throws Exception {
        mockMvc.perform(get("/recipe-shop/list"))
                .andExpect(status().isUnauthorized());
    }

    private void assertEqualsLong(long expected, long actual) {
        assertTrue(expected == actual,
                "Expected " + expected + " but got " + actual);
    }
}
