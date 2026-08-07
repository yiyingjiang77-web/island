package com.fruitisland.game.service;
import com.fruitisland.game.dto.SatisfactionStatusVO;

import java.util.List;

public interface SatisfactionService {
    SatisfactionStatusVO getStatus(Long playerId);
    List<SatisfactionStatusVO.History> settlePastDays(Long playerId);
}
