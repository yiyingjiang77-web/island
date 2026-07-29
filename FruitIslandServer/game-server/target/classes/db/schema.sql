-- ============================================================
-- Fruit Island Game Database Schema
-- ============================================================

-- 与 application-common.yml 的 JDBC 地址保持一致，避免外键或查询跨到错误数据库。
CREATE DATABASE IF NOT EXISTS fruit_island
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE fruit_island;

-- ============================================================
-- 1. Game Player
-- ============================================================
CREATE TABLE game_player
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    user_id    BIGINT      NOT NULL COMMENT '用户中心ID',
    game_id    VARCHAR(32) NOT NULL DEFAULT 'fruit_island' COMMENT '游戏ID',
    nickname   VARCHAR(32) COMMENT '岛主名称',
    level      INT                  DEFAULT 1 COMMENT '等级',
    exp        INT                  DEFAULT 0 COMMENT '经验',
    gold       BIGINT               DEFAULT 1000 COMMENT '金币',
    diamond    INT                  DEFAULT 20 COMMENT '钻石',
    avatar_id  VARCHAR(64) COMMENT '角色形象',
    create_time DATETIME            DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) COMMENT = '游戏角色表';

-- ============================================================
-- 2. Island
-- ============================================================
CREATE TABLE island
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '岛屿ID',
    player_id   BIGINT NOT NULL COMMENT '角色ID',
    island_name VARCHAR(64) COMMENT '岛屿名称',
    level       INT    DEFAULT 1 COMMENT '岛屿等级',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_player_id (player_id)
) COMMENT = '玩家岛屿表';

-- ============================================================
-- 3. Island Area
-- ============================================================
CREATE TABLE island_area
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '区域ID',
    island_id    BIGINT      NOT NULL COMMENT '所属岛屿',
    area_type    VARCHAR(32) NOT NULL COMMENT '区域类型',
    area_name    VARCHAR(64) COMMENT '区域名称',
    unlock_level INT         DEFAULT 1 COMMENT '解锁等级',
    unlock_cost  BIGINT      DEFAULT 0 COMMENT '解锁金币',
    status       TINYINT     DEFAULT 0 COMMENT '0锁定 1开启',
    position_x   INT         DEFAULT 0,
    position_y   INT         DEFAULT 0,
    create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_island_id (island_id)
) COMMENT = '岛屿区域表';

-- ============================================================
-- 4. Land Config (全局土地配置)
-- ============================================================
CREATE TABLE land_config
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    area_type    VARCHAR(16)  NOT NULL COMMENT 'FARM / FLOWER',
    block_id     VARCHAR(32)  NOT NULL COMMENT 'Farm-A, Farm-B, Flower-A 等',
    grid_x       INT          NOT NULL COMMENT 'Block内X坐标 (0-3)',
    grid_y       INT          NOT NULL COMMENT 'Block内Y坐标 (0-3)',
    unlock_level INT          DEFAULT 1 COMMENT '解锁等级',
    buy_price    BIGINT       DEFAULT 0 COMMENT '购买价格',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_area_type (area_type),
    INDEX idx_block_id (block_id)
) COMMENT = '土地配置表';

-- ============================================================
-- 5. Player Land (玩家土地数据)
-- ============================================================
CREATE TABLE player_land
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '土地ID',
    player_id      BIGINT       NOT NULL COMMENT '玩家ID',
    land_config_id BIGINT       NOT NULL COMMENT 'land_config.id',
    status         VARCHAR(16)  DEFAULT 'EMPTY' COMMENT 'EMPTY / PLANTED / READY',
    crop_id        VARCHAR(64)  COMMENT '当前种植作物ID',
    crop_level     INT          COMMENT '种植时作物等级快照',
    grow_seconds_snapshot INT   COMMENT '本轮成熟秒数快照',
    yield_count_snapshot INT    COMMENT '本轮收获数量快照',
    access_type    VARCHAR(16)  COMMENT '种植权限来源：PERMANENT / TEMPORARY',
    access_grant_id BIGINT      COMMENT '限时权限ID，永久种植时为空',
    plant_time     DATETIME     COMMENT '种植时间',
    finish_time    DATETIME     COMMENT '成熟时间',
    water_level    INT          DEFAULT 100 COMMENT '水分值 0-100',
    last_watered_at DATETIME    COMMENT '上次浇水时间',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_player_id (player_id),
    INDEX idx_land_config_id (land_config_id)
) COMMENT = '玩家土地表';

