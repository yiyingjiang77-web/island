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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DrinkShopRenovationHttpTest {
    @Autowired MockMvc mockMvc;
    @Autowired JwtUtils jwtUtils;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM player_drink_shop");
        jdbc.update("DELETE FROM game_player WHERE user_id IN (7401,7402)");
        jdbc.update("INSERT INTO game_player (user_id,game_id,nickname,level,exp,cumulative_exp,gold,diamond) VALUES (7401,'fruit_island','装修岛主',2,0,100,1000,20)");
        jdbc.update("INSERT INTO game_player (user_id,game_id,nickname,level,exp,cumulative_exp,gold,diamond) VALUES (7402,'fruit_island','其他岛主',1,0,0,1000,20)");
    }

    @Test
    void jwtReadInitializesLevelOneAndReturnsCurrentAndNextOperatingConfig() throws Exception {
        mockMvc.perform(get("/drink-shop/progress").header("Authorization", bearer(7401)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.currentLevel").value(1))
                .andExpect(jsonPath("$.data.currentConfig.queueCapacity").value(5))
                .andExpect(jsonPath("$.data.currentConfig.barCapacity").value(10))
                .andExpect(jsonPath("$.data.nextConfig.level").value(2))
                .andExpect(jsonPath("$.data.nextConfig.renovationGold").value(500))
                .andExpect(jsonPath("$.data.islandLevelMet").value(true));
        assertEquals(6, jdbc.queryForObject("SELECT COUNT(*) FROM drink_bar WHERE player_id=(SELECT id FROM game_player WHERE user_id=7401)", Integer.class));
    }

    @Test
    void renovationUsesJwtPlayerAndAtomicallyDeductsConfiguredGold() throws Exception {
        mockMvc.perform(post("/drink-shop/renovate")
                        .header("Authorization", bearer(7401)).contentType("application/json")
                        .content("{\"targetLevel\":2,\"playerId\":999999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.previousLevel").value(1))
                .andExpect(jsonPath("$.data.currentLevel").value(2))
                .andExpect(jsonPath("$.data.goldSpent").value(500))
                .andExpect(jsonPath("$.data.remainingGold").value(500));
        assertEquals(500L, jdbc.queryForObject("SELECT gold FROM game_player WHERE user_id=7401", Long.class));
        assertEquals(1000L, jdbc.queryForObject("SELECT gold FROM game_player WHERE user_id=7402", Long.class));
    }

    @Test
    void jumpLevelAndInsufficientIslandLevelDoNotChangeAssets() throws Exception {
        mockMvc.perform(post("/drink-shop/renovate").header("Authorization", bearer(7401))
                        .contentType("application/json").content("{\"targetLevel\":3}"))
                .andExpect(jsonPath("$.code").value(-1));
        mockMvc.perform(post("/drink-shop/renovate").header("Authorization", bearer(7402))
                        .contentType("application/json").content("{\"targetLevel\":2}"))
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("小岛等级不足，需要达到 2 级"));
        assertEquals(1000L, jdbc.queryForObject("SELECT gold FROM game_player WHERE user_id=7401", Long.class));
        assertEquals(1000L, jdbc.queryForObject("SELECT gold FROM game_player WHERE user_id=7402", Long.class));
    }

    private String bearer(long userId) { return "Bearer " + jwtUtils.generateToken(userId, "renovation-" + userId); }
}
