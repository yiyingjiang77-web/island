package com.fruitisland.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.Island;
import com.fruitisland.game.mapper.IslandMapper;
import com.fruitisland.game.service.IslandService;
import org.springframework.stereotype.Service;

@Service
public class IslandServiceImpl extends BaseServiceImplX<IslandMapper, Island> implements IslandService {

    @Override
    public Island findByPlayerId(Long playerId) {
        LambdaQueryWrapper<Island> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Island::getPlayerId, playerId);
        return getOne(wrapper);
    }

    @Override
    public Island createIsland(Long playerId) {
        Island island = new Island();
        island.setPlayerId(playerId);
        island.setIslandName("果香小岛");
        island.setLevel(1);
        save(island);
        return island;
    }
}
