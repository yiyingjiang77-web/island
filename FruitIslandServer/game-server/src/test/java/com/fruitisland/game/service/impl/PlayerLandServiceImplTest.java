package com.fruitisland.game.service.impl;

import com.fruitisland.game.entity.*;
import com.fruitisland.game.dto.ExpGainResult;
import com.fruitisland.game.dto.HarvestResultVO;
import com.fruitisland.game.mapper.PlayerLandMapper;
import com.fruitisland.game.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 土地种植核心规则测试。
 */
class PlayerLandServiceImplTest {

    private PlayerLandMapper playerLandMapper;
    private InventoryService inventoryService;
    private CropPlantService cropPlantService;
    private CropConfigService cropConfigService;
    private CropLevelConfigService cropLevelConfigService;
    private PlayerCropService playerCropService;
    private PlayerCropGrantService playerCropGrantService;
    private GamePlayerService gamePlayerService;
    private FlowerConfigService flowerConfigService;
    private FlowerLevelConfigService flowerLevelConfigService;
    private PlayerFlowerRightService playerFlowerRightService;
    private PlayerLandServiceImpl service;

    @BeforeEach
    void setUp() {
        playerLandMapper = mock(PlayerLandMapper.class);
        inventoryService = mock(InventoryService.class);
        cropPlantService = mock(CropPlantService.class);
        cropConfigService = mock(CropConfigService.class);
        cropLevelConfigService = mock(CropLevelConfigService.class);
        playerCropService = mock(PlayerCropService.class);
        playerCropGrantService = mock(PlayerCropGrantService.class);
        gamePlayerService = mock(GamePlayerService.class);
        flowerConfigService = mock(FlowerConfigService.class);
        flowerLevelConfigService = mock(FlowerLevelConfigService.class);
        playerFlowerRightService = mock(PlayerFlowerRightService.class);

        service = new PlayerLandServiceImpl(
                mock(LandConfigService.class),
                gamePlayerService,
                inventoryService,
                cropPlantService,
                cropConfigService,
                cropLevelConfigService,
                playerCropService,
                playerCropGrantService,
                flowerConfigService,
                flowerLevelConfigService,
                playerFlowerRightService
        );
        ReflectionTestUtils.setField(service, "baseMapper", playerLandMapper);
        when(playerLandMapper.updateById(any(PlayerLand.class))).thenReturn(1);
        when(cropConfigService.findByCropId("strawberry"))
                .thenReturn(cropConfig("strawberry", 1));
        when(cropLevelConfigService.findByCropAndLevel("strawberry", 2))
                .thenReturn(levelConfig("strawberry", 2, 50, 3));

        GamePlayer player = new GamePlayer();
        player.setId(1L);
        player.setLevel(5);
        when(gamePlayerService.getById(1L)).thenReturn(player);
        when(gamePlayerService.addExp(eq(1L), anyInt()))
                .thenAnswer(invocation -> new ExpGainResult(
                        invocation.getArgument(1),
                        5, 5, 20, 400, 0, null
                ));
    }

    @Test
    void permanentCropPlantsWithoutConsumingInventoryAndSavesSnapshot() {
        when(playerLandMapper.selectById(10L)).thenReturn(emptyLand());
        when(playerCropService.findByPlayerAndCrop(1L, "strawberry"))
                .thenReturn(playerCrop("strawberry", 2));

        PlayerLand result = service.plant(1L, 10L, "strawberry");

        assertEquals("PLANTED", result.getStatus());
        assertEquals(2, result.getCropLevel());
        assertEquals(50, result.getGrowSecondsSnapshot());
        assertEquals(3, result.getYieldCountSnapshot());
        assertEquals(8, result.getHarvestExpSnapshot());
        assertEquals("PERMANENT", result.getAccessType());
        assertNull(result.getAccessGrantId());
        assertNull(result.getFinishTime());
        assertEquals(0, result.getWaterLevel());

        // 永久种植权可无限种植，不读取也不扣减背包种子。
        verify(inventoryService, never()).findByPlayerAndItem(anyLong(), anyString());
        verify(inventoryService, never()).updateById(any());
        verify(cropPlantService).save(argThat(record ->
                record.getCropLevel() == 2
                        && record.getYieldCountSnapshot() == 3
                        && "PERMANENT".equals(record.getAccessType())
                        && "WAITING_WATER".equals(record.getStatus())));
    }

    @Test
    void activeRareGrantAllowsTemporaryPlanting() {
        when(playerLandMapper.selectById(10L)).thenReturn(emptyLand());
        when(playerCropService.findByPlayerAndCrop(1L, "strawberry")).thenReturn(null);

        PlayerCropGrant grant = new PlayerCropGrant();
        grant.setId(99L);
        grant.setCropId("strawberry");
        grant.setGrantCropLevel(2);
        when(playerCropGrantService.findActiveGrant(eq(1L), eq("strawberry"), any()))
                .thenReturn(grant);

        PlayerLand result = service.plant(1L, 10L, "strawberry");

        assertEquals("TEMPORARY", result.getAccessType());
        assertEquals(99L, result.getAccessGrantId());
        assertEquals(2, result.getCropLevel());
    }

