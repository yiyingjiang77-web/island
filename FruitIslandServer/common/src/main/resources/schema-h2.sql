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
    cumulative_exp INT DEFAULT 0,
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

CREATE TABLE IF NOT EXISTS island_level_config (
    level INT PRIMARY KEY,
    cumulative_exp INT NOT NULL,
    crop_id VARCHAR(64) NOT NULL,
    recipe_id VARCHAR(64) NOT NULL,
    material_source_hint VARCHAR(255),
    shop_capability_hint VARCHAR(255),
    enabled INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_island_level_reward_claim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    island_level INT NOT NULL,
    claimed_at TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (player_id, island_level)
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
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (player_id, item_id)
);

CREATE TABLE IF NOT EXISTS recipe_config (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    output_item VARCHAR(64) NOT NULL,
    make_time INT NOT NULL DEFAULT 0,
    unlock_level INT NOT NULL DEFAULT 1,
    sale_gold INT NOT NULL DEFAULT 0,
    sale_exp INT NOT NULL DEFAULT 0,
    bar_sale_interval_seconds INT NOT NULL DEFAULT 180,
    order_weight INT NOT NULL DEFAULT 1,
    enabled INT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS recipe_material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id VARCHAR(64) NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    count INT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS player_recipe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    recipe_id VARCHAR(64) NOT NULL,
    qualification_type VARCHAR(16) NOT NULL DEFAULT 'PERMANENT',
    unlock_source VARCHAR(32) NOT NULL,
    unlock_time TIMESTAMP NOT NULL,
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (player_id, recipe_id, qualification_type)
);

CREATE TABLE IF NOT EXISTS drink_bar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    slot_number INT NOT NULL,
    opened INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (player_id, slot_number)
);

CREATE TABLE IF NOT EXISTS drink_bar_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    bar_id BIGINT NOT NULL,
    recipe_id VARCHAR(64) NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    listed_quantity INT NOT NULL,
    sold_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    active_marker INT,
    unit_gold_snapshot INT NOT NULL,
    unit_exp_snapshot INT NOT NULL,
    sale_interval_seconds_snapshot INT NOT NULL,
    listed_at TIMESTAMP NOT NULL,
    sold_out_at TIMESTAMP,
    closed_at TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bar_id, active_marker)
);

CREATE TABLE IF NOT EXISTS drink_shop_level_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    level INT NOT NULL,
    required_island_level INT NOT NULL,
    renovation_gold BIGINT NOT NULL,
    queue_capacity INT NOT NULL,
    bar_capacity INT NOT NULL,
    sale_interval_seconds INT NOT NULL,
    arrival_interval_seconds INT NOT NULL,
    ice_cream_enabled INT NOT NULL DEFAULT 0,
    advanced_recipe_enabled INT NOT NULL DEFAULT 0,
    config_version INT NOT NULL DEFAULT 1,
    effective_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enabled INT NOT NULL DEFAULT 1,
    improvement_text VARCHAR(255) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (level, config_version)
);

CREATE TABLE IF NOT EXISTS satisfaction_gift_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, tier_code VARCHAR(16) NOT NULL,
    minimum_percent INT NOT NULL, minimum_delivered_quantity INT NOT NULL,
    reward_gold BIGINT NOT NULL, config_version INT NOT NULL DEFAULT 1,
    effective_from DATE NOT NULL, enabled INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tier_code, config_version)
);
CREATE TABLE IF NOT EXISTS daily_satisfaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, player_id BIGINT NOT NULL,
    business_date DATE NOT NULL, delivered_orders INT NOT NULL, rejected_orders INT NOT NULL,
    closed_orders INT NOT NULL, delivered_quantity INT NOT NULL, satisfaction_percent INT NOT NULL,
    gift_tier_snapshot VARCHAR(16), reward_gold_snapshot BIGINT NOT NULL DEFAULT 0,
    reward_status VARCHAR(24) NOT NULL, settled_at TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE (player_id, business_date)
);
INSERT IGNORE INTO satisfaction_gift_config
(tier_code,minimum_percent,minimum_delivered_quantity,reward_gold,config_version,effective_from,enabled) VALUES
('S60',60,20,100,1,'2020-01-01',1),('S70',70,20,200,1,'2020-01-01',1),
('S80',80,20,300,1,'2020-01-01',1),('S90',90,20,400,1,'2020-01-01',1),
('S100',100,20,500,1,'2020-01-01',1);

