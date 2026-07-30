package com.fruitisland.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.dto.ExpGainResult;
import com.fruitisland.game.dto.IslandLevelRewardVO;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.IslandLevelConfig;
import com.fruitisland.game.entity.PlayerIslandLevelRewardClaim;
import com.fruitisland.game.mapper.GamePlayerMapper;
import com.fruitisland.game.mapper.IslandLevelConfigMapper;
import com.fruitisland.game.mapper.PlayerIslandLevelRewardClaimMapper;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerCropService;
import com.fruitisland.game.service.PlayerRecipeService;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GamePlayerServiceImpl extends BaseServiceImplX<GamePlayerMapper, GamePlayer> implements GamePlayerService {

    private final IslandLevelConfigMapper islandLevelConfigMapper;
    private final PlayerIslandLevelRewardClaimMapper claimMapper;
    private final PlayerCropService playerCropService;
    private final PlayerRecipeService playerRecipeService;

    public GamePlayerServiceImpl(
            IslandLevelConfigMapper islandLevelConfigMapper,
            PlayerIslandLevelRewardClaimMapper claimMapper,
            @Lazy PlayerCropService playerCropService,
            PlayerRecipeService playerRecipeService) {
        this.islandLevelConfigMapper = islandLevelConfigMapper;
        this.claimMapper = claimMapper;
        this.playerCropService = playerCropService;
        this.playerRecipeService = playerRecipeService;
    }

    @Override
    public GamePlayer findByUserId(Long userId) {
        LambdaQueryWrapper<GamePlayer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GamePlayer::getUserId, userId);
        return getOne(wrapper);
    }

    @Override
    public GamePlayer createPlayer(Long userId) {
        GamePlayer player = new GamePlayer();
        player.setUserId(userId);
        player.setGameId("fruit_island");
        player.setNickname("岛主");
        player.setLevel(1);
        player.setExp(0);
        player.setCumulativeExp(0);
        player.setGold(500L);
        player.setDiamond(20);
        player.setAvatarId("default");
        save(player);
        return player;
    }

    @Override
    @Transactional
    public ExpGainResult addExp(Long playerId, int amount) {
        if (amount < 0) throw new IllegalArgumentException("经验值不能为负数");
        GamePlayer player = baseMapper.selectForUpdate(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");
        return applyExperience(player, amount);
    }

    @Override
    @Transactional
    public ExpGainResult settleDrinkSaleReward(Long playerId, int gold, int exp) {
        if (gold < 0 || exp < 0) throw new IllegalArgumentException("售出收益不能为负数");
        GamePlayer player = baseMapper.selectForUpdate(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");
        player.setGold((player.getGold() == null ? 0L : player.getGold()) + gold);
        return applyExperience(player, exp);
    }

    private ExpGainResult applyExperience(GamePlayer player, int amount) {
        int beforeLevel = player.getLevel() == null ? 1 : player.getLevel();
        int cumulativeExp = (player.getCumulativeExp() == null ? 0 : player.getCumulativeExp()) + amount;

        List<IslandLevelConfig> configs = islandLevelConfigMapper.selectList(
                new LambdaQueryWrapper<IslandLevelConfig>()
                        .eq(IslandLevelConfig::getEnabled, 1)
                        .orderByAsc(IslandLevelConfig::getLevel));

        int newLevel = beforeLevel;
        for (IslandLevelConfig config : configs) {
            if (cumulativeExp >= config.getCumulativeExp()) {
                newLevel = Math.max(newLevel, config.getLevel());
            }
        }
        final int resolvedLevel = newLevel;

        player.setCumulativeExp(cumulativeExp);
        player.setLevel(resolvedLevel);
        int currentLevelFloor = configs.stream()
                .filter(c -> c.getLevel() <= resolvedLevel)
                .map(IslandLevelConfig::getCumulativeExp)
                .max(Integer::compareTo)
                .orElse(0);
        player.setExp(cumulativeExp - currentLevelFloor);
        updateById(player);

        Set<Integer> claimedLevels = new HashSet<>();
        claimMapper.selectList(new LambdaQueryWrapper<PlayerIslandLevelRewardClaim>()
                        .eq(PlayerIslandLevelRewardClaim::getPlayerId, player.getId()))
                .forEach(claim -> claimedLevels.add(claim.getIslandLevel()));

        List<IslandLevelRewardVO> levelRewards = new ArrayList<>();
        for (IslandLevelConfig config : configs) {
            if (config.getLevel() <= resolvedLevel && !claimedLevels.contains(config.getLevel())) {
                playerCropService.grantPermanent(
                        player.getId(), config.getCropId(), "ISLAND_LEVEL_REWARD");
                playerRecipeService.grantPermanent(
                        player.getId(), config.getRecipeId(), "ISLAND_LEVEL_REWARD");
                recordClaim(player.getId(), config.getLevel());
                levelRewards.add(new IslandLevelRewardVO(
                        config.getLevel(),
                        config.getCumulativeExp(),
                        config.getCropId(),
                        config.getRecipeId(),
                        true,
                        config.getMaterialSourceHint(),
                        config.getShopCapabilityHint()));
            }
        }

        Integer nextLevelThreshold = configs.stream()
                .filter(c -> c.getLevel() > resolvedLevel)
                .map(IslandLevelConfig::getCumulativeExp)
                .findFirst()
                .orElse(null);

        int levelsGained = resolvedLevel - beforeLevel;
        return new ExpGainResult(
                amount,
                beforeLevel,
                resolvedLevel,
                cumulativeExp,
                nextLevelThreshold,
                levelsGained,
                levelRewards);
    }

    private void recordClaim(Long playerId, Integer level) {
        PlayerIslandLevelRewardClaim existing = claimMapper.selectOne(
                new LambdaQueryWrapper<PlayerIslandLevelRewardClaim>()
                        .eq(PlayerIslandLevelRewardClaim::getPlayerId, playerId)
                        .eq(PlayerIslandLevelRewardClaim::getIslandLevel, level));
        if (existing != null) return;
        PlayerIslandLevelRewardClaim claim = new PlayerIslandLevelRewardClaim();
        claim.setPlayerId(playerId);
        claim.setIslandLevel(level);
        claim.setClaimedAt(LocalDateTime.now());
        try {
            claimMapper.insert(claim);
        } catch (DuplicateKeyException ignored) {
            // 唯一约束保证并发只保留一条领取记录。
        }
    }
}
