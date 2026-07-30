package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.CustomerArrivalState;

public interface CustomerArrivalStateService extends BaseServiceX<CustomerArrivalState> {
    CustomerArrivalState lockByPlayer(Long playerId);
    CustomerArrivalState lockOrCreate(Long playerId);
    void setNextArrivalAt(Long playerId, java.time.LocalDateTime nextArrivalAt);
}