-- ============================================================
-- 6. Crop Plant (种植历史记录)
-- ============================================================
CREATE TABLE crop_plant
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_land_id BIGINT NOT NULL COMMENT 'player_land.id',
    crop_id        VARCHAR(64) COMMENT '作物ID',
    crop_level     INT COMMENT '种植时作物等级快照',
    grow_seconds_snapshot INT COMMENT '种植时成熟秒数快照',
    yield_count_snapshot INT COMMENT '种植时收获数量快照',
    access_type    VARCHAR(16) COMMENT 'PERMANENT / TEMPORARY',
    access_grant_id BIGINT COMMENT '限时种植权限ID',
    plant_time     DATETIME COMMENT '种植时间',
    finish_time    DATETIME COMMENT '成熟时间',
    status         VARCHAR(32) COMMENT '状态',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_player_land_id (player_land_id)
) COMMENT = '作物种植记录';

-- ============================================================
-- 7. Item Config
-- ============================================================
CREATE TABLE item_config
(
    id         VARCHAR(64) PRIMARY KEY COMMENT '物品ID',
    name       VARCHAR(64) COMMENT '名称',
    type       VARCHAR(32) COMMENT '类型',
    icon       VARCHAR(255) COMMENT '图标',
    sell_price INT DEFAULT 0 COMMENT '出售价格',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT = '物品配置表';

-- ============================================================
-- 8. Crop Config
-- ============================================================
CREATE TABLE crop_config
(
    crop_id                  VARCHAR(64) PRIMARY KEY COMMENT '作物唯一编码，也是收获物品ID',
    name                     VARCHAR(64) NOT NULL COMMENT '作物显示名称',
    rarity                   VARCHAR(16) NOT NULL DEFAULT 'COMMON' COMMENT 'COMMON/RARE/EPIC/LEGENDARY',
    reward_eligible          TINYINT NOT NULL DEFAULT 0 COMMENT '是否可进入随机奖励池：0否1是',
    permanent_unlock_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许永久获得：0否1是',
    upgrade_enabled          TINYINT NOT NULL DEFAULT 1 COMMENT '永久拥有后是否可金币升级：0否1是',
    player_unlock_level      INT NOT NULL DEFAULT 1 COMMENT '玩家达到多少级后可以种植',
    max_crop_level           INT NOT NULL DEFAULT 1 COMMENT '作物最高等级',
    enabled                  TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否1是',
    create_time              DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time              DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_player_unlock_level (player_unlock_level),
    INDEX idx_reward (reward_eligible, rarity),
    CHECK (reward_eligible = 0 OR rarity <> 'COMMON')
) COMMENT = '作物基础配置表';

CREATE TABLE crop_level_config
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    crop_id         VARCHAR(64) NOT NULL COMMENT '作物编码',
    crop_level      INT NOT NULL COMMENT '作物等级，从1开始',
    grow_seconds    INT NOT NULL COMMENT '浇水后成熟秒数',
    yield_count     INT NOT NULL COMMENT '单次收获数量',
    upgrade_gold    BIGINT NOT NULL DEFAULT 0 COMMENT '从上一等级升到本等级所需金币；1级为0',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_crop_level (crop_id, crop_level),
    CHECK (crop_level >= 1 AND grow_seconds > 0 AND yield_count > 0 AND upgrade_gold >= 0)
) COMMENT = '作物等级数值配置表';

