package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.PlayerCakeRack;

import java.util.List;

public interface PlayerCakeRackService extends BaseServiceX<PlayerCakeRack> {

    /** 确保玩家拥有两个蛋糕架，返回列表 */
    List<PlayerCakeRack> getOrCreateRacks(Long playerId);

    /** 上架蛋糕到指定槽位 */
    PlayerCakeRack listCake(Long playerId, int slot, String recipeId, int quantity);

    /** 惰性结算单个架子（更新已售数量和状态） */
    PlayerCakeRack settleRack(Long playerId, int slot);

    /** 惰性结算玩家所有架子 */
    void settleAllRacks(Long playerId);

    /** 下架（退回未售蛋糕，结算已售收益） */
    PlayerCakeRack takeDown(Long playerId, int slot);

    /** 收取售罄架子的收益 */
    PlayerCakeRack collect(Long playerId, int slot);
}
