package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.PlayerCoop;

public interface PlayerCoopService extends BaseServiceX<PlayerCoop> {

    /** 获取或初始化玩家鸡舍记录。 */
    PlayerCoop getOrCreate(Long playerId);

    /** 首次解锁鸡舍（岛屿 Lv8 + 3000 金币 + 送 1 鸡 + 5 鸡蛋）。 */
    PlayerCoop unlockCoop(Long playerId);

    /** 逐级升级鸡舍（先结算当前周期产出）。 */
    PlayerCoop upgradeCoop(Long playerId);

    /** 惰性结算鸡蛋生产，补算所有已完成周期并入库。 */
    PlayerCoop settleEggProduction(Long playerId);
}