CREATE TABLE crop_unlock_source
(
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '渠道配置ID',
    crop_id               VARCHAR(64) NOT NULL COMMENT '作物编码',
    source_type           VARCHAR(32) NOT NULL COMMENT 'INITIAL/GOLD_SHOP/DIAMOND_SHOP/LEVEL_REWARD',
    currency_type         VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/GOLD/DIAMOND',
    price                 BIGINT NOT NULL DEFAULT 0 COMMENT '获得永久种植权所需价格',
    required_player_level INT NOT NULL DEFAULT 1 COMMENT '使用渠道所需玩家等级',
    source_ref_id         VARCHAR(128) COMMENT '商店商品或等级奖励等外部配置编号',
    enabled               TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否1是',
    create_time           DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_crop_source (crop_id, source_type, source_ref_id),
    INDEX idx_unlock_source (source_type, enabled),
    CHECK (price >= 0 AND required_player_level >= 1)
) COMMENT = '作物永久种植权获得渠道配置';

CREATE TABLE player_crop
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    player_id     BIGINT NOT NULL COMMENT '玩家角色ID',
    crop_id       VARCHAR(64) NOT NULL COMMENT '永久拥有的作物编码',
    crop_level    INT NOT NULL DEFAULT 1 COMMENT '当前作物等级',
    unlock_source VARCHAR(32) NOT NULL COMMENT 'INITIAL/GOLD_SHOP/DIAMOND_SHOP/LEVEL_REWARD等',
    unlock_time   DATETIME NOT NULL COMMENT '永久种植权获得时间',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_player_crop (player_id, crop_id),
    INDEX idx_player_crop_player (player_id)
) COMMENT = '玩家永久作物权限表；记录存在即可无限次种植';

CREATE TABLE player_crop_grant
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '限时权限ID',
    player_id        BIGINT NOT NULL COMMENT '玩家角色ID',
    crop_id          VARCHAR(64) NOT NULL COMMENT '限时可种植的稀有作物',
    grant_crop_level INT NOT NULL DEFAULT 1 COMMENT '奖励指定的固定作物等级',
    grant_source     VARCHAR(64) NOT NULL COMMENT 'QUEST/EVENT/GIFT/REWARD_POOL等',
    source_ref_id    VARCHAR(128) COMMENT '任务、活动或奖励流水编号',
    valid_from       DATETIME NOT NULL COMMENT '生效时间',
    valid_until      DATETIME NOT NULL COMMENT '失效时间',
    status           VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/REVOKED',
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_grant_active (player_id, crop_id, status, valid_until),
    CHECK (valid_until > valid_from)
) COMMENT = '玩家限时稀有作物权限；有效期内无限种植但不可升级';

CREATE TABLE crop_reward_pool_item
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '奖励项ID',
    pool_code         VARCHAR(64) NOT NULL COMMENT '奖励池编码',
    crop_id           VARCHAR(64) NOT NULL COMMENT '可奖励的稀有作物编码',
    grant_crop_level  INT NOT NULL DEFAULT 1 COMMENT '发放的固定作物等级',
    weight            INT NOT NULL DEFAULT 1 COMMENT '随机权重',
    duration_seconds  BIGINT NOT NULL COMMENT '限时种植权有效秒数',
    enabled           TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否1是',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_reward_pool (pool_code, enabled),
    CHECK (weight > 0 AND duration_seconds > 0)
) COMMENT = '稀有作物随机奖励池明细';

-- ============================================================
-- 9. Inventory
-- ============================================================
CREATE TABLE inventory
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id   BIGINT NOT NULL,
    item_id     VARCHAR(64) NOT NULL,
    count       INT    DEFAULT 0,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_player_item (player_id, item_id),
    INDEX idx_player_id (player_id)
) COMMENT = '玩家背包';