CREATE TABLE IF NOT EXISTS player_drink_shop (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL UNIQUE,
    shop_level INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO drink_shop_level_config
(level,required_island_level,renovation_gold,queue_capacity,bar_capacity,sale_interval_seconds,arrival_interval_seconds,ice_cream_enabled,advanced_recipe_enabled,config_version,effective_from,enabled,improvement_text) VALUES
(1,1,0,5,10,300,120,0,0,1,'2020-01-01 00:00:00',1,'饮品店开业，开放六个室外吧台'),
(2,2,500,6,11,300,120,0,0,1,'2020-01-01 00:00:00',1,'顾客队列与单吧台容量提升'),
(3,3,1000,6,12,285,120,0,0,1,'2020-01-01 00:00:00',1,'吧台容量提升，销售加快'),
(4,4,2000,6,13,285,120,0,0,1,'2020-01-01 00:00:00',1,'单吧台容量提升'),
(5,5,3500,6,14,285,120,1,0,1,'2020-01-01 00:00:00',1,'开放冰淇淋制作能力'),
(6,6,5500,6,16,285,105,1,0,1,'2020-01-01 00:00:00',1,'顾客到店加快，吧台容量提升'),
(7,7,8000,7,17,285,105,1,0,1,'2020-01-01 00:00:00',1,'顾客队列扩容'),
(8,8,11000,7,18,285,105,1,0,1,'2020-01-01 00:00:00',1,'单吧台容量提升'),
(9,9,15000,7,19,270,105,1,0,1,'2020-01-01 00:00:00',1,'销售加快'),
(10,10,20000,7,20,270,105,1,1,1,'2020-01-01 00:00:00',1,'开放高级配方制作能力');

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

CREATE TABLE IF NOT EXISTS customer_template (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(32),
    avatar VARCHAR(255),
    type VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS customer_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT,
    customer_id VARCHAR(64),
    recipe_id VARCHAR(64),
    item_id VARCHAR(64),
    quantity INT NOT NULL DEFAULT 1,
    unit_gold_snapshot INT NOT NULL DEFAULT 0,
    unit_exp_snapshot INT NOT NULL DEFAULT 0,
    queue_position INT,
    status VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    close_time TIMESTAMP,
    close_reason VARCHAR(32)
);
CREATE INDEX IF NOT EXISTS idx_waiting_queue ON customer_order(player_id, status, queue_position);

CREATE TABLE IF NOT EXISTS customer_arrival_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    next_arrival_at TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (player_id)
);

CREATE TABLE IF NOT EXISTS order_quantity_weight (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantity INT NOT NULL,
    weight INT NOT NULL,
    enabled INT NOT NULL DEFAULT 1,
    UNIQUE (quantity)
);

INSERT INTO customer_template (id, name, avatar, type) VALUES
('berry', '莓莓', 'girl', 'ISLANDER'),
('sunny', '小晴', 'boy', 'ISLANDER'),
('captain', '船长', 'man', 'VISITOR');

INSERT INTO order_quantity_weight (quantity, weight, enabled) VALUES
(1, 60, 1), (2, 30, 1), (3, 10, 1);

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

-- Item Config (基础作物 + 花卉 + 蜂蜜 + 材料)
INSERT INTO item_config (id, name, type, icon, sell_price) VALUES
('strawberry','草莓','CROP','strawberry',5),
('carrot','胡萝卜','CROP','carrot',8),
('tomato','番茄','CROP','tomato',6),
('moonberry','月光莓','CROP','moonberry',120),
('orange','橙子','CROP','orange',10),
('blueberry','蓝莓','CROP','blueberry',12),
('apple','苹果','CROP','apple',14),
('watermelon','西瓜','CROP','watermelon',16),
('wheat','小麦','CROP','wheat',10),
('lemon','柠檬','CROP','lemon',14),
('cucumber','黄瓜','CROP','cucumber',12),
('milk','牛奶','MATERIAL','milk',0),
('egg','鸡蛋','MATERIAL','egg',0),
('strawberry_juice','草莓汁','DRINK','strawberry_juice',0),
('rose','玫瑰','FLOWER','rose',5),
('chrysanthemum','菊花','FLOWER','chrysanthemum',3),
('jasmine','茉莉花','FLOWER','jasmine',4),
('osmanthus','桂花','FLOWER','osmanthus',5),
('lavender','薰衣草','FLOWER','lavender',6),
('hibiscus','洛神花','FLOWER','hibiscus',4),
('chamomile','洋甘菊','FLOWER','chamomile',3),
('sakura','樱花','FLOWER','sakura',10),
('honey','蜂蜜','MATERIAL','honey',15);

