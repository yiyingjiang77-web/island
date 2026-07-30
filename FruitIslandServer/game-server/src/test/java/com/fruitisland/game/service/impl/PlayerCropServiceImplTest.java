package com.fruitisland.game.service.impl;

import com.fruitisland.game.entity.CropConfig;
import com.fruitisland.game.entity.CropLevelConfig;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerCrop;
import com.fruitisland.game.mapper.PlayerCropMapper;
import com.fruitisland.game.service.CropConfigService;
import com.fruitisland.game.service.CropLevelConfigService;
import com.fruitisland.game.service.GamePlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlayerCropServiceImplTest {

    private PlayerCropMapper mapper;
    private CropConfigService cropConfigService;
    private CropLevelConfigService levelConfigService;
    private GamePlayerService playerService;
    private PlayerCropServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(PlayerCropMapper.class);
        cropConfigService = mock(CropConfigService.class);
        levelConfigService = mock(CropLevelConfigService.class);
        playerService = mock(GamePlayerService.class);
        service = new PlayerCropServiceImpl(
                cropConfigService, levelConfigService, playerService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.updateById(any(PlayerCrop.class))).thenReturn(1);
    }

    @Test
    void upgradeDeductsConfiguredGoldAndRaisesOneLevel() {
        PlayerCrop crop = new PlayerCrop();
        crop.setId(1L);
        crop.setPlayerId(8L);
        crop.setCropId("strawberry");
        crop.setCropLevel(1);
        when(mapper.selectOne(any())).thenReturn(crop);

        CropConfig base = new CropConfig();
        base.setCropId("strawberry");
        base.setEnabled(1);
        base.setUpgradeEnabled(1);
        base.setMaxCropLevel(3);
        when(cropConfigService.findByCropId("strawberry")).thenReturn(base);

        CropLevelConfig target = new CropLevelConfig();
        target.setCropId("strawberry");
        target.setCropLevel(2);
        target.setUpgradeGold(200L);
        when(levelConfigService.findByCropAndLevel("strawberry", 2)).thenReturn(target);

        GamePlayer player = new GamePlayer();
        player.setId(8L);
        player.setGold(500L);
        when(playerService.getById(8L)).thenReturn(player);

        PlayerCrop result = service.upgrade(8L, "strawberry");

        assertEquals(2, result.getCropLevel());
        assertEquals(300L, player.getGold());
        verify(playerService).updateById(player);
        verify(mapper).updateById(crop);
    }
}
