package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.QuestConfig;
import com.fruitisland.game.mapper.QuestConfigMapper;
import com.fruitisland.game.service.QuestConfigService;
import org.springframework.stereotype.Service;

@Service
public class QuestConfigServiceImpl extends BaseServiceImplX<QuestConfigMapper, QuestConfig> implements QuestConfigService {
}
