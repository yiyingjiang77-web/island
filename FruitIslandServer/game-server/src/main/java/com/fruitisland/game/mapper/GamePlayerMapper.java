package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.GamePlayer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GamePlayerMapper extends BaseMapperX<GamePlayer> {
}
