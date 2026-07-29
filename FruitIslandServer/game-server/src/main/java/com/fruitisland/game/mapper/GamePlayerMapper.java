package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.GamePlayer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GamePlayerMapper extends BaseMapperX<GamePlayer> {
    @Select("SELECT * FROM game_player WHERE id=#{playerId} FOR UPDATE")
    GamePlayer selectForUpdate(@Param("playerId") Long playerId);
}