INSERT INTO crop_config
(crop_id,name,rarity,reward_eligible,permanent_unlock_enabled,upgrade_enabled,
 player_unlock_level,max_crop_level,enabled) VALUES
('strawberry','草莓','COMMON',0,1,1,1,10,1),
('carrot','胡萝卜','COMMON',0,1,1,2,10,1),
('tomato','番茄','COMMON',0,1,1,4,10,1),
('moonberry','月光莓','RARE',1,0,0,1,1,1),
('orange','橙子','COMMON',0,1,1,3,10,1),
('blueberry','蓝莓','COMMON',0,1,1,5,10,1),
('apple','苹果','COMMON',0,1,1,6,10,1),
('watermelon','西瓜','COMMON',0,1,1,7,10,1),
('wheat','小麦','COMMON',0,1,1,8,10,1),
('lemon','柠檬','COMMON',0,1,1,9,10,1),
('cucumber','黄瓜','COMMON',0,1,1,10,10,1);

INSERT INTO recipe_config
(id,name,output_item,make_time,unlock_level,sale_gold,sale_exp,
 bar_sale_interval_seconds,order_weight,enabled) VALUES
('strawberry_juice','草莓汁','strawberry_juice',0,1,30,5,180,100,1),
('carrot_juice','胡萝卜汁','carrot_juice',0,2,35,5,180,100,1),
('orange_juice','橙汁','orange_juice',0,3,40,6,180,100,1),
('tomato_juice','番茄汁','tomato_juice',0,4,40,6,180,100,1),
('milk_ice_cream','牛奶冰淇淋','milk_ice_cream',0,5,55,8,180,100,1),
('apple_carrot_juice','苹果胡萝卜汁','apple_carrot_juice',0,6,50,7,180,100,1),
('watermelon_milk_ice_cream','西瓜牛奶冰淇淋','watermelon_milk_ice_cream',0,7,65,9,180,100,1),
('strawberry_cake','草莓蛋糕','strawberry_cake',0,8,80,12,180,100,1),
('lemon_milk_ice_cream','柠檬牛奶冰淇淋','lemon_milk_ice_cream',0,9,70,10,180,100,1),
('cucumber_apple_juice','黄瓜苹果汁','cucumber_apple_juice',0,10,60,8,180,100,1);

INSERT INTO recipe_material (recipe_id,item_id,count) VALUES
('strawberry_juice','strawberry',2),
('carrot_juice','carrot',2),
('orange_juice','orange',2),
('tomato_juice','tomato',2),
('milk_ice_cream','milk',2),
('apple_carrot_juice','apple',1),
('apple_carrot_juice','carrot',1),
('watermelon_milk_ice_cream','watermelon',2),
('watermelon_milk_ice_cream','milk',1),
('strawberry_cake','strawberry',2),
('strawberry_cake','wheat',2),
('strawberry_cake','egg',1),
('lemon_milk_ice_cream','lemon',2),
('lemon_milk_ice_cream','milk',1),
('cucumber_apple_juice','cucumber',1),
('cucumber_apple_juice','apple',1);

