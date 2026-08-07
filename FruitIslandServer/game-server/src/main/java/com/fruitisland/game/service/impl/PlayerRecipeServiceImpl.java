package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.PlayerRecipe;
import com.fruitisland.game.mapper.PlayerRecipeMapper;
import com.fruitisland.game.service.PlayerRecipeService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlayerRecipeServiceImpl extends BaseServiceImplX<PlayerRecipeMapper, PlayerRecipe>
        implements PlayerRecipeService {
    private final Clock clock;

    public PlayerRecipeServiceImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public PlayerRecipe findPermanent(Long playerId, String recipeId) {
        return baseMapper.selectPermanent(playerId, recipeId);
    }

    @Override
    public PlayerRecipe findActive(Long playerId, String recipeId) {
        return baseMapper.selectActive(playerId, recipeId, LocalDateTime.now(clock));
    }

    @Override
    public PlayerRecipe grantPermanent(Long playerId, String recipeId, String source) {
        PlayerRecipe existing = findPermanent(playerId, recipeId);
        if (existing != null) return existing;
        PlayerRecipe qualification = new PlayerRecipe();
        qualification.setPlayerId(playerId);
        qualification.setRecipeId(recipeId);
        qualification.setQualificationType("PERMANENT");
        qualification.setUnlockSource(source);
        qualification.setUnlockTime(LocalDateTime.now());
        try {
            save(qualification);
            return qualification;
        } catch (DuplicateKeyException ignored) {
            return findPermanent(playerId, recipeId);
        }
    }

    @Override
    public List<PlayerRecipe> listByPlayer(Long playerId) {
        return new java.util.ArrayList<>(
                baseMapper.selectActiveByPlayer(playerId, LocalDateTime.now(clock)).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                PlayerRecipe::getRecipeId,
                                value -> value,
                                (first, ignored) -> first,
                                java.util.LinkedHashMap::new))
                        .values());
    }
}
