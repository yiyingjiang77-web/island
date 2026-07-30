package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.PlayerCropGrant;

import java.time.LocalDateTime;
import java.util.List;

public interface PlayerCropGrantService extends BaseServiceX<PlayerCropGrant> {

    /** 查询当前有效的限时权限。 */
    PlayerCropGrant findActiveGrant(Long playerId, String cropId, LocalDateTime now);

    /** 查询玩家尚在有效期内的全部限时权限。 */
    List<PlayerCropGrant> listActiveByPlayer(Long playerId, LocalDateTime now);

    /**
     * 发放限时稀有作物权限。
     *
     * @param durationSeconds 有效秒数，必须大于 0
     */
    PlayerCropGrant grantRareCrop(
            Long playerId,
            String cropId,
            Integer cropLevel,
            long durationSeconds,
            String source,
            String sourceRefId
    );
}
