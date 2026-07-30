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
    exp        INT                  DEFAULT 0 COMMENT '兼容任务02前的本级经验进度',
    cumulative_exp INT              DEFAULT 0 COMMENT '累计玩家经验；小岛成长权威读模型',
    gold       BIGINT               DEFAULT 1000 COMMENT '金币',
    diamond    INT                  DEFAULT 20 COMMENT '钻石',
    avatar_id  VARCHAR(64) COMMENT '角色形象',
    create_time DATETIME            DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) COMMENT = '游戏角色表';

-- 玩家当前 exp 表示“本级进度”，升级所需经验和奖励由配置表驱动。
CREATE TABLE player_level_config
(
    level           INT PRIMARY KEY COMMENT '当前玩家等级',
    required_exp    INT NOT NULL COMMENT '从当前等级升到下一级所需经验',
    reward_gold     BIGINT NOT NULL DEFAULT 0 COMMENT '升到下一级时奖励金币',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CHECK (level >= 1 AND required_exp > 0 AND reward_gold >= 0)
) COMMENT = '玩家等级成长配置表';

CREATE TABLE island_level_config
(
    level                  INT PRIMARY KEY COMMENT '小岛等级',
    cumulative_exp         INT NOT NULL COMMENT '达到本级所需累计玩家经验',
    crop_id                VARCHAR(64) NOT NULL COMMENT '本级固定赠送作物种植权',
    recipe_id              VARCHAR(64) NOT NULL COMMENT '本级固定赠送配方',
    material_source_hint   VARCHAR(255) COMMENT '材料来源尚未开放时的引导',
    shop_capability_hint   VARCHAR(255) COMMENT '店铺能力尚未开放时的引导',
    enabled                TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time            DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time            DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_island_level_cumulative_exp (cumulative_exp),
    CHECK (level BETWEEN 1 AND 10 AND cumulative_exp >= 0)
) COMMENT = '小岛累计等级与固定奖励配置';

CREATE TABLE player_island_level_reward_claim
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id    BIGINT NOT NULL COMMENT '玩家角色ID',
    island_level INT NOT NULL COMMENT '已领取的小岛等级',
    claimed_at   DATETIME NOT NULL COMMENT '领取时间',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_player_island_level_reward (player_id, island_level),
    INDEX idx_island_level_reward_player (player_id)
) COMMENT = '玩家逐级固定奖励领取记录';

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
    harvest_exp_snapshot INT    COMMENT '本轮收获经验快照',
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
    harvest_exp_snapshot INT COMMENT '种植时收获经验快照',
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
    harvest_exp     INT NOT NULL DEFAULT 0 COMMENT '收获该等级作物一次获得的玩家经验',
    upgrade_gold    BIGINT NOT NULL DEFAULT 0 COMMENT '从上一等级升到本等级所需金币；1级为0',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_crop_level (crop_id, crop_level),
    CHECK (crop_level >= 1 AND grow_seconds > 0 AND yield_count > 0 AND harvest_exp >= 0 AND upgrade_gold >= 0)
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
    unlock_level INT DEFAULT 1,
    sale_gold    INT NOT NULL DEFAULT 0 COMMENT '单份售出金币',
    sale_exp     INT NOT NULL DEFAULT 0 COMMENT '单份售出玩家经验',
    bar_sale_interval_seconds INT NOT NULL DEFAULT 180 COMMENT '吧台单份销售间隔秒数',
    order_weight INT NOT NULL DEFAULT 1 COMMENT '订单配方权重',
    enabled      TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用'
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

CREATE TABLE player_recipe
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id     BIGINT NOT NULL,
    recipe_id     VARCHAR(64) NOT NULL,
    qualification_type VARCHAR(16) NOT NULL DEFAULT 'PERMANENT',
    unlock_source VARCHAR(32) NOT NULL,
    unlock_time   DATETIME NOT NULL,
    valid_from    DATETIME,
    valid_until   DATETIME,
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_player_recipe (player_id, recipe_id, qualification_type),
    INDEX idx_player_recipe_player (player_id)
) COMMENT = '玩家永久或限时配方资格';

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
    recipe_id   VARCHAR(64),
    item_id     VARCHAR(64),
    quantity    INT NOT NULL DEFAULT 1,
    unit_gold_snapshot INT NOT NULL DEFAULT 0,
    unit_exp_snapshot INT NOT NULL DEFAULT 0,
    queue_position INT,
    status      VARCHAR(32),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    close_time  DATETIME,
    close_reason VARCHAR(32),
    INDEX idx_waiting_queue (player_id, status, queue_position)
) COMMENT = '顾客订单';

