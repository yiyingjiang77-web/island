package com.fruitisland.game.service.impl;

import com.fruitisland.game.entity.CropRewardPoolItem;
import com.fruitisland.game.entity.PlayerCropGrant;
import com.fruitisland.game.mapper.CropRewardPoolItemMapper;
import com.fruitisland.game.service.PlayerCropGrantService;
import com.fruitisland.game.service.RareCropRewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 按整数权重抽取稀有作物奖励。
 */
@Service
@RequiredArgsConstructor
public class RareCropRewardServiceImpl implements RareCropRewardService {

    private final CropRewardPoolItemMapper rewardPoolItemMapper;
    private final PlayerCropGrantService playerCropGrantService;

    @Override
    @Transactional
    public PlayerCropGrant draw(Long playerId, String poolCode, String sourceRefId) {
        List<CropRewardPoolItem> candidates = rewardPoolItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CropRewardPoolItem>()
                        .eq(CropRewardPoolItem::getPoolCode, poolCode)
                        .eq(CropRewardPoolItem::getEnabled, 1)
                        .gt(CropRewardPoolItem::getWeight, 0)
                        .orderByAsc(CropRewardPoolItem::getId)
        );
        if (candidates.isEmpty()) throw new RuntimeException("奖励池不存在或没有可用奖励");

        int totalWeight = candidates.stream()
                .mapToInt(CropRewardPoolItem::getWeight)
                .sum();
        int ticket = ThreadLocalRandom.current().nextInt(totalWeight);
        CropRewardPoolItem selected = candidates.get(candidates.size() - 1);
        for (CropRewardPoolItem candidate : candidates) {
            ticket -= candidate.getWeight();
            if (ticket < 0) {
                selected = candidate;
                break;
            }
        }

        // grantRareCrop 会执行第二层校验，确保普通作物无法因错误配置被发出去。
        return playerCropGrantService.grantRareCrop(
                playerId,
                selected.getCropId(),
                selected.getGrantCropLevel(),
                selected.getDurationSeconds(),
                "REWARD_POOL:" + poolCode,
                sourceRefId
        );
    }
}
