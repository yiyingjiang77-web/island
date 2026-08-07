package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.PlayerBeehive;

public interface PlayerBeehiveService extends BaseServiceX<PlayerBeehive> {

    /** 获取或初始化玩家蜂箱记录。 */
    PlayerBeehive getOrCreate(Long playerId);

    /** 购买一个蜂箱（最多 3 个）。 */
    PlayerBeehive purchaseBeehive(Long playerId);

    /** 惰性结算产蜜并返回当前可收取量。 */
    PlayerBeehive settleProduction(Long playerId);

    /** 收取蜂蜜并入背包。 */
    int collectHoney(Long playerId);
}
