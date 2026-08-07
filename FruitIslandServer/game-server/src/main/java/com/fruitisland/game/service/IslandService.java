package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.Island;

public interface IslandService extends BaseServiceX<Island> {

    /** 根据 playerId 查找岛屿 */
    Island findByPlayerId(Long playerId);

    /** 为角色创建初始岛屿 */
    Island createIsland(Long playerId);
}
