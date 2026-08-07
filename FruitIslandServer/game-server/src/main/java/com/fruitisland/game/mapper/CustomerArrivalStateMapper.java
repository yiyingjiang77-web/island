package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.CustomerArrivalState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CustomerArrivalStateMapper extends BaseMapperX<CustomerArrivalState> {
    @Insert("INSERT IGNORE INTO customer_arrival_state (player_id) VALUES (#{playerId})")
    int insertIgnore(@Param("playerId") Long playerId);

    @Select("SELECT * FROM customer_arrival_state WHERE player_id=#{playerId} FOR UPDATE")
    CustomerArrivalState lockByPlayer(@Param("playerId") Long playerId);

    @Update("""
            UPDATE customer_arrival_state
            SET next_arrival_at=#{nextArrivalAt}
            WHERE player_id=#{playerId}
            """)
    int updateNextArrivalAt(
            @Param("playerId") Long playerId,
            @Param("nextArrivalAt") java.time.LocalDateTime nextArrivalAt);
}
