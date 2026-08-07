CREATE TABLE IF NOT EXISTS drink_shop_level_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    level TINYINT NOT NULL, required_island_level TINYINT NOT NULL,
    renovation_gold BIGINT NOT NULL, queue_capacity TINYINT NOT NULL,
    bar_capacity TINYINT NOT NULL, sale_interval_seconds INT NOT NULL,
    arrival_interval_seconds INT NOT NULL, ice_cream_enabled TINYINT NOT NULL DEFAULT 0,
    advanced_recipe_enabled TINYINT NOT NULL DEFAULT 0, config_version INT NOT NULL DEFAULT 1,
    effective_from DATETIME NOT NULL, enabled TINYINT NOT NULL DEFAULT 1,
    improvement_text VARCHAR(255) NOT NULL, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drink_shop_level_version (level, config_version)
);
CREATE TABLE IF NOT EXISTS player_drink_shop (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, player_id BIGINT NOT NULL,
    shop_level TINYINT NOT NULL DEFAULT 1, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_player_drink_shop (player_id)
);
INSERT INTO drink_shop_level_config
(level,required_island_level,renovation_gold,queue_capacity,bar_capacity,sale_interval_seconds,arrival_interval_seconds,ice_cream_enabled,advanced_recipe_enabled,config_version,effective_from,enabled,improvement_text) VALUES
(1,1,0,5,10,300,120,0,0,1,'2020-01-01',1,'饮品店开业，开放六个室外吧台'),
(2,2,500,6,11,300,120,0,0,1,'2020-01-01',1,'顾客队列与单吧台容量提升'),
(3,3,1000,6,12,285,120,0,0,1,'2020-01-01',1,'吧台容量提升，销售加快'),
(4,4,2000,6,13,285,120,0,0,1,'2020-01-01',1,'单吧台容量提升'),
(5,5,3500,6,14,285,120,1,0,1,'2020-01-01',1,'开放冰淇淋制作能力'),
(6,6,5500,6,16,285,105,1,0,1,'2020-01-01',1,'顾客到店加快，吧台容量提升'),
(7,7,8000,7,17,285,105,1,0,1,'2020-01-01',1,'顾客队列扩容'),
(8,8,11000,7,18,285,105,1,0,1,'2020-01-01',1,'单吧台容量提升'),
(9,9,15000,7,19,270,105,1,0,1,'2020-01-01',1,'销售加快'),
(10,10,20000,7,20,270,105,1,1,1,'2020-01-01',1,'开放高级配方制作能力')
ON DUPLICATE KEY UPDATE improvement_text=VALUES(improvement_text);
