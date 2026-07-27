package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.mapper.GamePlayerMapper;
import com.fruitisland.game.service.GamePlayerService;
import org.springframework.stereotype.Service;

@Service
public class GamePlayerServiceImpl extends BaseServiceImplX<GamePlayerMapper, GamePlayer> implements GamePlayerService {
}
