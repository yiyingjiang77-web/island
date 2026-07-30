package com.fruitisland.game.service;

import com.fruitisland.game.dto.IslandGrowthVO;
import com.fruitisland.game.entity.GamePlayer;

public interface IslandGrowthService {
    IslandGrowthVO initialize(GamePlayer player);
}
