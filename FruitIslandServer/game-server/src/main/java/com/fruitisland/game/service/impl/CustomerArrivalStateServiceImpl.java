package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CustomerArrivalState;
import com.fruitisland.game.mapper.CustomerArrivalStateMapper;
import com.fruitisland.game.service.CustomerArrivalStateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerArrivalStateServiceImpl
        extends BaseServiceImplX<CustomerArrivalStateMapper, CustomerArrivalState>
        implements CustomerArrivalStateService {
    @Override
    public CustomerArrivalState lockByPlayer(Long playerId) {
        return baseMapper.lockByPlayer(playerId);
    }

    @Override
    @Transactional
    public CustomerArrivalState lockOrCreate(Long playerId) {
        baseMapper.insertIgnore(playerId);
        return baseMapper.lockByPlayer(playerId);
    }

    @Override
    public void setNextArrivalAt(Long playerId, java.time.LocalDateTime nextArrivalAt) {
        baseMapper.updateNextArrivalAt(playerId, nextArrivalAt);
    }
}