-- ============================================================
-- 9. Building
-- ============================================================
CREATE TABLE building
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    area_id     BIGINT NOT NULL COMMENT '所属区域',
    player_id   BIGINT NOT NULL,
    type        VARCHAR(32) COMMENT '建筑类型',
    level       INT    DEFAULT 1,
    position_x  INT,
    position_y  INT,
    rotation    INT    DEFAULT 0,
    status      TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_player_id (player_id),
    INDEX idx_area_id (area_id)
) COMMENT = '建筑表';

-- ============================================================
-- 10. Building Upgrade
-- ============================================================
CREATE TABLE building_upgrade
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    building_id  BIGINT NOT NULL,
    old_level    INT,
    new_level    INT,
    cost_gold    INT,
    upgrade_time DATETIME
) COMMENT = '建筑升级记录';

-- ============================================================
-- 11. Recipe Config
-- ============================================================
CREATE TABLE recipe_config
(
    id           VARCHAR(64) PRIMARY KEY,
    name         VARCHAR(64),
    output_item  VARCHAR(64),
    make_time    INT COMMENT '制作秒数',
    unlock_level INT DEFAULT 1
) COMMENT = '制作配方';

-- ============================================================
-- 12. Recipe Material
-- ============================================================
CREATE TABLE recipe_material
(
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_id VARCHAR(64),
    item_id   VARCHAR(64),
    count     INT DEFAULT 1
) COMMENT = '配方材料';

-- ============================================================
-- 13. Production Order
-- ============================================================
CREATE TABLE production_order
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id   BIGINT,
    building_id BIGINT,
    recipe_id   VARCHAR(64),
    start_time  DATETIME,
    finish_time DATETIME,
    status      VARCHAR(32),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT = '生产订单';

-- ============================================================
-- 14. Animal
-- ============================================================
CREATE TABLE animal
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id   BIGINT,
    area_id     BIGINT,
    type        VARCHAR(32),
    level       INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT = '动物表';

-- ============================================================
-- 15. Animal Product
-- ============================================================
CREATE TABLE animal_product
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    animal_id   BIGINT,
    item_id     VARCHAR(64),
    finish_time DATETIME,
    status      VARCHAR(32)
) COMMENT = '动物生产记录';

-- ============================================================
-- 16. Customer Template
-- ============================================================
CREATE TABLE customer_template
(
    id     VARCHAR(64) PRIMARY KEY,
    name   VARCHAR(32),
    avatar VARCHAR(255),
    type   VARCHAR(32)
) COMMENT = '顾客模板';

-- ============================================================
-- 17. Customer Order
-- ============================================================
CREATE TABLE customer_order
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id   BIGINT,
    customer_id VARCHAR(64),
    item_id     VARCHAR(64),
    reward_gold INT,
    status      VARCHAR(32),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT = '顾客订单';

-- ============================================================
-- 18. Quest Config
-- ============================================================
CREATE TABLE quest_config
(
    id             VARCHAR(64) PRIMARY KEY,
    type           VARCHAR(32),
    title          VARCHAR(128),
    condition_json JSON,
    reward_json    JSON
) COMMENT = '任务配置';

-- ============================================================
-- 19. Player Quest
-- ============================================================
CREATE TABLE player_quest
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id   BIGINT,
    quest_id    VARCHAR(64),
    progress    INT DEFAULT 0,
    status      VARCHAR(32),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_player_id (player_id)
) COMMENT = '玩家任务';

-- ============================================================
-- 20. Shop Config
-- ============================================================
CREATE TABLE shop_config
(
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id   VARCHAR(64),
    price     INT,
    buy_limit INT DEFAULT -1
) COMMENT = '商店配置';

-- ============================================================
-- 21. Decoration
-- ============================================================
CREATE TABLE decoration
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id   BIGINT,
    item_id     VARCHAR(64),
    position_x  INT,
    position_y  INT,
    rotation    INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT = '岛屿装饰';

-- ============================================================
-- 22. Land Config Seed Data
-- ============================================================

