-- ============================================================
-- Crop System V2 一次性迁移脚本（适用于已存在旧 crop_config 的 MySQL）
-- 执行前请备份数据库；本脚本只应执行一次。
-- ============================================================

-- 与 application-common.yml 保持一致，保证所有被引用表都在同一个数据库中。
USE fruit_island;

ALTER TABLE crop_config
    ADD COLUMN name VARCHAR(64) NULL COMMENT '作物显示名称' AFTER crop_id,
    ADD COLUMN rarity VARCHAR(16) NOT NULL DEFAULT 'COMMON' COMMENT 'COMMON/RARE/EPIC/LEGENDARY' AFTER name,
    ADD COLUMN reward_eligible TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许作为随机奖励' AFTER rarity,
    ADD COLUMN permanent_unlock_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许永久获得' AFTER reward_eligible,
    ADD COLUMN upgrade_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许金币升级' AFTER permanent_unlock_enabled,
    ADD COLUMN player_unlock_level INT NOT NULL DEFAULT 1 COMMENT '玩家可开始种植的等级' AFTER upgrade_enabled,
    ADD COLUMN max_crop_level INT NOT NULL DEFAULT 3 COMMENT '作物最高等级' AFTER player_unlock_level,
    ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用' AFTER max_crop_level;

-- 先把旧 seed_level 迁移到新的玩家解锁等级，再删除旧的成长数字段。
UPDATE crop_config
SET name = crop_id,
    player_unlock_level = seed_level,
    max_crop_level = 3,
    rarity = 'COMMON',
    reward_eligible = 0,
    permanent_unlock_enabled = 1,
    upgrade_enabled = 1,
    enabled = 1;

CREATE TABLE crop_level_config
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    crop_id VARCHAR(64) NOT NULL COMMENT '作物编码',
    crop_level INT NOT NULL COMMENT '作物等级',
    grow_seconds INT NOT NULL COMMENT '浇水后成熟秒数',
    yield_count INT NOT NULL COMMENT '单次收获数量',
    upgrade_gold BIGINT NOT NULL DEFAULT 0 COMMENT '升到本等级所需金币；1级为0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_crop_level (crop_id, crop_level)
) COMMENT='作物等级数值配置表';

-- 保留旧数据作为每个作物的 1 级配置。
INSERT INTO crop_level_config
    (crop_id, crop_level, grow_seconds, yield_count, upgrade_gold)
SELECT crop_id, 1, grow_seconds, yield_count, 0
FROM crop_config;

INSERT INTO crop_level_config
    (crop_id, crop_level, grow_seconds, yield_count, upgrade_gold)
SELECT crop_id, 2, GREATEST(1, ROUND(grow_seconds * 0.85)), yield_count + 1, 200
FROM crop_config;

INSERT INTO crop_level_config
    (crop_id, crop_level, grow_seconds, yield_count, upgrade_gold)
SELECT crop_id, 3, GREATEST(1, ROUND(grow_seconds * 0.70)), yield_count + 2, 500
FROM crop_config;

ALTER TABLE crop_config
    DROP COLUMN seed_item_id,
    DROP COLUMN seed_level,
    DROP COLUMN grow_seconds,
    DROP COLUMN yield_count,
    MODIFY COLUMN name VARCHAR(64) NOT NULL;

CREATE TABLE player_crop
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    player_id BIGINT NOT NULL COMMENT '玩家角色ID',
    crop_id VARCHAR(64) NOT NULL COMMENT '永久拥有的作物编码',
    crop_level INT NOT NULL DEFAULT 1 COMMENT '当前作物等级',
    unlock_source VARCHAR(32) NOT NULL COMMENT '永久权限来源',
    unlock_time DATETIME NOT NULL COMMENT '获得时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_player_crop (player_id, crop_id)
) COMMENT='玩家永久作物权限表';

CREATE TABLE crop_unlock_source
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '渠道配置ID',
    crop_id VARCHAR(64) NOT NULL COMMENT '作物编码',
    source_type VARCHAR(32) NOT NULL COMMENT 'INITIAL/GOLD_SHOP/DIAMOND_SHOP/LEVEL_REWARD',
    currency_type VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/GOLD/DIAMOND',
    price BIGINT NOT NULL DEFAULT 0 COMMENT '所需价格',
    required_player_level INT NOT NULL DEFAULT 1 COMMENT '所需玩家等级',
    source_ref_id VARCHAR(128) COMMENT '外部配置编号',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_crop_source (crop_id, source_type, source_ref_id)
) COMMENT='作物永久种植权获得渠道配置';

CREATE TABLE player_crop_grant
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '限时权限ID',
    player_id BIGINT NOT NULL COMMENT '玩家角色ID',
    crop_id VARCHAR(64) NOT NULL COMMENT '稀有作物编码',
    grant_crop_level INT NOT NULL DEFAULT 1 COMMENT '奖励固定等级',
    grant_source VARCHAR(64) NOT NULL COMMENT '奖励来源',
    source_ref_id VARCHAR(128) COMMENT '外部来源编号',
    valid_from DATETIME NOT NULL COMMENT '生效时间',
    valid_until DATETIME NOT NULL COMMENT '失效时间',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/REVOKED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_grant_active (player_id, crop_id, status, valid_until)
) COMMENT='玩家限时稀有作物权限表';

