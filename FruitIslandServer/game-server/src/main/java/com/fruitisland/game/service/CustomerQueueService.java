package com.fruitisland.game.service;

import com.fruitisland.game.dto.CustomerQueueVO;

public interface CustomerQueueService {
    CustomerQueueVO getQueue(Long playerId);
    void recordDeparture(Long playerId);
}
