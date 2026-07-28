package com.fruitisland.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.mapper.GamePlayerMapper;
import com.fruitisland.game.service.GamePlayerService;
import org.springframework.stereotype.Service;

@Service
public class GamePlayerServiceImpl extends BaseServiceImplX<GamePlayerMapper, GamePlayer> implements GamePlayerService {

    @Override
    public GamePlayer findByUserId(Long userId) {
        LambdaQueryWrapper<GamePlayer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GamePlayer::getUserId, userId);
        return getOne(wrapper);
    }

    @Override
    public GamePlayer createPlayer(Long userId) {
        GamePlayer player = new GamePlayer();
        player.setUserId(userId);
        player.setGameId("fruit_island");
        player.setNickname("岛主");
        player.setLevel(1);
        player.setExp(0);
        player.setGold(500L);
        player.setDiamond(20);
        player.setAvatarId("default");
        save(player);
        return player;
    }
}