    @Test
    void plantingWithoutPermanentOrTemporaryAccessIsRejected() {
        when(playerLandMapper.selectById(10L)).thenReturn(emptyLand());

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> service.plant(1L, 10L, "strawberry")
        );

        assertEquals("尚未获得该作物的种植权限", error.getMessage());
    }

    @Test
    void firstWaterUsesPlantingSnapshotAndSecondWaterIsRejected() {
        PlayerLand land = emptyLand();
        land.setStatus("PLANTED");
        land.setCropId("strawberry");
        land.setGrowSecondsSnapshot(50);
        land.setYieldCountSnapshot(3);
        land.setWaterLevel(0);

        CropPlant cropRecord = new CropPlant();
        cropRecord.setStatus("WAITING_WATER");
        when(playerLandMapper.selectById(10L)).thenReturn(land);
        when(cropPlantService.findLatestByPlayerLand(10L)).thenReturn(cropRecord);

        LocalDateTime before = LocalDateTime.now();
        PlayerLand result = service.water(1L, 10L);

        assertFalse(result.getFinishTime().isBefore(before.plusSeconds(49)));
        assertEquals("GROWING", cropRecord.getStatus());
        verify(cropLevelConfigService, never()).findByCropAndLevel(anyString(), anyInt());

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> service.water(1L, 10L)
        );
        assertEquals("这轮作物已经浇过水了", error.getMessage());
    }

    @Test
    void harvestUsesSnapshotEvenIfTemporaryGrantHasExpired() {
        PlayerLand land = emptyLand();
        land.setStatus("READY");
        land.setCropId("strawberry");
        land.setCropLevel(2);
        land.setYieldCountSnapshot(7);
        land.setHarvestExpSnapshot(11);
        land.setAccessType("TEMPORARY");
        land.setAccessGrantId(99L);

        CropPlant cropRecord = new CropPlant();
        cropRecord.setStatus("GROWING");
        when(playerLandMapper.selectById(10L)).thenReturn(land);
        when(cropPlantService.findLatestByPlayerLand(10L)).thenReturn(cropRecord);

        HarvestResultVO result = service.harvest(1L, 10L);

        verify(inventoryService).addItem(1L, "strawberry", 7);
        verify(gamePlayerService).addExp(1L, 11);
        verify(playerCropGrantService, never()).findActiveGrant(anyLong(), anyString(), any());
        assertEquals("EMPTY", land.getStatus());
        assertNull(land.getCropId());
        assertNull(land.getAccessGrantId());
        assertEquals("HARVESTED", cropRecord.getStatus());
        assertEquals(11, result.getExpGained());
    }

    @Test
    void playerLevelMustReachCropUnlockLevel() {
        when(cropConfigService.findByCropId("carrot"))
                .thenReturn(cropConfig("carrot", 8));
        when(playerLandMapper.selectById(10L)).thenReturn(emptyLand());

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> service.plant(1L, 10L, "carrot")
        );
        assertEquals("玩家等级不足，需要 Lv.8", error.getMessage());
        verify(playerCropService, never()).findByPlayerAndCrop(anyLong(), anyString());
    }

    private PlayerLand emptyLand() {
        PlayerLand land = new PlayerLand();
        land.setId(10L);
        land.setPlayerId(1L);
        land.setLandConfigId(1L);
        land.setStatus("EMPTY");
        return land;
    }

    private CropConfig cropConfig(String cropId, int playerUnlockLevel) {
        CropConfig config = new CropConfig();
        config.setCropId(cropId);
        config.setPlayerUnlockLevel(playerUnlockLevel);
        config.setEnabled(1);
        return config;
    }

    private CropLevelConfig levelConfig(
            String cropId,
            int cropLevel,
            int growSeconds,
            int yieldCount
    ) {
        CropLevelConfig config = new CropLevelConfig();
        config.setCropId(cropId);
        config.setCropLevel(cropLevel);
        config.setGrowSeconds(growSeconds);
        config.setYieldCount(yieldCount);
        config.setHarvestExp(switch (cropLevel) {
            case 1 -> 5;
            case 2 -> 8;
            default -> 12;
        });
        config.setUpgradeGold(0L);
        return config;
    }

    private PlayerCrop playerCrop(String cropId, int cropLevel) {
        PlayerCrop playerCrop = new PlayerCrop();
        playerCrop.setCropId(cropId);
        playerCrop.setCropLevel(cropLevel);
        return playerCrop;
    }
}
