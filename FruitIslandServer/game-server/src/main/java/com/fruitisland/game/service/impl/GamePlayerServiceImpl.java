package com.fruitisland.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.dto.ExpGainResult;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerLevelConfig;
import com.fruitisland.game.mapper.GamePlayerMapper;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerLevelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GamePlayerServiceImpl extends BaseServiceImplX<GamePlayerMapper, GamePlayer> implements GamePlayerService {

    private final PlayerLevelConfigService playerLevelConfigService;

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

        GamePlayer player = getById(playerId);
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
        int level = beforeLevel;
        int exp = (player.getExp() == null ? 0 : player.getExp()) + amount;
        long rewardGold = 0L;
        int levelsGained = 0;

        PlayerLevelConfig levelConfig = playerLevelConfigService.findByLevel(level);
        while (levelConfig != null && exp >= levelConfig.getRequiredExp()) {
            exp -= levelConfig.getRequiredExp();
            rewardGold += levelConfig.getRewardGold();
            level++;
            levelsGained++;
            levelConfig = playerLevelConfigService.findByLevel(level);
        }

        player.setLevel(level);
        player.setExp(exp);
        player.setGold((player.getGold() == null ? 0L : player.getGold()) + rewardGold);
        updateById(player);

        return new ExpGainResult(
                amount,
                beforeLevel,
                level,
                exp,
                levelConfig == null ? null : levelConfig.getRequiredExp(),
                levelsGained,
                rewardGold
        );
    }
}
