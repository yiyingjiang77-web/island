package com.fruitisland.game.service.impl;

import com.fruitisland.game.dto.ExpGainResult;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.IslandLevelConfig;
import com.fruitisland.game.mapper.GamePlayerMapper;
import com.fruitisland.game.mapper.IslandLevelConfigMapper;
import com.fruitisland.game.mapper.PlayerIslandLevelRewardClaimMapper;
import com.fruitisland.game.service.PlayerCropService;
import com.fruitisland.game.service.PlayerRecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GamePlayerServiceImplTest {

    private GamePlayerMapper mapper;
    private IslandLevelConfigMapper levelConfigMapper;
    private PlayerIslandLevelRewardClaimMapper claimMapper;
    private PlayerCropService playerCropService;
    private PlayerRecipeService playerRecipeService;
    private GamePlayerServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(GamePlayerMapper.class);
        levelConfigMapper = mock(IslandLevelConfigMapper.class);
        claimMapper = mock(PlayerIslandLevelRewardClaimMapper.class);
        playerCropService = mock(PlayerCropService.class);
        playerRecipeService = mock(PlayerRecipeService.class);
        service = new GamePlayerServiceImpl(
                levelConfigMapper, claimMapper, playerCropService, playerRecipeService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.updateById(any(GamePlayer.class))).thenReturn(1);
    }

    @Test
    void addExpUsesCumulativeExpAndDoesNotSubtractThreshold() {
        GamePlayer player = new GamePlayer();
        player.setId(1L);
        player.setLevel(1);
        player.setExp(90);
        player.setCumulativeExp(90);
        player.setGold(500L);
        when(mapper.selectForUpdate(1L)).thenReturn(player);
        when(levelConfigMapper.selectList(any())).thenReturn(List.of(
                config(1, 0, "strawberry", "strawberry_juice"),
                config(2, 100, "carrot", "carrot_juice"),
                config(3, 250, "orange", "orange_juice")));

        ExpGainResult result = service.addExp(1L, 15);

        assertEquals(2, player.getLevel());
        assertEquals(105, player.getCumulativeExp());
        assertEquals(5, player.getExp());
        assertEquals(1, result.getLevelsGained());
        assertEquals(105, result.getCumulativeExp());
        assertEquals(250, result.getNextLevelThreshold());
        verify(playerCropService).grantPermanent(1L, "carrot", "ISLAND_LEVEL_REWARD");
        verify(playerRecipeService).grantPermanent(1L, "carrot_juice", "ISLAND_LEVEL_REWARD");
    }

    @Test
    void settleDrinkSaleRewardLocksPlayerAndSettlesGoldAndCumulativeExp() {
        GamePlayer player = new GamePlayer();
        player.setId(1L);
        player.setLevel(1);
        player.setExp(0);
        player.setCumulativeExp(0);
        player.setGold(500L);
        when(mapper.selectForUpdate(1L)).thenReturn(player);
        when(levelConfigMapper.selectList(any())).thenReturn(List.of(
                config(1, 0, "strawberry", "strawberry_juice"),
                config(2, 100, "carrot", "carrot_juice")));

        ExpGainResult result = service.settleDrinkSaleReward(1L, 30, 20);

        assertEquals(530L, player.getGold());
        assertEquals(20, player.getCumulativeExp());
        assertEquals(1, player.getLevel());
        assertEquals(0, result.getLevelsGained());
        verify(mapper).selectForUpdate(1L);
        verify(mapper).updateById(player);
    }

    @Test
    void oneLargeGainCrossesMultipleLevelsAndGrantsAllRewards() {
        GamePlayer player = new GamePlayer();
        player.setId(1L);
        player.setLevel(1);
        player.setExp(0);
        player.setCumulativeExp(0);
        player.setGold(500L);
        when(mapper.selectForUpdate(1L)).thenReturn(player);
        when(levelConfigMapper.selectList(any())).thenReturn(List.of(
                config(1, 0, "strawberry", "strawberry_juice"),
                config(2, 100, "carrot", "carrot_juice"),
                config(3, 250, "orange", "orange_juice")));

        ExpGainResult result = service.addExp(1L, 260);

        assertEquals(3, player.getLevel());
        assertEquals(260, player.getCumulativeExp());
        assertEquals(2, result.getLevelsGained());
        verify(playerCropService).grantPermanent(1L, "strawberry", "ISLAND_LEVEL_REWARD");
        verify(playerCropService).grantPermanent(1L, "carrot", "ISLAND_LEVEL_REWARD");
        verify(playerCropService).grantPermanent(1L, "orange", "ISLAND_LEVEL_REWARD");
        verify(playerRecipeService).grantPermanent(1L, "strawberry_juice", "ISLAND_LEVEL_REWARD");
        verify(playerRecipeService).grantPermanent(1L, "carrot_juice", "ISLAND_LEVEL_REWARD");
        verify(playerRecipeService).grantPermanent(1L, "orange_juice", "ISLAND_LEVEL_REWARD");
    }

    private IslandLevelConfig config(int level, int exp, String cropId, String recipeId) {
        IslandLevelConfig config = new IslandLevelConfig();
        config.setLevel(level);
        config.setCumulativeExp(exp);
        config.setCropId(cropId);
        config.setRecipeId(recipeId);
        config.setEnabled(1);
        return config;
    }
}
