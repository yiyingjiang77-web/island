package com.fruitisland.game.dto;

import com.fruitisland.game.entity.FlowerConfig;
import com.fruitisland.game.entity.PlayerFlowerRight;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FlowerCatalogVO {
    private List<FlowerConfig> flowers;
    private List<PlayerFlowerRight> rights;
}
