package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.PlayerRecipe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlayerRecipeMapper extends BaseMapperX<PlayerRecipe> {
    @Select("""
            SELECT * FROM player_recipe
            WHERE player_id=#{playerId} AND recipe_id=#{recipeId}
              AND qualification_type='PERMANENT'
            """)
    PlayerRecipe selectPermanent(@Param("playerId") Long playerId, @Param("recipeId") String recipeId);

    @Select("""
            SELECT * FROM player_recipe
            WHERE player_id=#{playerId} AND recipe_id=#{recipeId}
              AND (qualification_type='PERMANENT'
                OR (qualification_type='TEMPORARY'
                  AND valid_from <= #{now} AND valid_until > #{now}))
            LIMIT 1
            """)
    PlayerRecipe selectActive(
            @Param("playerId") Long playerId,
            @Param("recipeId") String recipeId,
            @Param("now") java.time.LocalDateTime now);

    @Select("""
            SELECT * FROM player_recipe
            WHERE player_id=#{playerId}
              AND (qualification_type='PERMANENT'
                OR (qualification_type='TEMPORARY'
                  AND valid_from <= #{now} AND valid_until > #{now}))
            """)
    java.util.List<PlayerRecipe> selectActiveByPlayer(
            @Param("playerId") Long playerId,
            @Param("now") java.time.LocalDateTime now);
}