CREATE TABLE customer_arrival_state
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id       BIGINT NOT NULL,
    next_arrival_at DATETIME,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_arrival_player (player_id)
) COMMENT = '玩家顾客到店状态';

CREATE TABLE order_quantity_weight
(
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    quantity INT NOT NULL,
    weight   INT NOT NULL,
    enabled  TINYINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_order_quantity (quantity),
    CHECK (quantity > 0 AND weight > 0)
) COMMENT = '顾客订单数量权重';

CREATE TABLE drink_bar
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id   BIGINT NOT NULL,
    slot_number TINYINT NOT NULL,
    opened      TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drink_bar_player_slot (player_id, slot_number),
    CHECK (slot_number BETWEEN 1 AND 6)
) COMMENT = '玩家室外吧台';

CREATE TABLE drink_bar_batch
(
    id                             BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id                      BIGINT NOT NULL,
    bar_id                         BIGINT NOT NULL,
    recipe_id                      VARCHAR(64) NOT NULL,
    item_id                        VARCHAR(64) NOT NULL,
    listed_quantity                INT NOT NULL,
    sold_quantity                  INT NOT NULL DEFAULT 0,
    status                         VARCHAR(32) NOT NULL,
    active_marker                  TINYINT NULL COMMENT '活动批次固定为1；关闭后为空',
    unit_gold_snapshot             INT NOT NULL,
    unit_exp_snapshot              INT NOT NULL,
    sale_interval_seconds_snapshot INT NOT NULL,
    listed_at                      DATETIME NOT NULL,
    sold_out_at                    DATETIME,
    closed_at                      DATETIME,
    create_time                    DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time                    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drink_bar_active_batch (bar_id, active_marker),
    INDEX idx_drink_bar_batch_player (player_id, active_marker),
    INDEX idx_drink_bar_batch_history (bar_id, create_time),
    CHECK (listed_quantity BETWEEN 1 AND 10),
    CHECK (sold_quantity BETWEEN 0 AND listed_quantity),
    CHECK (unit_gold_snapshot >= 0),
    CHECK (unit_exp_snapshot >= 0),
    CHECK (sale_interval_seconds_snapshot > 0),
    CHECK (
        (status IN ('SELLING', 'SOLD_OUT') AND active_marker = 1)
        OR (status = 'CLOSED' AND active_marker IS NULL)
    )
) COMMENT = '吧台销售批次及快照历史';

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
('moonberry', '月光莓', 'CROP', 'moonberry', 120),
('orange', '橙子', 'CROP', 'orange', 10),
('blueberry', '蓝莓', 'CROP', 'blueberry', 12),
('apple', '苹果', 'CROP', 'apple', 14),
('watermelon', '西瓜', 'CROP', 'watermelon', 16),
('wheat', '小麦', 'CROP', 'wheat', 10),
('lemon', '柠檬', 'CROP', 'lemon', 14),
('cucumber', '黄瓜', 'CROP', 'cucumber', 12),
('milk', '牛奶', 'MATERIAL', 'milk', 0),
('egg', '鸡蛋', 'MATERIAL', 'egg', 0),
('strawberry_juice', '草莓汁', 'DRINK', 'strawberry_juice', 0);

INSERT INTO recipe_config
(id, name, output_item, make_time, unlock_level, sale_gold, sale_exp,
 bar_sale_interval_seconds, order_weight, enabled) VALUES
