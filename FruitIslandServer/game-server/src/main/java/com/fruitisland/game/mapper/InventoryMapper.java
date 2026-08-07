package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper extends BaseMapperX<Inventory> {

    /** 查询玩家某物品 */
    @Select("SELECT * FROM inventory WHERE player_id = #{playerId} AND item_id = #{itemId}")
    Inventory selectByPlayerAndItem(@Param("playerId") Long playerId, @Param("itemId") String itemId);

    @Update("UPDATE inventory SET count=count-#{count} WHERE player_id=#{playerId} AND item_id=#{itemId} AND count>=#{count}")
    int decrementIfEnough(@Param("playerId") Long playerId, @Param("itemId") String itemId, @Param("count") int count);

    @Update("UPDATE inventory SET count=count+#{count} WHERE player_id=#{playerId} AND item_id=#{itemId}")
    int incrementExisting(@Param("playerId") Long playerId, @Param("itemId") String itemId, @Param("count") int count);
}
