-- Demo2.8 flower_config 迁移到新 schema
-- 旧列：purchase_currency, purchase_price, max_flower_level
-- 新列：currency_type, seed_price, grow_seconds, yield_count, harvest_exp, max_level

START TRANSACTION;

-- 1. 删除引用旧列名的 CHECK 约束
ALTER TABLE flower_config DROP CHECK flower_config_chk_1;

-- 2. 重命名旧列到新列名
ALTER TABLE flower_config
    CHANGE COLUMN purchase_currency currency_type VARCHAR(16) NOT NULL,
    CHANGE COLUMN purchase_price seed_price BIGINT NOT NULL,
    CHANGE COLUMN max_flower_level max_level INT NOT NULL DEFAULT 10;

-- 3. 添加缺失的新列
ALTER TABLE flower_config
    ADD COLUMN grow_seconds INT NOT NULL DEFAULT 0 AFTER seed_price,
    ADD COLUMN yield_count INT NOT NULL DEFAULT 0 AFTER grow_seconds,
    ADD COLUMN harvest_exp INT NOT NULL DEFAULT 0 AFTER yield_count;

-- 4. 用 H2 schema 的真实值更新老数据
UPDATE flower_config SET grow_seconds = 300, yield_count = 2, harvest_exp = 4 WHERE flower_id = 'rose';
UPDATE flower_config SET grow_seconds = 240, yield_count = 3, harvest_exp = 3 WHERE flower_id = 'chrysanthemum';
UPDATE flower_config SET grow_seconds = 300, yield_count = 3, harvest_exp = 3 WHERE flower_id = 'jasmine';
UPDATE flower_config SET grow_seconds = 360, yield_count = 3, harvest_exp = 4 WHERE flower_id = 'osmanthus';
UPDATE flower_config SET grow_seconds = 420, yield_count = 2, harvest_exp = 5 WHERE flower_id = 'lavender';
UPDATE flower_config SET grow_seconds = 300, yield_count = 3, harvest_exp = 4 WHERE flower_id = 'hibiscus';
UPDATE flower_config SET grow_seconds = 240, yield_count = 3, harvest_exp = 3 WHERE flower_id = 'chamomile';
UPDATE flower_config SET grow_seconds = 480, yield_count = 2, harvest_exp = 5 WHERE flower_id = 'sakura';

-- 5. player_flower_right 列重命名
ALTER TABLE player_flower_right
    CHANGE COLUMN purchase_currency unlock_source VARCHAR(16) NOT NULL,
    CHANGE COLUMN purchase_time unlock_time DATETIME NOT NULL;

COMMIT;