package com.fruitisland.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitisland.game.dto.IslandGrowthVO;
import com.fruitisland.game.dto.IslandLevelRewardVO;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.IslandLevelConfig;
import com.fruitisland.game.entity.PlayerIslandLevelRewardClaim;
import com.fruitisland.game.mapper.IslandLevelConfigMapper;
import com.fruitisland.game.mapper.PlayerIslandLevelRewardClaimMapper;
import com.fruitisland.game.mapper.PlayerLevelConfigMapper;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.IslandGrowthService;
import com.fruitisland.game.service.PlayerCropService;
import com.fruitisland.game.service.PlayerRecipeService;
import com.fruitisland.game.util.LevelFormulaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IslandGrowthServiceImpl implements IslandGrowthService {

    private final IslandLevelConfigMapper levelConfigMapper;
    private final PlayerIslandLevelRewardClaimMapper claimMapper;
    private final PlayerLevelConfigMapper legacyLevelConfigMapper;
    private final GamePlayerService gamePlayerService;
    private final PlayerCropService playerCropService;
    private final PlayerRecipeService playerRecipeService;

    @Override
    @Transactional
    public IslandGrowthVO initialize(GamePlayer player) {
        List<IslandLevelConfig> configs = levelConfigMapper.selectList(
                new LambdaQueryWrapper<IslandLevelConfig>()
                        .eq(IslandLevelConfig::getEnabled, 1)
                        .orderByAsc(IslandLevelConfig::getLevel));
        convertLegacyExperience(player, configs);

        // ── 等级计算：Lv1-20 从表查，Lv21+ 用公式 ──
        int tableMaxLevel = configs.isEmpty() ? 1 : configs.get(configs.size() - 1).getLevel();
        long tableMaxThreshold = configs.isEmpty() ? 0L
                : configs.get(configs.size() - 1).getCumulativeExp().longValue();
        long cumExp = player.getCumulativeExp() == null ? 0L : player.getCumulativeExp().longValue();

        int currentLevel = player.getLevel() == null ? 1 : player.getLevel();
        // 重新从累计经验计算等级（确保数据一致性）
        for (IslandLevelConfig config : configs) {
            if (cumExp >= config.getCumulativeExp()) {
                currentLevel = Math.max(currentLevel, config.getLevel());
            }
        }
        if (cumExp >= tableMaxThreshold && tableMaxLevel >= LevelFormulaUtil.MAX_TABLE_LEVEL) {
            currentLevel = LevelFormulaUtil.calculateLevelBeyondTable(cumExp, tableMaxThreshold);
        }
        player.setLevel(currentLevel);

        for (IslandLevelConfig config : configs) {
            if (config.getLevel() > currentLevel) break;
            // Lv11+ 可能为 NULL（NPC/动物系统未建）
            if (config.getCropId() != null) {
                playerCropService.grantPermanent(
                        player.getId(), config.getCropId(), "ISLAND_LEVEL_REWARD");
            }
            if (config.getRecipeId() != null) {
                playerRecipeService.grantPermanent(
                        player.getId(), config.getRecipeId(), "ISLAND_LEVEL_REWARD");
            }
            // Lv5 额外授予 milk_ice_cream（使用牛棚牛奶，不依赖作物解锁）
            if (config.getLevel() == 5) {
                playerRecipeService.grantPermanent(
                        player.getId(), "milk_ice_cream", "ISLAND_LEVEL_REWARD");
            }
            recordClaim(player.getId(), config.getLevel());
        }

        Set<Integer> claimedLevels = new HashSet<>();
        claimMapper.selectList(new LambdaQueryWrapper<PlayerIslandLevelRewardClaim>()
                        .eq(PlayerIslandLevelRewardClaim::getPlayerId, player.getId()))
                .forEach(claim -> claimedLevels.add(claim.getIslandLevel()));

        // ── 下一级累计经验阈值 ──
        final int resolvedLevel = currentLevel;
        Integer nextThreshold;
        if (resolvedLevel < tableMaxLevel) {
            nextThreshold = configs.stream()
                    .filter(config -> config.getLevel() > resolvedLevel)
                    .map(IslandLevelConfig::getCumulativeExp)
                    .findFirst()
                    .orElse(null);
        } else {
            // Lv20+ 用公式计算
            nextThreshold = (int) LevelFormulaUtil.nextThresholdBeyond(resolvedLevel, tableMaxThreshold);
        }
        List<IslandLevelRewardVO> rewards = configs.stream()
                .map(config -> new IslandLevelRewardVO(
                        config.getLevel(),
                        config.getCumulativeExp(),
                        config.getCropId(),
                        config.getRecipeId(),
                        claimedLevels.contains(config.getLevel()),
                        config.getMaterialSourceHint(),
                        config.getShopCapabilityHint()))
                .toList();
        gamePlayerService.updateById(player);
        return new IslandGrowthVO(
                player.getCumulativeExp() == null ? 0 : player.getCumulativeExp(),
                resolvedLevel,
                nextThreshold,
                rewards);
    }

    private void convertLegacyExperience(
            GamePlayer player, List<IslandLevelConfig> configs) {
        if (player.getCumulativeExp() != null) return;
        int currentLevel = player.getLevel() == null ? 1 : player.getLevel();
        int levelFloor = configs.stream()
                .filter(config -> config.getLevel() <= currentLevel)
                .map(IslandLevelConfig::getCumulativeExp)
                .reduce((first, second) -> second)
                .orElse(0);
        int legacyCompletedLevelExp = legacyLevelConfigMapper.selectList(
                        new LambdaQueryWrapper<com.fruitisland.game.entity.PlayerLevelConfig>()
                                .lt(com.fruitisland.game.entity.PlayerLevelConfig::getLevel, currentLevel))
                .stream()
                .map(com.fruitisland.game.entity.PlayerLevelConfig::getRequiredExp)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int legacyTotal = legacyCompletedLevelExp
                + Math.max(0, player.getExp() == null ? 0 : player.getExp());
        player.setCumulativeExp(Math.max(levelFloor, legacyTotal));
        gamePlayerService.updateById(player);
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
            // 唯一约束保证并发初始化只保留一条领取记录。
        }
    }
}
