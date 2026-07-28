package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.CropPlant;

public interface CropPlantService extends BaseServiceX<CropPlant> {

    /** 查询某土地最新的种植记录 */
    CropPlant findLatestByPlayerLand(Long playerLandId);
}
