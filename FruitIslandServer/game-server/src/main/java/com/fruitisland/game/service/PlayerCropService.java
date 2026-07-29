package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.PlayerCrop;

import java.util.List;

public interface PlayerCropService extends BaseServiceX<PlayerCrop> {

    /** 查询玩家是否永久拥有某个作物。 */
    PlayerCrop findByPlayerAndCrop(Long playerId, String cropId);

    /** 查询玩家全部永久作物权限。 */
    List<PlayerCrop> listByPlayer(Long playerId);

    /** 赠送永久种植权；重复调用不会重复创建。 */
    PlayerCrop grantPermanent(Long playerId, String cropId, String source);

    /** 使用金币将永久作物提升一级。 */
    PlayerCrop upgrade(Long playerId, String cropId);
}