('strawberry_juice', '草莓汁', 'strawberry_juice', 0, 1, 30, 5, 180, 100, 1),
('carrot_juice', '胡萝卜汁', 'carrot_juice', 0, 2, 35, 5, 180, 100, 1),
('orange_juice', '橙汁', 'orange_juice', 0, 3, 40, 6, 180, 100, 1),
('tomato_juice', '番茄汁', 'tomato_juice', 0, 4, 40, 6, 180, 100, 1),
('milk_ice_cream', '牛奶冰淇淋', 'milk_ice_cream', 0, 5, 55, 8, 180, 100, 1),
('apple_carrot_juice', '苹果胡萝卜汁', 'apple_carrot_juice', 0, 6, 50, 7, 180, 100, 1),
('watermelon_milk_ice_cream', '西瓜牛奶冰淇淋', 'watermelon_milk_ice_cream', 0, 7, 65, 9, 180, 100, 1),
('strawberry_cake', '草莓蛋糕', 'strawberry_cake', 0, 8, 80, 12, 180, 100, 1),
('lemon_milk_ice_cream', '柠檬牛奶冰淇淋', 'lemon_milk_ice_cream', 0, 9, 70, 10, 180, 100, 1),
('cucumber_apple_juice', '黄瓜苹果汁', 'cucumber_apple_juice', 0, 10, 60, 8, 180, 100, 1);

INSERT INTO recipe_material (recipe_id, item_id, count) VALUES
('strawberry_juice', 'strawberry', 2),
('carrot_juice', 'carrot', 2),
('orange_juice', 'orange', 2),
('tomato_juice', 'tomato', 2),
('milk_ice_cream', 'milk', 2),
('apple_carrot_juice', 'apple', 1),
('apple_carrot_juice', 'carrot', 1),
('watermelon_milk_ice_cream', 'watermelon', 2),
('watermelon_milk_ice_cream', 'milk', 1),
('strawberry_cake', 'strawberry', 2),
('strawberry_cake', 'wheat', 2),
('strawberry_cake', 'egg', 1),
('lemon_milk_ice_cream', 'lemon', 2),
('lemon_milk_ice_cream', 'milk', 1),
('cucumber_apple_juice', 'cucumber', 1),
('cucumber_apple_juice', 'apple', 1);

INSERT INTO order_quantity_weight (quantity, weight, enabled) VALUES
(1, 60, 1), (2, 30, 1), (3, 10, 1);

INSERT INTO customer_template (id, name, avatar, type) VALUES
('berry', '莓莓', '👧', 'ISLANDER'),
('sunny', '小晴', '🧒', 'ISLANDER'),
('captain', '船长', '🧔', 'VISITOR'),
('artist', '画家', '👩‍🎨', 'VISITOR'),
('ranger', '巡林员', '🧑‍🌾', 'ISLANDER');
-- ============================================================
-- Crop Config Seed Data
-- 基础属性与等级数值分离；种植时从两张配置表共同读取。
-- ============================================================
INSERT INTO crop_config
(crop_id, name, rarity, reward_eligible, permanent_unlock_enabled, upgrade_enabled,
 player_unlock_level, max_crop_level, enabled) VALUES
('strawberry', '草莓', 'COMMON', 0, 1, 1, 1, 3, 1),
('cabbage', '白菜', 'COMMON', 0, 1, 1, 2, 3, 1),
('carrot', '胡萝卜', 'COMMON', 0, 1, 1, 2, 3, 1),
('tomato', '番茄', 'COMMON', 0, 1, 1, 4, 3, 1),
('potato', '土豆', 'COMMON', 0, 1, 1, 5, 3, 1),
('chili', '辣椒', 'COMMON', 0, 1, 1, 8, 3, 1),
('corn', '玉米', 'COMMON', 0, 1, 1, 10, 3, 1),
('moonberry', '月光莓', 'RARE', 1, 0, 0, 1, 1, 1),
('orange', '橙子', 'COMMON', 0, 1, 0, 3, 1, 1),
('blueberry', '蓝莓', 'COMMON', 0, 1, 0, 5, 1, 1),
('apple', '苹果', 'COMMON', 0, 1, 0, 6, 1, 1),
('watermelon', '西瓜', 'COMMON', 0, 1, 0, 7, 1, 1),
('wheat', '小麦', 'COMMON', 0, 1, 0, 8, 1, 1),
('lemon', '柠檬', 'COMMON', 0, 1, 0, 9, 1, 1),
('cucumber', '黄瓜', 'COMMON', 0, 1, 0, 10, 1, 1);

