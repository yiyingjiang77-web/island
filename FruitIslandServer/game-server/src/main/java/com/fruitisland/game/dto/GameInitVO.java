package com.fruitisland.game.dto;

import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.Inventory;
import com.fruitisland.game.entity.Island;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * /game/init 接口返回数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameInitVO implements Serializable {

    /** 玩家信息 */
    private GamePlayer player;

    /** 岛屿信息 */
    private Island island;

    /** 土地列表 */
    private List<LandVO> lands;

    /** 背包物品 */
    private List<Inventory> inventory;

    public static GameInitVO of(GamePlayer player, Island island, List<LandVO> lands, List<Inventory> inventory) {
        GameInitVO vo = new GameInitVO();
        vo.setPlayer(player);
        vo.setIsland(island);
        vo.setLands(lands);
        vo.setInventory(inventory);
        return vo;
    }
}
