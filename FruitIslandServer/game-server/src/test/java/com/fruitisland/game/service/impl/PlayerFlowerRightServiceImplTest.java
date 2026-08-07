package com.fruitisland.game.service.impl;

import com.fruitisland.game.entity.FlowerConfig;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.mapper.PlayerFlowerRightMapper;
import com.fruitisland.game.service.FlowerConfigService;
import com.fruitisland.game.service.GamePlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlayerFlowerRightServiceImplTest {
    private PlayerFlowerRightMapper mapper;
    private FlowerConfigService configService;
    private GamePlayerService playerService;
    private PlayerFlowerRightServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(PlayerFlowerRightMapper.class);
        configService = mock(FlowerConfigService.class);
        playerService = mock(GamePlayerService.class);
        service = new PlayerFlowerRightServiceImpl(configService, playerService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.insert(any())).thenReturn(1);
    }

    @Test
    void goldPurchaseDeductsGoldAndCreatesPermanentRight() {
        FlowerConfig rose = flower("rose", "GOLD", 500L);
        GamePlayer player = player(1000L, 20);
        when(configService.findByFlowerId("rose")).thenReturn(rose);
        when(playerService.getById(1L)).thenReturn(player);

        var right = service.purchase(1L, "rose");

        assertEquals(500L, player.getGold());
        assertEquals(1, right.getFlowerLevel());
        assertEquals("GOLD", right.getPurchaseCurrency());
        verify(playerService).updateById(player);
    }

    @Test
    void diamondPurchaseDeductsDiamonds() {
        when(configService.findByFlowerId("sakura"))
                .thenReturn(flower("sakura", "DIAMOND", 10L));
        GamePlayer player = player(1000L, 20);
        when(playerService.getById(1L)).thenReturn(player);

        service.purchase(1L, "sakura");

        assertEquals(10, player.getDiamond());
        assertEquals(1000L, player.getGold());
    }

    private FlowerConfig flower(String id, String currency, Long price) {
        FlowerConfig config = new FlowerConfig();
        config.setFlowerId(id);
        config.setName(id);
        config.setPurchaseCurrency(currency);
        config.setPurchasePrice(price);
        config.setEnabled(1);
        return config;
    }

    private GamePlayer player(Long gold, Integer diamonds) {
        GamePlayer player = new GamePlayer();
        player.setId(1L);
        player.setGold(gold);
        player.setDiamond(diamonds);
        return player;
    }
}