CREATE TABLE crop_reward_pool_item
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '奖励项ID',
    pool_code VARCHAR(64) NOT NULL COMMENT '奖励池编码',
    crop_id VARCHAR(64) NOT NULL COMMENT '稀有作物编码',
    grant_crop_level INT NOT NULL DEFAULT 1 COMMENT '发放等级',
    weight INT NOT NULL DEFAULT 1 COMMENT '随机权重',
    duration_seconds BIGINT NOT NULL COMMENT '有效秒数',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_reward_pool (pool_code, enabled)
) COMMENT='稀有作物随机奖励池明细';

-- 提供一个可直接验证限时奖励流程的示例稀有品种。
INSERT IGNORE INTO item_config (id, name, type, icon, sell_price)
VALUES ('moonberry', '月光莓', 'CROP', 'moonberry', 120);

INSERT INTO crop_config
(crop_id, name, rarity, reward_eligible, permanent_unlock_enabled, upgrade_enabled,
 player_unlock_level, max_crop_level, enabled)
VALUES ('moonberry', '月光莓', 'RARE', 1, 0, 0, 1, 1, 1);

INSERT INTO crop_level_config
(crop_id, crop_level, grow_seconds, yield_count, upgrade_gold)
VALUES ('moonberry', 1, 300, 5, 0);

INSERT INTO crop_reward_pool_item
(pool_code, crop_id, grant_crop_level, weight, duration_seconds, enabled)
VALUES ('DAILY_RARE_CROP', 'moonberry', 1, 100, 86400, 1);

ALTER TABLE player_land
    ADD COLUMN crop_level INT NULL COMMENT '种植时等级快照' AFTER crop_id,
    ADD COLUMN grow_seconds_snapshot INT NULL COMMENT '成熟秒数快照' AFTER crop_level,
    ADD COLUMN yield_count_snapshot INT NULL COMMENT '收获数量快照' AFTER grow_seconds_snapshot,
    ADD COLUMN access_type VARCHAR(16) NULL COMMENT 'PERMANENT/TEMPORARY' AFTER yield_count_snapshot,
    ADD COLUMN access_grant_id BIGINT NULL COMMENT '限时权限ID' AFTER access_type;

ALTER TABLE crop_plant
    ADD COLUMN crop_level INT NULL COMMENT '种植时等级快照' AFTER crop_id,
    ADD COLUMN grow_seconds_snapshot INT NULL COMMENT '成熟秒数快照' AFTER crop_level,
    ADD COLUMN yield_count_snapshot INT NULL COMMENT '收获数量快照' AFTER grow_seconds_snapshot,
    ADD COLUMN access_type VARCHAR(16) NULL COMMENT 'PERMANENT/TEMPORARY' AFTER yield_count_snapshot,
    ADD COLUMN access_grant_id BIGINT NULL COMMENT '限时权限ID' AFTER access_type;

-- 老玩家如果背包中曾有某种种子，则转换为该作物的永久种植权。
INSERT IGNORE INTO player_crop
    (player_id, crop_id, crop_level, unlock_source, unlock_time)
SELECT i.player_id,
       REPLACE(i.item_id, '_seed', ''),
       1,
       'LEGACY_SEED',
       NOW()
FROM inventory i
WHERE i.item_id LIKE '%_seed' AND i.count > 0;

-- 所有旧玩家至少拥有教程作物草莓，避免没有旧种子记录的账号无法继续种植。
INSERT IGNORE INTO player_crop
    (player_id, crop_id, crop_level, unlock_source, unlock_time)
SELECT id, 'strawberry', 1, 'MIGRATION_DEFAULT', NOW()
FROM game_player;

-- 新模型不再使用种子数量；保留其他背包物品，只清理旧种子条目。
DELETE FROM inventory WHERE item_id LIKE '%_seed';

INSERT IGNORE INTO crop_unlock_source
(crop_id, source_type, currency_type, price, required_player_level, source_ref_id, enabled) VALUES
('strawberry', 'INITIAL', 'NONE', 0, 1, 'NEW_PLAYER', 1),
('cabbage', 'GOLD_SHOP', 'GOLD', 200, 2, 'SHOP_CABBAGE', 1),
('carrot', 'GOLD_SHOP', 'GOLD', 350, 3, 'SHOP_CARROT', 1),
('tomato', 'DIAMOND_SHOP', 'DIAMOND', 10, 4, 'SHOP_TOMATO', 1),
('potato', 'LEVEL_REWARD', 'NONE', 0, 5, 'LEVEL_5', 1),
('chili', 'GOLD_SHOP', 'GOLD', 1000, 8, 'SHOP_CHILI', 1),
('corn', 'LEVEL_REWARD', 'NONE', 0, 10, 'LEVEL_10', 1);
