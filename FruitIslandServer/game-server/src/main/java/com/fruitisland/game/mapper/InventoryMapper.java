package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InventoryMapper extends BaseMapperX<Inventory> {

    /** 查询玩家某物品 */
    @Select("SELECT * FROM inventory WHERE player_id = #{playerId} AND item_id = #{itemId}")
    Inventory selectByPlayerAndItem(@Param("playerId") Long playerId, @Param("itemId") String itemId);
}
