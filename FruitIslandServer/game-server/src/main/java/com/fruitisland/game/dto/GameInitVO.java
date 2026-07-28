package com.fruitisland.game.dto;

import com.fruitisland.game.entity.GamePlayer;
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

    /** 岛屿区域列表（Demo2 启用） */
    private List<?> areas;

    /** 土地列表（Demo2 启用） */
    private List<?> lands;

    /** 建筑列表（Demo2 启用） */
    private List<?> buildings;

    /** 背包物品（Demo2 启用） */
    private List<?> inventory;

    public static GameInitVO of(GamePlayer player, Island island) {
        GameInitVO vo = new GameInitVO();
        vo.setPlayer(player);
        vo.setIsland(island);
        vo.setAreas(List.of());
        vo.setLands(List.of());
        vo.setBuildings(List.of());
        vo.setInventory(List.of());
        return vo;
    }
}