INSERT INTO crop_level_config
(crop_id,crop_level,grow_seconds,yield_count,harvest_exp,upgrade_gold) VALUES
('strawberry',1,60,2,4,0),('strawberry',2,57,3,5,200),('strawberry',3,54,3,5,400),('strawberry',4,51,4,6,800),('strawberry',5,48,4,6,1400),
('strawberry',6,45,5,7,2200),('strawberry',7,42,5,8,3200),('strawberry',8,39,6,9,4400),('strawberry',9,36,6,10,5800),('strawberry',10,30,7,11,7400),
('carrot',1,180,3,6,0),('carrot',2,171,4,7,400),('carrot',3,162,4,8,800),('carrot',4,153,5,9,1600),('carrot',5,144,5,10,2800),
('carrot',6,135,6,11,4400),('carrot',7,126,6,12,6400),('carrot',8,117,7,14,8800),('carrot',9,108,7,15,11600),('carrot',10,90,8,17,14800),
('orange',1,240,3,8,0),('orange',2,228,4,9,500),('orange',3,216,4,10,1000),('orange',4,204,5,12,2000),('orange',5,192,5,13,3500),
('orange',6,180,6,14,5500),('orange',7,168,6,16,8000),('orange',8,156,7,18,11000),('orange',9,144,7,20,14500),('orange',10,120,8,22,18500),
('tomato',1,240,3,8,0),('tomato',2,228,4,9,500),('tomato',3,216,4,10,1000),('tomato',4,204,5,12,2000),('tomato',5,192,5,13,3500),
('tomato',6,180,6,14,5500),('tomato',7,168,6,16,8000),('tomato',8,156,7,18,11000),('tomato',9,144,7,20,14500),('tomato',10,120,8,22,18500),
('blueberry',1,300,4,10,0),('blueberry',2,285,5,12,600),('blueberry',3,270,5,13,1200),('blueberry',4,255,6,14,2400),('blueberry',5,240,6,16,4200),
('blueberry',6,225,7,18,6600),('blueberry',7,210,7,20,9600),('blueberry',8,195,8,22,13200),('blueberry',9,180,8,25,17400),('blueberry',10,150,8,28,22200),
('apple',1,360,4,12,0),('apple',2,342,5,14,700),('apple',3,324,5,16,1400),('apple',4,306,6,17,2800),('apple',5,288,6,19,4900),
('apple',6,270,7,22,7700),('apple',7,252,7,24,11200),('apple',8,234,8,27,15400),('apple',9,216,8,30,20300),('apple',10,180,8,34,25900),
('watermelon',1,480,3,14,0),('watermelon',2,456,4,16,900),('watermelon',3,432,4,18,1800),('watermelon',4,408,5,20,3600),('watermelon',5,384,5,22,6300),
('watermelon',6,360,6,25,9900),('watermelon',7,336,6,28,14400),('watermelon',8,312,7,32,19800),('watermelon',9,288,7,35,26100),('watermelon',10,240,8,39,33300),
('wheat',1,300,5,10,0),('wheat',2,285,6,12,800),('wheat',3,270,6,13,1600),('wheat',4,255,7,14,3200),('wheat',5,240,7,16,5600),
('wheat',6,225,8,18,8800),('wheat',7,210,8,20,12800),('wheat',8,195,8,22,17600),('wheat',9,180,8,25,23200),('wheat',10,150,8,28,29600),
('lemon',1,420,4,12,0),('lemon',2,399,5,14,900),('lemon',3,378,5,16,1800),('lemon',4,357,6,17,3600),('lemon',5,336,6,19,6300),
('lemon',6,315,7,22,9900),('lemon',7,294,7,24,14400),('lemon',8,273,8,27,19800),('lemon',9,252,8,30,26100),('lemon',10,210,8,34,33300),
('cucumber',1,360,4,12,0),('cucumber',2,342,5,14,800),('cucumber',3,324,5,16,1600),('cucumber',4,306,6,17,3200),('cucumber',5,288,6,19,5600),
('cucumber',6,270,7,22,8800),('cucumber',7,252,7,24,12800),('cucumber',8,234,8,27,17600),('cucumber',9,216,8,30,23200),('cucumber',10,180,8,34,29600),
('moonberry',1,600,2,25,0);

INSERT INTO player_level_config (level,required_exp,reward_gold) VALUES
(1,100,50),(2,150,75),(3,220,100),(4,300,125),(5,400,150),
(6,520,180),(7,660,210),(8,820,250),(9,1000,300),(10,1200,350),
(11,1450,400),(12,1700,450),(13,2000,500),(14,2350,550),(15,2750,600),
(16,3200,700),(17,3700,800),(18,4250,900),(19,4850,1000),(20,5500,1200);

