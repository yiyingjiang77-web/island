package com.fruitisland.game.service;

import com.fruitisland.common.base.BaseServiceX;
import com.fruitisland.game.entity.GamePlayer;

public interface GamePlayerService extends BaseServiceX<GamePlayer> {

    /** 根据 userId 查找角色 */
    GamePlayer findByUserId(Long userId);

    /** 为新用户创建初始角色 */
    GamePlayer createPlayer(Long userId);
}
