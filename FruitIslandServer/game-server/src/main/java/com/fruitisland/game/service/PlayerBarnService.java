package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.PlayerBarn;

public interface PlayerBarnService extends BaseServiceX<PlayerBarn> {

    /** 获取或初始化玩家牛棚记录。 */
    PlayerBarn getOrCreate(Long playerId);

    /** 首次解锁牛棚（岛屿 Lv5 + 1000 金币 + 送 1 奶牛 + 10 牛奶）。 */
    PlayerBarn unlockBarn(Long playerId);

    /** 逐级升级牛棚（先结算当前周期产出）。 */
    PlayerBarn upgradeBarn(Long playerId);

    /** 惰性结算牛奶生产，补算所有已完成周期并入库。 */
    PlayerBarn settleMilkProduction(Long playerId);
}
