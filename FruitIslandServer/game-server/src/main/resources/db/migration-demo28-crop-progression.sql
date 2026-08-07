-- ============================================================
-- Demo2.8: 作物体系重建 + 花卉系统 + 蜂蜜系统
-- 基于 Demo3.0 经济重平衡设计，一次性迁移脚本。
-- 执行前请备份数据库；本脚本只应执行一次。
-- ============================================================

USE fruit_island;

-- ============================================================
-- Part 1: 删除废弃作物（白菜/土豆/辣椒/玉米）
-- ============================================================

-- 标记已有玩家种植权为已废弃（不删除记录以保证数据完整性）
UPDATE player_crop SET unlock_source = 'DEPRECATED'
WHERE crop_id IN ('cabbage', 'potato', 'chili', 'corn');

-- 删除配置表中的废弃作物
DELETE FROM crop_level_config WHERE crop_id IN ('cabbage', 'potato', 'chili', 'corn');
DELETE FROM crop_unlock_source WHERE crop_id IN ('cabbage', 'potato', 'chili', 'corn');
DELETE FROM crop_config WHERE crop_id IN ('cabbage', 'potato', 'chili', 'corn');

-- ============================================================
-- Part 2: 更新剩余 10 种基础作物配置
-- ============================================================

-- 所有基础作物 max_crop_level=10, upgrade_enabled=1
UPDATE crop_config
SET max_crop_level = 10,
    upgrade_enabled = 1,
    permanent_unlock_enabled = 1
WHERE crop_id IN ('strawberry', 'carrot', 'orange', 'tomato', 'blueberry',
                  'apple', 'watermelon', 'wheat', 'lemon', 'cucumber');

-- 更新月光莓一级数值（Demo3.0 经济重平衡）
UPDATE crop_level_config
SET grow_seconds = 600,
    yield_count = 2,
    harvest_exp = 25
WHERE crop_id = 'moonberry' AND crop_level = 1;

-- ============================================================
-- Part 3: 替换 crop_level_config 为完整 10 级配置（100 条）
-- ============================================================

-- 先删除旧的等级配置（1-3 级的旧数据）
DELETE FROM crop_level_config
WHERE crop_id IN ('strawberry', 'carrot', 'orange', 'tomato', 'blueberry',
                  'apple', 'watermelon', 'wheat', 'lemon', 'cucumber');

INSERT INTO crop_level_config
(crop_id, crop_level, grow_seconds, yield_count, harvest_exp, upgrade_gold) VALUES
('strawberry',1,60,2,4,0),
('strawberry',2,57,3,5,200),
('strawberry',3,54,3,5,400),
('strawberry',4,51,4,6,800),
('strawberry',5,48,4,6,1400),
('strawberry',6,45,5,7,2200),
('strawberry',7,42,5,8,3200),
('strawberry',8,39,6,9,4400),
('strawberry',9,36,6,10,5800),
('strawberry',10,30,7,11,7400),
('carrot',1,180,3,6,0),
('carrot',2,171,4,7,400),
('carrot',3,162,4,8,800),
('carrot',4,153,5,9,1600),
('carrot',5,144,5,10,2800),
('carrot',6,135,6,11,4400),
('carrot',7,126,6,12,6400),
('carrot',8,117,7,14,8800),
('carrot',9,108,7,15,11600),
('carrot',10,90,8,17,14800),
('orange',1,240,3,8,0),
('orange',2,228,4,9,500),
('orange',3,216,4,10,1000),
('orange',4,204,5,12,2000),
('orange',5,192,5,13,3500),
('orange',6,180,6,14,5500),
('orange',7,168,6,16,8000),
('orange',8,156,7,18,11000),
('orange',9,144,7,20,14500),
('orange',10,120,8,22,18500),
('tomato',1,240,3,8,0),
('tomato',2,228,4,9,500),
('tomato',3,216,4,10,1000),
('tomato',4,204,5,12,2000),
('tomato',5,192,5,13,3500),
('tomato',6,180,6,14,5500),
('tomato',7,168,6,16,8000),
('tomato',8,156,7,18,11000),
('tomato',9,144,7,20,14500),
('tomato',10,120,8,22,18500),
('blueberry',1,300,4,10,0),
('blueberry',2,285,5,12,600),
('blueberry',3,270,5,13,1200),
('blueberry',4,255,6,14,2400),
('blueberry',5,240,6,16,4200),
('blueberry',6,225,7,18,6600),
('blueberry',7,210,7,20,9600),
('blueberry',8,195,8,22,13200),
('blueberry',9,180,8,25,17400),
('blueberry',10,150,8,28,22200),
('apple',1,360,4,12,0),
('apple',2,342,5,14,700),
('apple',3,324,5,16,1400),
('apple',4,306,6,17,2800),
('apple',5,288,6,19,4900),
('apple',6,270,7,22,7700),
('apple',7,252,7,24,11200),
('apple',8,234,8,27,15400),
('apple',9,216,8,30,20300),
('apple',10,180,8,34,25900),
('watermelon',1,480,3,14,0),
('watermelon',2,456,4,16,900),
('watermelon',3,432,4,18,1800),
('watermelon',4,408,5,20,3600),
('watermelon',5,384,5,22,6300),
('watermelon',6,360,6,25,9900),
('watermelon',7,336,6,28,14400),
('watermelon',8,312,7,32,19800),
('watermelon',9,288,7,35,26100),
('watermelon',10,240,8,39,33300),
('wheat',1,300,5,10,0),
('wheat',2,285,6,12,800),
('wheat',3,270,6,13,1600),
('wheat',4,255,7,14,3200),
('wheat',5,240,7,16,5600),
('wheat',6,225,8,18,8800),
('wheat',7,210,8,20,12800),
('wheat',8,195,8,22,17600),
('wheat',9,180,8,25,23200),
('wheat',10,150,8,28,29600),
('lemon',1,420,4,12,0),
('lemon',2,399,5,14,900),
('lemon',3,378,5,16,1800),
('lemon',4,357,6,17,3600),
('lemon',5,336,6,19,6300),
('lemon',6,315,7,22,9900),
('lemon',7,294,7,24,14400),
('lemon',8,273,8,27,19800),
('lemon',9,252,8,30,26100),
('lemon',10,210,8,34,33300),
('cucumber',1,360,4,12,0),
('cucumber',2,342,5,14,800),
('cucumber',3,324,5,16,1600),
('cucumber',4,306,6,17,3200),
('cucumber',5,288,6,19,5600),
('cucumber',6,270,7,22,8800),
('cucumber',7,252,7,24,12800),
('cucumber',8,234,8,27,17600),
('cucumber',9,216,8,30,23200),
('cucumber',10,180,8,34,29600);

