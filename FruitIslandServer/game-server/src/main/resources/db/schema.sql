-- ============================================================
-- Fruit Island Game Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS fruit_island_db
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE fruit_island_db;

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
-- 4. Land
-- ============================================================
CREATE TABLE land
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '土地ID',
    area_id      BIGINT NOT NULL COMMENT '区域ID',
    position_x   INT COMMENT '地图X',
    position_y   INT COMMENT '地图Y',
    state        VARCHAR(32) DEFAULT 'LOCKED' COMMENT '土地状态',
    unlock_level INT         DEFAULT 1,
    create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_area_id (area_id)
) COMMENT = '土地表';

-- ============================================================
-- 5. Crop Plant
-- ============================================================
CREATE TABLE crop_plant
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    land_id     BIGINT NOT NULL COMMENT '土地ID',
    crop_id     VARCHAR(64) COMMENT '作物ID',
    plant_time  DATETIME COMMENT '种植时间',
    finish_time DATETIME COMMENT '成熟时间',
    status      VARCHAR(32) COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_land_id (land_id)
) COMMENT = '作物种植记录';

-- ============================================================
-- 6. Item Config
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
-- 7. Inventory
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
-- 8. Building
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
-- 9. Building Upgrade
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
-- 10. Recipe Config
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
-- 11. Recipe Material
-- ============================================================
CREATE TABLE recipe_material
(
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_id VARCHAR(64),
    item_id   VARCHAR(64),
    count     INT DEFAULT 1
) COMMENT = '配方材料';

-- ============================================================
-- 12. Production Order
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
-- 13. Animal
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
-- 14. Animal Product
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
-- 15. Customer Template
-- ============================================================
CREATE TABLE customer_template
(
    id     VARCHAR(64) PRIMARY KEY,
    name   VARCHAR(32),
    avatar VARCHAR(255),
    type   VARCHAR(32)
) COMMENT = '顾客模板';

-- ============================================================
-- 16. Customer Order
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
-- 17. Quest Config
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
-- 18. Player Quest
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
-- 19. Shop Config
-- ============================================================
CREATE TABLE shop_config
(
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id   VARCHAR(64),
    price     INT,
    buy_limit INT DEFAULT -1
) COMMENT = '商店配置';

-- ============================================================
-- 20. Decoration
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
