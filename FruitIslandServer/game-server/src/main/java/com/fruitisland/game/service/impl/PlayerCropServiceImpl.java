package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CropConfig;
import com.fruitisland.game.entity.CropLevelConfig;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerCrop;
import com.fruitisland.game.mapper.PlayerCropMapper;
import com.fruitisland.game.service.CropConfigService;
import com.fruitisland.game.service.CropLevelConfigService;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerCropService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 玩家永久作物权限业务。
 */
@Service
@RequiredArgsConstructor
public class PlayerCropServiceImpl
        extends BaseServiceImplX<PlayerCropMapper, PlayerCrop>
        implements PlayerCropService {

    private final CropConfigService cropConfigService;
    private final CropLevelConfigService cropLevelConfigService;
    private final GamePlayerService gamePlayerService;

    @Override
    public PlayerCrop findByPlayerAndCrop(Long playerId, String cropId) {
        return lambdaQuery()
                .eq(PlayerCrop::getPlayerId, playerId)
                .eq(PlayerCrop::getCropId, cropId)
                .one();
    }

    @Override
    public List<PlayerCrop> listByPlayer(Long playerId) {
        return lambdaQuery()
                .eq(PlayerCrop::getPlayerId, playerId)
                .orderByAsc(PlayerCrop::getId)
                .list();
    }

    @Override
    @Transactional
    public PlayerCrop grantPermanent(Long playerId, String cropId, String source) {
        PlayerCrop existing = findByPlayerAndCrop(playerId, cropId);
        if (existing != null) return existing;

        CropConfig config = requireCrop(cropId);
        if (!Integer.valueOf(1).equals(config.getPermanentUnlockEnabled())) {
            throw new RuntimeException("该作物不支持永久解锁: " + cropId);
        }

        GamePlayer player = requirePlayer(playerId);
        if (player.getLevel() < config.getPlayerUnlockLevel()) {
            throw new RuntimeException("玩家等级不足，需要 Lv." + config.getPlayerUnlockLevel());
        }

        PlayerCrop playerCrop = new PlayerCrop();
        playerCrop.setPlayerId(playerId);
        playerCrop.setCropId(cropId);
        playerCrop.setCropLevel(1);
        playerCrop.setUnlockSource(source);
        playerCrop.setUnlockTime(LocalDateTime.now());
        try {
            save(playerCrop);
            return playerCrop;
        } catch (DuplicateKeyException ignored) {
            return findByPlayerAndCrop(playerId, cropId);
        }
    }

    @Override
    @Transactional
    public PlayerCrop upgrade(Long playerId, String cropId) {
        PlayerCrop playerCrop = findByPlayerAndCrop(playerId, cropId);
        if (playerCrop == null) {
            throw new RuntimeException("尚未永久获得该作物，不能升级");
        }

        CropConfig config = requireCrop(cropId);
        if (!Integer.valueOf(1).equals(config.getUpgradeEnabled())) {
            throw new RuntimeException("该作物不可升级");
        }

        int targetLevel = playerCrop.getCropLevel() + 1;
        if (targetLevel > config.getMaxCropLevel()) {
            throw new RuntimeException("作物已达到最高等级");
        }

        CropLevelConfig targetConfig =
                cropLevelConfigService.findByCropAndLevel(cropId, targetLevel);
        if (targetConfig == null || targetConfig.getUpgradeGold() == null) {
            throw new RuntimeException("缺少目标等级配置: " + cropId + " Lv." + targetLevel);
        }

        GamePlayer player = requirePlayer(playerId);
        if (player.getGold() < targetConfig.getUpgradeGold()) {
            throw new RuntimeException("金币不足，需要 " + targetConfig.getUpgradeGold());
        }

        // 扣金币和升级必须处于同一事务，任一更新失败都会整体回滚。
        player.setGold(player.getGold() - targetConfig.getUpgradeGold());
        gamePlayerService.updateById(player);
        playerCrop.setCropLevel(targetLevel);
        updateById(playerCrop);
        return playerCrop;
    }

    private CropConfig requireCrop(String cropId) {
        CropConfig config = cropConfigService.findByCropId(cropId);
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            throw new RuntimeException("作物不存在或已停用: " + cropId);
        }
        return config;
    }

    private GamePlayer requirePlayer(Long playerId) {
        GamePlayer player = gamePlayerService.getById(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");
        return player;
    }
}
