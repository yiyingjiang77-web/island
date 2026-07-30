package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.DrinkBarBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface DrinkBarBatchMapper extends BaseMapperX<DrinkBarBatch> {

    @Select("""
            SELECT *
            FROM drink_bar_batch
            WHERE bar_id = #{barId}
              AND player_id = #{playerId}
              AND active_marker = 1
            FOR UPDATE
            """)
    DrinkBarBatch lockActiveBatch(
            @Param("playerId") Long playerId,
            @Param("barId") Long barId);

    @Update("""
            UPDATE drink_bar_batch
            SET status = 'CLOSED',
                active_marker = NULL,
                closed_at = #{closedAt},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{batchId}
              AND active_marker = 1
            """)
    int closeActiveBatch(
            @Param("batchId") Long batchId,
            @Param("closedAt") LocalDateTime closedAt);

    @Update("""
            UPDATE drink_bar_batch
            SET sold_quantity = #{soldQuantity},
                status = #{status},
                sold_out_at = #{soldOutAt},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{batchId}
              AND active_marker = 1
              AND status = 'SELLING'
              AND sold_quantity < #{soldQuantity}
            """)
    int updateSalesProgress(
            @Param("batchId") Long batchId,
            @Param("soldQuantity") int soldQuantity,
            @Param("status") String status,
            @Param("soldOutAt") LocalDateTime soldOutAt);
}