-- Farm Block A: 4x4 grid, unlock Lv1
INSERT INTO land_config (area_type, block_id, grid_x, grid_y, unlock_level, buy_price) VALUES
('FARM', 'Farm-A', 0, 0, 1, 0),
('FARM', 'Farm-A', 1, 0, 1, 50),
('FARM', 'Farm-A', 2, 0, 1, 50),
('FARM', 'Farm-A', 3, 0, 1, 50),
('FARM', 'Farm-A', 0, 1, 1, 50),
('FARM', 'Farm-A', 1, 1, 1, 100),
('FARM', 'Farm-A', 2, 1, 1, 100),
('FARM', 'Farm-A', 3, 1, 1, 100),
('FARM', 'Farm-A', 0, 2, 1, 50),
('FARM', 'Farm-A', 1, 2, 1, 100),
('FARM', 'Farm-A', 2, 2, 1, 150),
('FARM', 'Farm-A', 3, 2, 1, 100),
('FARM', 'Farm-A', 0, 3, 1, 50),
('FARM', 'Farm-A', 1, 3, 1, 100),
('FARM', 'Farm-A', 2, 3, 1, 100),
('FARM', 'Farm-A', 3, 3, 1, 150);

-- Farm Block B: 4x4 grid, unlock Lv3
INSERT INTO land_config (area_type, block_id, grid_x, grid_y, unlock_level, buy_price) VALUES
('FARM', 'Farm-B', 0, 0, 3, 100),
('FARM', 'Farm-B', 1, 0, 3, 100),
('FARM', 'Farm-B', 2, 0, 3, 100),
('FARM', 'Farm-B', 3, 0, 3, 100),
('FARM', 'Farm-B', 0, 1, 3, 100),
('FARM', 'Farm-B', 1, 1, 3, 150),
('FARM', 'Farm-B', 2, 1, 3, 150),
('FARM', 'Farm-B', 3, 1, 3, 150),
('FARM', 'Farm-B', 0, 2, 3, 100),
('FARM', 'Farm-B', 1, 2, 3, 150),
('FARM', 'Farm-B', 2, 2, 3, 200),
('FARM', 'Farm-B', 3, 2, 3, 150),
('FARM', 'Farm-B', 0, 3, 3, 100),
('FARM', 'Farm-B', 1, 3, 3, 150),
('FARM', 'Farm-B', 2, 3, 3, 150),
('FARM', 'Farm-B', 3, 3, 3, 200);

-- Farm Block C: 4x4 grid, unlock Lv5
INSERT INTO land_config (area_type, block_id, grid_x, grid_y, unlock_level, buy_price) VALUES
('FARM', 'Farm-C', 0, 0, 5, 200),
('FARM', 'Farm-C', 1, 0, 5, 200),
('FARM', 'Farm-C', 2, 0, 5, 200),
('FARM', 'Farm-C', 3, 0, 5, 200),
('FARM', 'Farm-C', 0, 1, 5, 200),
('FARM', 'Farm-C', 1, 1, 5, 300),
('FARM', 'Farm-C', 2, 1, 5, 300),
('FARM', 'Farm-C', 3, 1, 5, 300),
('FARM', 'Farm-C', 0, 2, 5, 200),
('FARM', 'Farm-C', 1, 2, 5, 300),
('FARM', 'Farm-C', 2, 2, 5, 400),
('FARM', 'Farm-C', 3, 2, 5, 300),
('FARM', 'Farm-C', 0, 3, 5, 200),
('FARM', 'Farm-C', 1, 3, 5, 300),
('FARM', 'Farm-C', 2, 3, 5, 300),
('FARM', 'Farm-C', 3, 3, 5, 400);

