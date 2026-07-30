package com.fruitisland.game.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.game.entity.CropPlant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CropPlantMapper extends BaseMapperX<CropPlant> {

    /** 查询某土地最新的种植记录 */
    @Select("SELECT * FROM crop_plant WHERE player_land_id = #{playerLandId} ORDER BY create_time DESC LIMIT 1")
    CropPlant selectLatestByPlayerLand(@Param("playerLandId") Long playerLandId);
}