INSERT INTO crop_level_config
(crop_id, crop_level, grow_seconds, yield_count, harvest_exp, upgrade_gold) VALUES
('strawberry', 1, 60, 2, 5, 0), ('strawberry', 2, 50, 3, 8, 200), ('strawberry', 3, 40, 4, 12, 500),
('cabbage', 1, 120, 2, 8, 0), ('cabbage', 2, 105, 3, 12, 300), ('cabbage', 3, 90, 4, 18, 700),
('carrot', 1, 180, 3, 12, 0), ('carrot', 2, 155, 4, 18, 400), ('carrot', 3, 130, 5, 25, 900),
('tomato', 1, 240, 3, 15, 0), ('tomato', 2, 210, 4, 22, 500), ('tomato', 3, 180, 5, 30, 1100),
('potato', 1, 300, 4, 18, 0), ('potato', 2, 270, 5, 26, 600), ('potato', 3, 240, 6, 36, 1300),
('chili', 1, 480, 3, 25, 0), ('chili', 2, 420, 4, 36, 900), ('chili', 3, 360, 5, 50, 1800),
('corn', 1, 600, 5, 30, 0), ('corn', 2, 520, 6, 45, 1200), ('corn', 3, 450, 8, 65, 2500),
('moonberry', 1, 300, 5, 40, 0),
('orange', 1, 240, 3, 15, 0),
('blueberry', 1, 300, 4, 18, 0),
('apple', 1, 360, 4, 22, 0),
('watermelon', 1, 480, 3, 25, 0),
('wheat', 1, 300, 5, 20, 0),
('lemon', 1, 420, 4, 25, 0),
('cucumber', 1, 360, 4, 22, 0);

INSERT INTO player_level_config (level, required_exp, reward_gold) VALUES
(1,100,50),(2,150,75),(3,220,100),(4,300,125),(5,400,150),
(6,520,180),(7,660,210),(8,820,250),(9,1000,300),(10,1200,350),
(11,1450,400),(12,1700,450),(13,2000,500),(14,2350,550),(15,2750,600),
(16,3200,700),(17,3700,800),(18,4250,900),(19,4850,1000),(20,5500,1200);

INSERT INTO island_level_config
(level, cumulative_exp, crop_id, recipe_id, material_source_hint, shop_capability_hint, enabled) VALUES
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
(crop_id, source_type, currency_type, price, required_player_level, source_ref_id, enabled) VALUES
('strawberry', 'INITIAL', 'NONE', 0, 1, 'NEW_PLAYER', 1),
('cabbage', 'GOLD_SHOP', 'GOLD', 200, 2, 'SHOP_CABBAGE', 1),
('carrot', 'GOLD_SHOP', 'GOLD', 350, 3, 'SHOP_CARROT', 1),
('tomato', 'DIAMOND_SHOP', 'DIAMOND', 10, 4, 'SHOP_TOMATO', 1),
('potato', 'LEVEL_REWARD', 'NONE', 0, 5, 'LEVEL_5', 1),
('chili', 'GOLD_SHOP', 'GOLD', 1000, 8, 'SHOP_CHILI', 1),
('corn', 'LEVEL_REWARD', 'NONE', 0, 10, 'LEVEL_10', 1);

INSERT INTO crop_unlock_source
(crop_id, source_type, currency_type, price, required_player_level, source_ref_id, enabled) VALUES
('carrot','LEVEL_REWARD','NONE',0,2,'ISLAND_LEVEL_2',1),
('orange','LEVEL_REWARD','NONE',0,3,'ISLAND_LEVEL_3',1),
('tomato','LEVEL_REWARD','NONE',0,4,'ISLAND_LEVEL_4',1),
('blueberry','LEVEL_REWARD','NONE',0,5,'ISLAND_LEVEL_5',1),
('apple','LEVEL_REWARD','NONE',0,6,'ISLAND_LEVEL_6',1),
('watermelon','LEVEL_REWARD','NONE',0,7,'ISLAND_LEVEL_7',1),
('wheat','LEVEL_REWARD','NONE',0,8,'ISLAND_LEVEL_8',1),
('lemon','LEVEL_REWARD','NONE',0,9,'ISLAND_LEVEL_9',1),
('cucumber','LEVEL_REWARD','NONE',0,10,'ISLAND_LEVEL_10',1);

-- 月光莓只能通过奖励获得，发放后 24 小时内可无限次种植但不可升级。
INSERT INTO crop_reward_pool_item
(pool_code, crop_id, grant_crop_level, weight, duration_seconds, enabled) VALUES
('DAILY_RARE_CROP', 'moonberry', 1, 100, 86400, 1);
