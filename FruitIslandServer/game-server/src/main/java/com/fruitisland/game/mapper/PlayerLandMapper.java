package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.PlayerLand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PlayerLandMapper extends BaseMapperX<PlayerLand> {

    /** 查询某玩家的所有已购买土地 */
    @Select("SELECT * FROM player_land WHERE player_id = #{playerId}")
    List<PlayerLand> selectByPlayerId(Long playerId);

    /** 查询某玩家某块配置对应的土地（用于防重复购买） */
    @Select("SELECT * FROM player_land WHERE player_id = #{playerId} AND land_config_id = #{landConfigId}")
    PlayerLand selectByPlayerAndConfig(Long playerId, Long landConfigId);
}
