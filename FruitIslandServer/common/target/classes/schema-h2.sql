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
('strawberry_seed','草莓种子','SEED','strawberry_seed',1),
('cabbage_seed','小白菜种子','SEED','cabbage_seed',2),
('carrot_seed','胡萝卜种子','SEED','carrot_seed',5),
('tomato_seed','番茄种子','SEED','tomato_seed',4),
('potato_seed','土豆种子','SEED','potato_seed',3),
('chili_seed','辣椒种子','SEED','chili_seed',10),
('corn_seed','玉米种子','SEED','corn_seed',25);
