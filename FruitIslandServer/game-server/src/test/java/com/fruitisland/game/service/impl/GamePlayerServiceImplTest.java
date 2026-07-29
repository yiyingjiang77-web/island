package com.fruitisland.game.service.impl;

import com.fruitisland.game.dto.ExpGainResult;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerLevelConfig;
import com.fruitisland.game.mapper.GamePlayerMapper;
import com.fruitisland.game.service.PlayerLevelConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GamePlayerServiceImplTest {

    private GamePlayerMapper mapper;
    private PlayerLevelConfigService levelConfigService;
    private GamePlayerServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(GamePlayerMapper.class);
        levelConfigService = mock(PlayerLevelConfigService.class);
        service = new GamePlayerServiceImpl(levelConfigService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.updateById(any(GamePlayer.class))).thenReturn(1);
    }

    @Test
    void addExpSupportsMultipleLevelUpsAndRewardsGold() {
        GamePlayer player = new GamePlayer();
        player.setId(1L);
        player.setLevel(1);
        player.setExp(90);
        player.setGold(500L);
        when(mapper.selectById(1L)).thenReturn(player);
        when(levelConfigService.findByLevel(1)).thenReturn(level(1, 100, 50));
        when(levelConfigService.findByLevel(2)).thenReturn(level(2, 150, 75));
        when(levelConfigService.findByLevel(3)).thenReturn(level(3, 220, 100));

        ExpGainResult result = service.addExp(1L, 200);

        assertEquals(3, player.getLevel());
        assertEquals(40, player.getExp());
        assertEquals(625L, player.getGold());
        assertEquals(2, result.getLevelsGained());
        assertEquals(220, result.getNextLevelExp());
        verify(mapper).updateById(player);
    }

    private PlayerLevelConfig level(int level, int requiredExp, long rewardGold) {
        PlayerLevelConfig config = new PlayerLevelConfig();
        config.setLevel(level);
        config.setRequiredExp(requiredExp);
        config.setRewardGold(rewardGold);
        return config;
    }
}
