package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.PlayerDrinkShop;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PlayerDrinkShopMapper extends BaseMapperX<PlayerDrinkShop> {
    @Insert("INSERT IGNORE INTO player_drink_shop (player_id, shop_level) VALUES (#{playerId}, 1)")
    int ensureInitial(@Param("playerId") Long playerId);

    @Select("SELECT * FROM player_drink_shop WHERE player_id=#{playerId} FOR UPDATE")
    PlayerDrinkShop selectForUpdate(@Param("playerId") Long playerId);
}