-- ============================================================
-- Part 4: 清理作物解锁渠道
-- 基础作物单一渠道：strawberry=INITIAL, 其余=LEVEL_REWARD
-- 删除旧的金币/钻石商店来源
-- ============================================================

DELETE FROM crop_unlock_source
WHERE crop_id IN ('carrot', 'tomato')
  AND source_type IN ('GOLD_SHOP', 'DIAMOND_SHOP');

-- 确保 strawberry 只有 INITIAL 来源
DELETE FROM crop_unlock_source
WHERE crop_id = 'strawberry' AND source_type != 'INITIAL';

-- 确保岛屿等级奖励来源存在且正确
INSERT INTO crop_unlock_source
(crop_id, source_type, currency_type, price, required_player_level, source_ref_id, enabled) VALUES
('strawberry', 'INITIAL', 'NONE', 0, 1, 'NEW_PLAYER', 1),
('carrot', 'LEVEL_REWARD', 'NONE', 0, 2, 'ISLAND_LEVEL_2', 1),
('orange', 'LEVEL_REWARD', 'NONE', 0, 3, 'ISLAND_LEVEL_3', 1),
('tomato', 'LEVEL_REWARD', 'NONE', 0, 4, 'ISLAND_LEVEL_4', 1),
('blueberry', 'LEVEL_REWARD', 'NONE', 0, 5, 'ISLAND_LEVEL_5', 1),
('apple', 'LEVEL_REWARD', 'NONE', 0, 6, 'ISLAND_LEVEL_6', 1),
('watermelon', 'LEVEL_REWARD', 'NONE', 0, 7, 'ISLAND_LEVEL_7', 1),
('wheat', 'LEVEL_REWARD', 'NONE', 0, 8, 'ISLAND_LEVEL_8', 1),
('lemon', 'LEVEL_REWARD', 'NONE', 0, 9, 'ISLAND_LEVEL_9', 1),
('cucumber', 'LEVEL_REWARD', 'NONE', 0, 10, 'ISLAND_LEVEL_10', 1)
ON DUPLICATE KEY UPDATE
currency_type=VALUES(currency_type),
price=VALUES(price),
required_player_level=VALUES(required_player_level),
enabled=VALUES(enabled);

-- ============================================================
-- Part 5: 新增花卉系统表
-- ============================================================

CREATE TABLE IF NOT EXISTS flower_config
(
    flower_id       VARCHAR(64) PRIMARY KEY COMMENT '花卉唯一编码',
    name            VARCHAR(64) NOT NULL COMMENT '花卉显示名称',
    currency_type   VARCHAR(16) NOT NULL DEFAULT 'GOLD' COMMENT 'GOLD/DIAMOND',
    seed_price      BIGINT NOT NULL DEFAULT 0 COMMENT '种子（永久种植权）价格',
    grow_seconds    INT NOT NULL COMMENT '一级成熟秒数',
    yield_count     INT NOT NULL COMMENT '一级产量',
    harvest_exp     INT NOT NULL DEFAULT 0 COMMENT '一级收获经验',
    honey_coefficient INT NOT NULL DEFAULT 1 COMMENT '蜂蜜系数：金币花=1 钻石花=2',
    max_level       INT NOT NULL DEFAULT 10 COMMENT '花卉最高等级',
    enabled         TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK (grow_seconds > 0 AND yield_count > 0 AND harvest_exp >= 0 AND seed_price >= 0)
) COMMENT = '花卉基础配置表';