-- Farm Block D: 4x4 grid, unlock Lv8
INSERT INTO land_config (area_type, block_id, grid_x, grid_y, unlock_level, buy_price) VALUES
('FARM', 'Farm-D', 0, 0, 8, 400),
('FARM', 'Farm-D', 1, 0, 8, 400),
('FARM', 'Farm-D', 2, 0, 8, 400),
('FARM', 'Farm-D', 3, 0, 8, 400),
('FARM', 'Farm-D', 0, 1, 8, 400),
('FARM', 'Farm-D', 1, 1, 8, 500),
('FARM', 'Farm-D', 2, 1, 8, 500),
('FARM', 'Farm-D', 3, 1, 8, 500),
('FARM', 'Farm-D', 0, 2, 8, 400),
('FARM', 'Farm-D', 1, 2, 8, 500),
('FARM', 'Farm-D', 2, 2, 8, 600),
('FARM', 'Farm-D', 3, 2, 8, 500),
('FARM', 'Farm-D', 0, 3, 8, 400),
('FARM', 'Farm-D', 1, 3, 8, 500),
('FARM', 'Farm-D', 2, 3, 8, 500),
('FARM', 'Farm-D', 3, 3, 8, 600);

-- Flower Block A: 4x4 grid, unlock Lv10
INSERT INTO land_config (area_type, block_id, grid_x, grid_y, unlock_level, buy_price) VALUES
('FLOWER', 'Flower-A', 0, 0, 10, 500),
('FLOWER', 'Flower-A', 1, 0, 10, 500),
('FLOWER', 'Flower-A', 2, 0, 10, 500),
('FLOWER', 'Flower-A', 3, 0, 10, 500),
('FLOWER', 'Flower-A', 0, 1, 10, 500),
('FLOWER', 'Flower-A', 1, 1, 10, 800),
('FLOWER', 'Flower-A', 2, 1, 10, 800),
('FLOWER', 'Flower-A', 3, 1, 10, 800),
('FLOWER', 'Flower-A', 0, 2, 10, 500),
('FLOWER', 'Flower-A', 1, 2, 10, 800),
('FLOWER', 'Flower-A', 2, 2, 10, 1000),
('FLOWER', 'Flower-A', 3, 2, 10, 800),
('FLOWER', 'Flower-A', 0, 3, 10, 500),
('FLOWER', 'Flower-A', 1, 3, 10, 800),
('FLOWER', 'Flower-A', 2, 3, 10, 800),
('FLOWER', 'Flower-A', 3, 3, 10, 1000);

-- Flower Block B: 4x4 grid, unlock Lv13
INSERT INTO land_config (area_type, block_id, grid_x, grid_y, unlock_level, buy_price) VALUES
('FLOWER', 'Flower-B', 0, 0, 13, 800),
('FLOWER', 'Flower-B', 1, 0, 13, 800),
('FLOWER', 'Flower-B', 2, 0, 13, 800),
('FLOWER', 'Flower-B', 3, 0, 13, 800),
('FLOWER', 'Flower-B', 0, 1, 13, 800),
('FLOWER', 'Flower-B', 1, 1, 13, 1200),
('FLOWER', 'Flower-B', 2, 1, 13, 1200),
('FLOWER', 'Flower-B', 3, 1, 13, 1200),
('FLOWER', 'Flower-B', 0, 2, 13, 800),
('FLOWER', 'Flower-B', 1, 2, 13, 1200),
('FLOWER', 'Flower-B', 2, 2, 13, 1500),
('FLOWER', 'Flower-B', 3, 2, 13, 1200),
('FLOWER', 'Flower-B', 0, 3, 13, 800),
('FLOWER', 'Flower-B', 1, 3, 13, 1200),
('FLOWER', 'Flower-B', 2, 3, 13, 1200),
('FLOWER', 'Flower-B', 3, 3, 13, 1500);

