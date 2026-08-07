package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.Animal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnimalMapper extends BaseMapperX<Animal> {
}
