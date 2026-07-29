-- Demo2.1 H2 测试表结构 + 种子数据
-- 使用反引号包裹保留字 user

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(32),
    avatar VARCHAR(255),
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_login (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    platform VARCHAR(32) NOT NULL,
    platform_uid VARCHAR(128) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_platform_uid ON user_login(platform, platform_uid);

CREATE TABLE IF NOT EXISTS user_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    expire_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_token ON user_token(token);

-- ========== 游戏表 ==========

CREATE TABLE IF NOT EXISTS game_player (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    game_id VARCHAR(32) DEFAULT 'fruit_island',
    nickname VARCHAR(32),
    level INT DEFAULT 1,
    exp INT DEFAULT 0,
    gold BIGINT DEFAULT 500,
    diamond INT DEFAULT 20,
    avatar_id VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_level_config (
    level INT PRIMARY KEY,
    required_exp INT NOT NULL,
    reward_gold BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS island (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    island_name VARCHAR(64),
    level INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS item_config (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64),
    type VARCHAR(32),
    icon VARCHAR(255),
    sell_price INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS crop_config (
    crop_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    rarity VARCHAR(16) NOT NULL DEFAULT 'COMMON',
    reward_eligible INT NOT NULL DEFAULT 0,
    permanent_unlock_enabled INT NOT NULL DEFAULT 1,
    upgrade_enabled INT NOT NULL DEFAULT 1,
    player_unlock_level INT NOT NULL DEFAULT 1,
    max_crop_level INT NOT NULL DEFAULT 1,
    enabled INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS crop_level_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crop_id VARCHAR(64) NOT NULL,
    crop_level INT NOT NULL,
    grow_seconds INT NOT NULL,
    yield_count INT NOT NULL,
    harvest_exp INT NOT NULL DEFAULT 0,
    upgrade_gold BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (crop_id, crop_level)
);

CREATE TABLE IF NOT EXISTS crop_unlock_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crop_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    currency_type VARCHAR(16) NOT NULL DEFAULT 'NONE',
    price BIGINT NOT NULL DEFAULT 0,
    required_player_level INT NOT NULL DEFAULT 1,
    source_ref_id VARCHAR(128),
    enabled INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_crop (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    crop_id VARCHAR(64) NOT NULL,
    crop_level INT NOT NULL DEFAULT 1,
    unlock_source VARCHAR(32) NOT NULL,
    unlock_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (player_id, crop_id)
);

CREATE TABLE IF NOT EXISTS player_crop_grant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    crop_id VARCHAR(64) NOT NULL,
    grant_crop_level INT NOT NULL DEFAULT 1,
    grant_source VARCHAR(64) NOT NULL,
    source_ref_id VARCHAR(128),
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS crop_reward_pool_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pool_code VARCHAR(64) NOT NULL,
    crop_id VARCHAR(64) NOT NULL,
    grant_crop_level INT NOT NULL DEFAULT 1,
    weight INT NOT NULL DEFAULT 1,
    duration_seconds BIGINT NOT NULL,
    enabled INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    count INT DEFAULT 0,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS land_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    area_type VARCHAR(16) NOT NULL,
    block_id VARCHAR(32) NOT NULL,
    grid_x INT NOT NULL,
    grid_y INT NOT NULL,
    unlock_level INT DEFAULT 1,
    buy_price BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_land (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    land_config_id BIGINT NOT NULL,
    status VARCHAR(16) DEFAULT 'EMPTY',
    crop_id VARCHAR(64),
    crop_level INT,
    grow_seconds_snapshot INT,
    yield_count_snapshot INT,
    harvest_exp_snapshot INT,
    access_type VARCHAR(16),
    access_grant_id BIGINT,
    plant_time TIMESTAMP,
    finish_time TIMESTAMP,
    water_level INT DEFAULT 100,
    last_watered_at TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS crop_plant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_land_id BIGINT NOT NULL,
    crop_id VARCHAR(64),
    crop_level INT,
    grow_seconds_snapshot INT,
    yield_count_snapshot INT,
    harvest_exp_snapshot INT,
    access_type VARCHAR(16),
    access_grant_id BIGINT,
    plant_time TIMESTAMP,
    finish_time TIMESTAMP,
    status VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========== 种子数据 ==========

-- Land Config (96 条)
INSERT INTO land_config (area_type, block_id, grid_x, grid_y, unlock_level, buy_price) VALUES
('FARM','Farm-A',0,0,1,0),('FARM','Farm-A',1,0,1,50),('FARM','Farm-A',2,0,1,50),('FARM','Farm-A',3,0,1,50),
('FARM','Farm-A',0,1,1,50),('FARM','Farm-A',1,1,1,100),('FARM','Farm-A',2,1,1,100),('FARM','Farm-A',3,1,1,100),
('FARM','Farm-A',0,2,1,50),('FARM','Farm-A',1,2,1,100),('FARM','Farm-A',2,2,1,150),('FARM','Farm-A',3,2,1,100),
('FARM','Farm-A',0,3,1,50),('FARM','Farm-A',1,3,1,100),('FARM','Farm-A',2,3,1,100),('FARM','Farm-A',3,3,1,150),
('FARM','Farm-B',0,0,3,100),('FARM','Farm-B',1,0,3,100),('FARM','Farm-B',2,0,3,100),('FARM','Farm-B',3,0,3,100),
('FARM','Farm-B',0,1,3,100),('FARM','Farm-B',1,1,3,150),('FARM','Farm-B',2,1,3,150),('FARM','Farm-B',3,1,3,150),
('FARM','Farm-B',0,2,3,100),('FARM','Farm-B',1,2,3,150),('FARM','Farm-B',2,2,3,200),('FARM','Farm-B',3,2,3,150),
('FARM','Farm-B',0,3,3,100),('FARM','Farm-B',1,3,3,150),('FARM','Farm-B',2,3,3,150),('FARM','Farm-B',3,3,3,200),
('FARM','Farm-C',0,0,5,200),('FARM','Farm-C',1,0,5,200),('FARM','Farm-C',2,0,5,200),('FARM','Farm-C',3,0,5,200),
('FARM','Farm-C',0,1,5,200),('FARM','Farm-C',1,1,5,300),('FARM','Farm-C',2,1,5,300),('FARM','Farm-C',3,1,5,300),
('FARM','Farm-C',0,2,5,200),('FARM','Farm-C',1,2,5,300),('FARM','Farm-C',2,2,5,400),('FARM','Farm-C',3,2,5,300),
('FARM','Farm-C',0,3,5,200),('FARM','Farm-C',1,3,5,300),('FARM','Farm-C',2,3,5,300),('FARM','Farm-C',3,3,5,400),
('FARM','Farm-D',0,0,8,400),('FARM','Farm-D',1,0,8,400),('FARM','Farm-D',2,0,8,400),('FARM','Farm-D',3,0,8,400),
('FARM','Farm-D',0,1,8,400),('FARM','Farm-D',1,1,8,500),('FARM','Farm-D',2,1,8,500),('FARM','Farm-D',3,1,8,500),
('FARM','Farm-D',0,2,8,400),('FARM','Farm-D',1,2,8,500),('FARM','Farm-D',2,2,8,600),('FARM','Farm-D',3,2,8,500),
('FARM','Farm-D',0,3,8,400),('FARM','Farm-D',1,3,8,500),('FARM','Farm-D',2,3,8,500),('FARM','Farm-D',3,3,8,600),
('FLOWER','Flower-A',0,0,10,500),('FLOWER','Flower-A',1,0,10,500),('FLOWER','Flower-A',2,0,10,500),('FLOWER','Flower-A',3,0,10,500),
('FLOWER','Flower-A',0,1,10,500),('FLOWER','Flower-A',1,1,10,800),('FLOWER','Flower-A',2,1,10,800),('FLOWER','Flower-A',3,1,10,800),
('FLOWER','Flower-A',0,2,10,500),('FLOWER','Flower-A',1,2,10,800),('FLOWER','Flower-A',2,2,10,1000),('FLOWER','Flower-A',3,2,10,800),
('FLOWER','Flower-A',0,3,10,500),('FLOWER','Flower-A',1,3,10,800),('FLOWER','Flower-A',2,3,10,800),('FLOWER','Flower-A',3,3,10,1000),
('FLOWER','Flower-B',0,0,13,800),('FLOWER','Flower-B',1,0,13,800),('FLOWER','Flower-B',2,0,13,800),('FLOWER','Flower-B',3,0,13,800),
('FLOWER','Flower-B',0,1,13,800),('FLOWER','Flower-B',1,1,13,1200),('FLOWER','Flower-B',2,1,13,1200),('FLOWER','Flower-B',3,1,13,1200),
('FLOWER','Flower-B',0,2,13,800),('FLOWER','Flower-B',1,2,13,1200),('FLOWER','Flower-B',2,2,13,1500),('FLOWER','Flower-B',3,2,13,1200),
('FLOWER','Flower-B',0,3,13,800),('FLOWER','Flower-B',1,3,13,1200),('FLOWER','Flower-B',2,3,13,1200),('FLOWER','Flower-B',3,3,13,1500);

-- Item Config (14 条)
INSERT INTO item_config (id, name, type, icon, sell_price) VALUES
('strawberry','草莓','CROP','strawberry',5),
('cabbage','小白菜','CROP','cabbage',3),
('carrot','胡萝卜','CROP','carrot',8),
('tomato','番茄','CROP','tomato',6),
('potato','土豆','CROP','potato',5),
('chili','辣椒','CROP','chili',20),
('corn','玉米','CROP','corn',40),
('moonberry','月光莓','CROP','moonberry',120);

INSERT INTO crop_config
(crop_id,name,rarity,reward_eligible,permanent_unlock_enabled,upgrade_enabled,
 player_unlock_level,max_crop_level,enabled) VALUES
('strawberry','草莓','COMMON',0,1,1,1,3,1),
('cabbage','白菜','COMMON',0,1,1,2,3,1),
('carrot','胡萝卜','COMMON',0,1,1,3,3,1),
('tomato','番茄','COMMON',0,1,1,4,3,1),
('potato','土豆','COMMON',0,1,1,5,3,1),
('chili','辣椒','COMMON',0,1,1,8,3,1),
('corn','玉米','COMMON',0,1,1,10,3,1),
('moonberry','月光莓','RARE',1,0,0,1,1,1);

INSERT INTO crop_level_config
(crop_id,crop_level,grow_seconds,yield_count,harvest_exp,upgrade_gold) VALUES
('strawberry',1,60,2,5,0),('strawberry',2,50,3,8,200),('strawberry',3,40,4,12,500),
('cabbage',1,120,2,8,0),('cabbage',2,105,3,12,300),('cabbage',3,90,4,18,700),
('carrot',1,180,3,12,0),('carrot',2,155,4,18,400),('carrot',3,130,5,25,900),
('tomato',1,240,3,15,0),('tomato',2,210,4,22,500),('tomato',3,180,5,30,1100),
('potato',1,300,4,18,0),('potato',2,270,5,26,600),('potato',3,240,6,36,1300),
('chili',1,480,3,25,0),('chili',2,420,4,36,900),('chili',3,360,5,50,1800),
('corn',1,600,5,30,0),('corn',2,520,6,45,1200),('corn',3,450,8,65,2500),
('moonberry',1,300,5,40,0);

INSERT INTO player_level_config (level,required_exp,reward_gold) VALUES
(1,100,50),(2,150,75),(3,220,100),(4,300,125),(5,400,150),
(6,520,180),(7,660,210),(8,820,250),(9,1000,300),(10,1200,350),
(11,1450,400),(12,1700,450),(13,2000,500),(14,2350,550),(15,2750,600),
(16,3200,700),(17,3700,800),(18,4250,900),(19,4850,1000),(20,5500,1200);

INSERT INTO crop_unlock_source
(crop_id,source_type,currency_type,price,required_player_level,source_ref_id,enabled) VALUES
('strawberry','INITIAL','NONE',0,1,'NEW_PLAYER',1),
('cabbage','GOLD_SHOP','GOLD',200,2,'SHOP_CABBAGE',1),
('carrot','GOLD_SHOP','GOLD',350,3,'SHOP_CARROT',1),
('tomato','DIAMOND_SHOP','DIAMOND',10,4,'SHOP_TOMATO',1),
('potato','LEVEL_REWARD','NONE',0,5,'LEVEL_5',1),
('chili','GOLD_SHOP','GOLD',1000,8,'SHOP_CHILI',1),
('corn','LEVEL_REWARD','NONE',0,10,'LEVEL_10',1);

INSERT INTO crop_reward_pool_item
(pool_code,crop_id,grant_crop_level,weight,duration_seconds,enabled) VALUES
('DAILY_RARE_CROP','moonberry',1,100,86400,1);