INSERT INTO island_level_config
(level,cumulative_exp,crop_id,recipe_id,material_source_hint,shop_capability_hint,enabled) VALUES
(1,0,'strawberry','strawberry_juice',NULL,NULL,1),
(2,100,'carrot','carrot_juice',NULL,NULL,1),
(3,250,'orange','orange_juice',NULL,NULL,1),
(4,450,'tomato','tomato_juice',NULL,NULL,1),
(5,700,'blueberry','milk_ice_cream','解锁牛棚后可获得牛奶材料','饮品店达到5级后可制作冰淇淋',1),
(6,1000,'apple','apple_carrot_juice',NULL,NULL,1),
(7,1400,'watermelon','watermelon_milk_ice_cream','解锁牛棚后可获得牛奶材料','饮品店达到5级后可制作冰淇淋',1),
(8,1900,'wheat','strawberry_cake','解锁鸡舍后可获得鸡蛋材料','解锁蛋糕店后可制作蛋糕',1),
(9,2500,'lemon','lemon_milk_ice_cream','解锁牛棚后可获得牛奶材料','饮品店达到5级后可制作冰淇淋',1),
(10,3200,'cucumber','cucumber_apple_juice',NULL,NULL,1);

INSERT INTO crop_unlock_source
(crop_id,source_type,currency_type,price,required_player_level,source_ref_id,enabled) VALUES
('strawberry','INITIAL','NONE',0,1,'NEW_PLAYER',1),
('carrot','LEVEL_REWARD','NONE',0,2,'ISLAND_LEVEL_2',1),
('orange','LEVEL_REWARD','NONE',0,3,'ISLAND_LEVEL_3',1),
('tomato','LEVEL_REWARD','NONE',0,4,'ISLAND_LEVEL_4',1),
('blueberry','LEVEL_REWARD','NONE',0,5,'ISLAND_LEVEL_5',1),
('apple','LEVEL_REWARD','NONE',0,6,'ISLAND_LEVEL_6',1),
('watermelon','LEVEL_REWARD','NONE',0,7,'ISLAND_LEVEL_7',1),
('wheat','LEVEL_REWARD','NONE',0,8,'ISLAND_LEVEL_8',1),
('lemon','LEVEL_REWARD','NONE',0,9,'ISLAND_LEVEL_9',1),
('cucumber','LEVEL_REWARD','NONE',0,10,'ISLAND_LEVEL_10',1);

INSERT INTO crop_reward_pool_item
(pool_code,crop_id,grant_crop_level,weight,duration_seconds,enabled) VALUES
('DAILY_RARE_CROP','moonberry',1,100,86400,1);

-- ========== 花卉系统表 ==========

CREATE TABLE IF NOT EXISTS flower_config (
    flower_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    currency_type VARCHAR(16) NOT NULL DEFAULT 'GOLD',
    seed_price BIGINT NOT NULL DEFAULT 0,
    grow_seconds INT NOT NULL,
    yield_count INT NOT NULL,
    harvest_exp INT NOT NULL DEFAULT 0,
    honey_coefficient INT NOT NULL DEFAULT 1,
    max_level INT NOT NULL DEFAULT 10,
    enabled INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS flower_level_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flower_id VARCHAR(64) NOT NULL,
    flower_level INT NOT NULL,
    grow_seconds INT NOT NULL,
    yield_count INT NOT NULL,
    harvest_exp INT NOT NULL DEFAULT 0,
    upgrade_gold BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (flower_id, flower_level)
);

CREATE TABLE IF NOT EXISTS player_flower_right (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    flower_id VARCHAR(64) NOT NULL,
    flower_level INT NOT NULL DEFAULT 1,
    unlock_source VARCHAR(32) NOT NULL,
    unlock_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (player_id, flower_id)
);

CREATE TABLE IF NOT EXISTS player_beehive (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    beehive_count INT NOT NULL DEFAULT 0,
    honey_stored INT NOT NULL DEFAULT 0,
    last_produce_time TIMESTAMP,
    last_collect_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (player_id)
);

