package com.fruitisland.game.service;

import com.fruitisland.game.entity.PlayerCropGrant;

/**
 * 稀有作物随机奖励服务。
 *
 * <p>任务、活动等业务完成后调用此服务，不允许客户端自行指定奖励作物。</p>
 */
public interface RareCropRewardService {

    /** 从指定奖励池按权重抽取，并发放限时稀有作物权限。 */
    PlayerCropGrant draw(Long playerId, String poolCode, String sourceRefId);
}
