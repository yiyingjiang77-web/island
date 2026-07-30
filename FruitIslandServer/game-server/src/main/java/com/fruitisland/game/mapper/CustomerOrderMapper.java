package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.CustomerOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerOrderMapper extends BaseMapperX<CustomerOrder> {
    @Select("""
            SELECT * FROM customer_order
            WHERE id = #{orderId} AND player_id = #{playerId} AND status = 'WAITING'
            FOR UPDATE
            """)
    CustomerOrder selectWaitingForUpdate(
            @Param("orderId") Long orderId, @Param("playerId") Long playerId);
}
