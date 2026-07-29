package com.fruitisland.game.service.impl;

import com.fruitisland.game.entity.CropConfig;
import com.fruitisland.game.entity.CropLevelConfig;
import com.fruitisland.game.mapper.PlayerCropGrantMapper;
import com.fruitisland.game.service.CropConfigService;
import com.fruitisland.game.service.CropLevelConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlayerCropGrantServiceImplTest {

    private PlayerCropGrantMapper mapper;
    private CropConfigService cropConfigService;
    private CropLevelConfigService levelConfigService;
    private PlayerCropGrantServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(PlayerCropGrantMapper.class);
        cropConfigService = mock(CropConfigService.class);
        levelConfigService = mock(CropLevelConfigService.class);
        service = new PlayerCropGrantServiceImpl(cropConfigService, levelConfigService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(levelConfigService.findByCropAndLevel(anyString(), eq(1)))
                .thenReturn(new CropLevelConfig());
    }

    @Test
    void commonCropCannotBeIssuedAsTemporaryReward() {
        CropConfig common = crop("strawberry", "COMMON", 0);
        when(cropConfigService.findByCropId("strawberry")).thenReturn(common);

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> service.grantRareCrop(
                        1L, "strawberry", 1, 3600, "QUEST", "q1")
        );

        assertEquals("只有允许奖励的稀有作物才能发放", error.getMessage());
        verify(mapper, never()).insert(any());
    }

    @Test
    void eligibleRareCropCanBeIssuedForLimitedTime() {
        CropConfig rare = crop("moonberry", "RARE", 1);
        when(cropConfigService.findByCropId("moonberry")).thenReturn(rare);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any())).thenReturn(1);

        var grant = service.grantRareCrop(
                1L, "moonberry", 1, 3600, "QUEST", "q1");

        assertEquals("ACTIVE", grant.getStatus());
        assertEquals("moonberry", grant.getCropId());
        assertTrue(grant.getValidUntil().isAfter(grant.getValidFrom()));
    }

    private CropConfig crop(String cropId, String rarity, int rewardEligible) {
        CropConfig config = new CropConfig();
        config.setCropId(cropId);
        config.setRarity(rarity);
        config.setRewardEligible(rewardEligible);
        config.setMaxCropLevel(1);
        config.setEnabled(1);
        return config;
    }
}
