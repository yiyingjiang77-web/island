package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CropConfig;
import com.fruitisland.game.entity.CropLevelConfig;
import com.fruitisland.game.entity.PlayerCropGrant;
import com.fruitisland.game.mapper.PlayerCropGrantMapper;
import com.fruitisland.game.service.CropConfigService;
import com.fruitisland.game.service.CropLevelConfigService;
import com.fruitisland.game.service.PlayerCropGrantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 限时稀有作物权限业务。
 */
@Service
@RequiredArgsConstructor
public class PlayerCropGrantServiceImpl
        extends BaseServiceImplX<PlayerCropGrantMapper, PlayerCropGrant>
        implements PlayerCropGrantService {

    private final CropConfigService cropConfigService;
    private final CropLevelConfigService cropLevelConfigService;

    @Override
    public PlayerCropGrant findActiveGrant(Long playerId, String cropId, LocalDateTime now) {
        return lambdaQuery()
                .eq(PlayerCropGrant::getPlayerId, playerId)
                .eq(PlayerCropGrant::getCropId, cropId)
                .eq(PlayerCropGrant::getStatus, "ACTIVE")
                .le(PlayerCropGrant::getValidFrom, now)
                .gt(PlayerCropGrant::getValidUntil, now)
                .orderByDesc(PlayerCropGrant::getValidUntil)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public List<PlayerCropGrant> listActiveByPlayer(Long playerId, LocalDateTime now) {
        return lambdaQuery()
                .eq(PlayerCropGrant::getPlayerId, playerId)
                .eq(PlayerCropGrant::getStatus, "ACTIVE")
                .le(PlayerCropGrant::getValidFrom, now)
                .gt(PlayerCropGrant::getValidUntil, now)
                .orderByAsc(PlayerCropGrant::getValidUntil)
                .list();
    }

    @Override
    @Transactional
    public PlayerCropGrant grantRareCrop(
            Long playerId,
            String cropId,
            Integer cropLevel,
            long durationSeconds,
            String source,
            String sourceRefId
    ) {
        if (durationSeconds <= 0) throw new RuntimeException("奖励有效时间必须大于 0");

        CropConfig config = cropConfigService.findByCropId(cropId);
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            throw new RuntimeException("奖励作物不存在或已停用: " + cropId);
        }
        if ("COMMON".equals(config.getRarity())
                || !Integer.valueOf(1).equals(config.getRewardEligible())) {
            throw new RuntimeException("只有允许奖励的稀有作物才能发放");
        }
        if (cropLevel < 1 || cropLevel > config.getMaxCropLevel()
                || cropLevelConfigService.findByCropAndLevel(cropId, cropLevel) == null) {
            throw new RuntimeException("奖励作物等级配置不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        PlayerCropGrant existing = findActiveGrant(playerId, cropId, now);
        if (existing != null) {
            // 重复获得同一种限时种子时直接延长有效期，避免产生大量重叠记录。
            existing.setValidUntil(existing.getValidUntil().plusSeconds(durationSeconds));
            existing.setGrantCropLevel(Math.max(existing.getGrantCropLevel(), cropLevel));
            updateById(existing);
            return existing;
        }

        PlayerCropGrant grant = new PlayerCropGrant();
        grant.setPlayerId(playerId);
        grant.setCropId(cropId);
        grant.setGrantCropLevel(cropLevel);
        grant.setGrantSource(source);
        grant.setSourceRefId(sourceRefId);
        grant.setValidFrom(now);
        grant.setValidUntil(now.plusSeconds(durationSeconds));
        grant.setStatus("ACTIVE");
        save(grant);
        return grant;
    }
}