-- ============================================================
-- Item Config Seed Data (基础作物)
-- ============================================================
INSERT INTO item_config (id, name, type, icon, sell_price) VALUES
('strawberry', '草莓', 'CROP', 'strawberry', 5),
('cabbage', '小白菜', 'CROP', 'cabbage', 3),
('carrot', '胡萝卜', 'CROP', 'carrot', 8),
('tomato', '番茄', 'CROP', 'tomato', 6),
('potato', '土豆', 'CROP', 'potato', 5),
('chili', '辣椒', 'CROP', 'chili', 20),
('corn', '玉米', 'CROP', 'corn', 40),
('moonberry', '月光莓', 'CROP', 'moonberry', 120);
-- ============================================================
-- Crop Config Seed Data
-- 基础属性与等级数值分离；种植时从两张配置表共同读取。
-- ============================================================
INSERT INTO crop_config
(crop_id, name, rarity, reward_eligible, permanent_unlock_enabled, upgrade_enabled,
 player_unlock_level, max_crop_level, enabled) VALUES
('strawberry', '草莓', 'COMMON', 0, 1, 1, 1, 3, 1),
('cabbage', '白菜', 'COMMON', 0, 1, 1, 2, 3, 1),
('carrot', '胡萝卜', 'COMMON', 0, 1, 1, 3, 3, 1),
('tomato', '番茄', 'COMMON', 0, 1, 1, 4, 3, 1),
('potato', '土豆', 'COMMON', 0, 1, 1, 5, 3, 1),
('chili', '辣椒', 'COMMON', 0, 1, 1, 8, 3, 1),
('corn', '玉米', 'COMMON', 0, 1, 1, 10, 3, 1),
('moonberry', '月光莓', 'RARE', 1, 0, 0, 1, 1, 1);

INSERT INTO crop_level_config
(crop_id, crop_level, grow_seconds, yield_count, upgrade_gold) VALUES
('strawberry', 1, 60, 2, 0), ('strawberry', 2, 50, 3, 200), ('strawberry', 3, 40, 4, 500),
('cabbage', 1, 120, 2, 0), ('cabbage', 2, 105, 3, 300), ('cabbage', 3, 90, 4, 700),
('carrot', 1, 180, 3, 0), ('carrot', 2, 155, 4, 400), ('carrot', 3, 130, 5, 900),
('tomato', 1, 240, 3, 0), ('tomato', 2, 210, 4, 500), ('tomato', 3, 180, 5, 1100),
('potato', 1, 300, 4, 0), ('potato', 2, 270, 5, 600), ('potato', 3, 240, 6, 1300),
('chili', 1, 480, 3, 0), ('chili', 2, 420, 4, 900), ('chili', 3, 360, 5, 1800),
('corn', 1, 600, 5, 0), ('corn', 2, 520, 6, 1200), ('corn', 3, 450, 8, 2500),
('moonberry', 1, 300, 5, 0);

INSERT INTO crop_unlock_source
(crop_id, source_type, currency_type, price, required_player_level, source_ref_id, enabled) VALUES
('strawberry', 'INITIAL', 'NONE', 0, 1, 'NEW_PLAYER', 1),
('cabbage', 'GOLD_SHOP', 'GOLD', 200, 2, 'SHOP_CABBAGE', 1),
('carrot', 'GOLD_SHOP', 'GOLD', 350, 3, 'SHOP_CARROT', 1),
('tomato', 'DIAMOND_SHOP', 'DIAMOND', 10, 4, 'SHOP_TOMATO', 1),
('potato', 'LEVEL_REWARD', 'NONE', 0, 5, 'LEVEL_5', 1),
('chili', 'GOLD_SHOP', 'GOLD', 1000, 8, 'SHOP_CHILI', 1),
('corn', 'LEVEL_REWARD', 'NONE', 0, 10, 'LEVEL_10', 1);

-- 月光莓只能通过奖励获得，发放后 24 小时内可无限次种植但不可升级。
INSERT INTO crop_reward_pool_item
(pool_code, crop_id, grant_crop_level, weight, duration_seconds, enabled) VALUES
('DAILY_RARE_CROP', 'moonberry', 1, 100, 86400, 1);