CREATE TABLE IF NOT EXISTS flower_level_config
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    flower_id       VARCHAR(64) NOT NULL COMMENT '花卉编码',
    flower_level    INT NOT NULL COMMENT '花卉等级，从1开始',
    grow_seconds    INT NOT NULL COMMENT '浇水后成熟秒数',
    yield_count     INT NOT NULL COMMENT '单次收获数量',
    harvest_exp     INT NOT NULL DEFAULT 0 COMMENT '收获经验',
    upgrade_gold    BIGINT NOT NULL DEFAULT 0 COMMENT '从上一等级升到本等级所需金币；1级为0',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_flower_level (flower_id, flower_level),
    CHECK (flower_level >= 1 AND grow_seconds > 0 AND yield_count > 0 AND harvest_exp >= 0 AND upgrade_gold >= 0)
) COMMENT = '花卉等级数值配置表';

CREATE TABLE IF NOT EXISTS player_flower_right
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    player_id     BIGINT NOT NULL COMMENT '玩家角色ID',
    flower_id     VARCHAR(64) NOT NULL COMMENT '永久拥有的花卉编码',
    flower_level  INT NOT NULL DEFAULT 1 COMMENT '当前花卉等级',
    unlock_source VARCHAR(32) NOT NULL COMMENT 'GOLD_SHOP/DIAMOND_SHOP',
    unlock_time   DATETIME NOT NULL COMMENT '获得时间',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_player_flower (player_id, flower_id),
    INDEX idx_player_flower_player (player_id)
) COMMENT = '玩家永久花卉种植权表';

-- ============================================================
-- Part 6: 新增蜂箱表
-- ============================================================

CREATE TABLE IF NOT EXISTS player_beehive
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    player_id         BIGINT NOT NULL COMMENT '玩家角色ID',
    beehive_count     INT NOT NULL DEFAULT 0 COMMENT '蜂箱数量（0-3）',
    honey_stored      INT NOT NULL DEFAULT 0 COMMENT '当前存储蜂蜜量',
    last_produce_time DATETIME COMMENT '上次产蜜结算时间',
    last_collect_time DATETIME COMMENT '上次收取时间',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_player_beehive (player_id)
) COMMENT = '玩家蜂箱状态表';

-- ============================================================
-- Part 7: 新增物品配置（花卉 + 蜂蜜）
-- ============================================================

INSERT INTO item_config (id, name, type, icon, sell_price) VALUES
('rose','玫瑰','FLOWER','rose',5),
('chrysanthemum','菊花','FLOWER','chrysanthemum',3),
('jasmine','茉莉花','FLOWER','jasmine',4),
('osmanthus','桂花','FLOWER','osmanthus',5),
('lavender','薰衣草','FLOWER','lavender',6),
('hibiscus','洛神花','FLOWER','hibiscus',4),
('chamomile','洋甘菊','FLOWER','chamomile',3),
('sakura','樱花','FLOWER','sakura',10),
('honey','蜂蜜','MATERIAL','honey',15)
ON DUPLICATE KEY UPDATE
name=VALUES(name), type=VALUES(type), icon=VALUES(icon), sell_price=VALUES(sell_price);

-- ============================================================
-- Part 8: 花卉基础配置种子数据（8 种）
-- ============================================================

INSERT INTO flower_config
(flower_id, name, currency_type, seed_price, grow_seconds, yield_count, harvest_exp, honey_coefficient, max_level, enabled) VALUES
('rose','玫瑰','GOLD',500,300,2,4,1,10,1),
('chrysanthemum','菊花','GOLD',300,240,3,3,1,10,1),
('jasmine','茉莉花','GOLD',400,300,3,3,1,10,1),
('osmanthus','桂花','GOLD',600,360,3,4,1,10,1),
('lavender','薰衣草','GOLD',800,420,2,5,1,10,1),
('hibiscus','洛神花','GOLD',500,300,3,4,1,10,1),
('chamomile','洋甘菊','GOLD',300,240,3,3,1,10,1),
('sakura','樱花','DIAMOND',10,480,2,5,2,10,1)
ON DUPLICATE KEY UPDATE
name=VALUES(name),
currency_type=VALUES(currency_type),
seed_price=VALUES(seed_price),
grow_seconds=VALUES(grow_seconds),
yield_count=VALUES(yield_count),
harvest_exp=VALUES(harvest_exp),
honey_coefficient=VALUES(honey_coefficient),
max_level=VALUES(max_level),
enabled=VALUES(enabled);

-- ============================================================
-- Part 9: 花卉等级配置种子数据（8 种 x 10 级 = 80 条）
-- ============================================================

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
('sakura',6,360,5,9,13200),('sakura',7,336,5,10,19200),('sakura',8,312,6,11,26400),('sakura',9,288,6,13,34800),('sakura',10,240,7,14,44400)
ON DUPLICATE KEY UPDATE
grow_seconds=VALUES(grow_seconds),
yield_count=VALUES(yield_count),
harvest_exp=VALUES(harvest_exp),
upgrade_gold=VALUES(upgrade_gold);
