package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.dto.HarvestResultVO;
import com.fruitisland.game.dto.LandVO;
import com.fruitisland.game.entity.PlayerLand;

import java.util.List;

public interface PlayerLandService extends BaseServiceX<PlayerLand> {

    /** 获取玩家所有土地视图（合并 land_config + 动态状态） */
    List<LandVO> listByPlayer(Long playerId, Integer playerLevel);

    /** 购买土地 */
    PlayerLand buy(Long playerId, Long landConfigId, Integer playerLevel);

    /** 种植 */
    PlayerLand plant(Long playerId, Long playerLandId, String cropId);

    /** 浇水 */
    PlayerLand water(Long playerId, Long playerLandId);

    /** 收获 */
    HarvestResultVO harvest(Long playerId, Long playerLandId);
}