INSERT INTO flower_config
(flower_id, name, currency_type, seed_price, grow_seconds, yield_count, harvest_exp, honey_coefficient, max_level, enabled) VALUES
('rose','玫瑰','GOLD',500,300,2,4,1,10,1),
('chrysanthemum','菊花','GOLD',300,240,3,3,1,10,1),
('jasmine','茉莉花','GOLD',400,300,3,3,1,10,1),
('osmanthus','桂花','GOLD',600,360,3,4,1,10,1),
('lavender','薰衣草','GOLD',800,420,2,5,1,10,1),
('hibiscus','洛神花','GOLD',500,300,3,4,1,10,1),
('chamomile','洋甘菊','GOLD',300,240,3,3,1,10,1),
('sakura','樱花','DIAMOND',10,480,2,5,2,10,1);

INSERT INTO flower_level_config
(flower_id, flower_level, grow_seconds, yield_count, harvest_exp, upgrade_gold) VALUES
('rose',1,300,2,4,0),('rose',2,285,3,5,500),('rose',3,270,3,5,1000),('rose',4,255,4,6,2000),('rose',5,240,4,6,3500),
('rose',6,225,5,7,5500),('rose',7,210,5,8,8000),('rose',8,195,6,9,11000),('rose',9,180,6,10,14500),('rose',10,150,7,11,18500),
('chrysanthemum',1,240,3,3,0),('chrysanthemum',2,228,4,4,300),('chrysanthemum',3,216,4,4,600),('chrysanthemum',4,204,5,4,1200),('chrysanthemum',5,192,5,5,2100),
('chrysanthemum',6,180,6,5,3300),('chrysanthemum',7,168,6,6,4800),('chrysanthemum',8,156,7,7,6600),('chrysanthemum',9,144,7,8,8700),('chrysanthemum',10,120,8,8,11100),
('jasmine',1,300,3,3,0),('jasmine',2,285,4,4,400),('jasmine',3,270,4,4,800),('jasmine',4,255,5,4,1600),('jasmine',5,240,5,5,2800),
('jasmine',6,225,6,5,4400),('jasmine',7,210,6,6,6400),('jasmine',8,195,7,7,8800),('jasmine',9,180,7,8,11600),('jasmine',10,150,8,8,14800),
('osmanthus',1,360,3,4,0),('osmanthus',2,342,4,5,600),('osmanthus',3,324,4,5,1200),('osmanthus',4,306,5,6,2400),('osmanthus',5,288,5,6,4200),
('osmanthus',6,270,6,7,6600),('osmanthus',7,252,6,8,9600),('osmanthus',8,234,7,9,13200),('osmanthus',9,216,7,10,17400),('osmanthus',10,180,8,11,22200),
('lavender',1,420,2,5,0),('lavender',2,399,3,6,800),('lavender',3,378,3,7,1600),('lavender',4,357,4,7,3200),('lavender',5,336,4,8,5600),
('lavender',6,315,5,9,8800),('lavender',7,294,5,10,12800),('lavender',8,273,6,11,17600),('lavender',9,252,6,13,23200),('lavender',10,210,7,14,29600),
('hibiscus',1,300,3,4,0),('hibiscus',2,285,4,5,500),('hibiscus',3,270,4,5,1000),('hibiscus',4,255,5,6,2000),('hibiscus',5,240,5,6,3500),
('hibiscus',6,225,6,7,5500),('hibiscus',7,210,6,8,8000),('hibiscus',8,195,7,9,11000),('hibiscus',9,180,7,10,14500),('hibiscus',10,150,8,11,18500),
('chamomile',1,240,3,3,0),('chamomile',2,228,4,4,300),('chamomile',3,216,4,4,600),('chamomile',4,204,5,4,1200),('chamomile',5,192,5,5,2100),
('chamomile',6,180,6,5,3300),('chamomile',7,168,6,6,4800),('chamomile',8,156,7,7,6600),('chamomile',9,144,7,8,8700),('chamomile',10,120,8,8,11100),
('sakura',1,480,2,5,0),('sakura',2,456,3,6,1200),('sakura',3,432,3,7,2400),('sakura',4,408,4,7,4800),('sakura',5,384,4,8,8400),
('sakura',6,360,5,9,13200),('sakura',7,336,5,10,19200),('sakura',8,312,6,11,26400),('sakura',9,288,6,13,34800),('sakura',10,240,7,14,44400);
