package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CropUnlockSource;
import com.fruitisland.game.mapper.CropUnlockSourceMapper;
import com.fruitisland.game.service.CropUnlockSourceService;
import org.springframework.stereotype.Service;

@Service
public class CropUnlockSourceServiceImpl
        extends BaseServiceImplX<CropUnlockSourceMapper, CropUnlockSource>
        implements CropUnlockSourceService {
}
