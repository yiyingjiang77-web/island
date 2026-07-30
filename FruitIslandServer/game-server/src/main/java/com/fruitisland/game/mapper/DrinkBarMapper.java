package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.DrinkBar;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DrinkBarMapper extends BaseMapperX<DrinkBar> {

    @Insert("""
            INSERT IGNORE INTO drink_bar (player_id, slot_number, opened)
            VALUES (#{playerId}, #{slotNumber}, 1)
            """)
    int ensureSlot(@Param("playerId") Long playerId, @Param("slotNumber") int slotNumber);

    @Select("""
            SELECT * FROM drink_bar
            WHERE id = #{barId} AND player_id = #{playerId}
            FOR UPDATE
            """)
    DrinkBar lockOwnedBar(
            @Param("playerId") Long playerId,
            @Param("barId") Long barId
    );
}
