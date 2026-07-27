package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.PlayerQuest;
import com.fruitisland.game.mapper.PlayerQuestMapper;
import com.fruitisland.game.service.PlayerQuestService;
import org.springframework.stereotype.Service;

@Service
public class PlayerQuestServiceImpl extends BaseServiceImplX<PlayerQuestMapper, PlayerQuest> implements PlayerQuestService {
}
