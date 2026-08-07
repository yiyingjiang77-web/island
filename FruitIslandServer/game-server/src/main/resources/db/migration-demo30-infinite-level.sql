-- ============================================================
-- Demo3.0 无限等级体系 MySQL 迁移脚本
-- 日期: 2026-08-07
-- 变更:
--   1. 删除限制 level BETWEEN 1 AND 10 的旧 CHECK 约束
--   2. crop_id / recipe_id 改为可空（Lv11+ 为 NULL，NPC/动物系统未建）
--   3. 添加新的 CHECK 约束（只要求 level >= 1）
--   4. 插入 Lv11-20 数据（公式 expToNext(level) = 100 × level^1.3 累加）
-- ============================================================

-- 1. 删除旧的 CHECK 约束（限制 level 1-10）
ALTER TABLE island_level_config DROP CHECK island_level_config_chk_1;

-- 2. crop_id 和 recipe_id 改为可空
ALTER TABLE island_level_config
    MODIFY COLUMN crop_id VARCHAR(64) NULL COMMENT '本级固定赠送作物种植权（Lv11+可能为NULL）',
    MODIFY COLUMN recipe_id VARCHAR(64) NULL COMMENT '本级固定赠送配方（Lv11+可能为NULL）';

-- 3. 添加新的 CHECK 约束（不限制上限，支持无限等级）
ALTER TABLE island_level_config
    ADD CONSTRAINT island_level_config_chk_1 CHECK (level >= 1 AND cumulative_exp >= 0);

-- 4. 插入 Lv11-20 数据
--    cumulative_exp 由公式递推：threshold(L) = threshold(L-1) + floor(100 × L^1.3)
--    Lv10 = 3200 (表配置终点)
--    Lv11 = 3200 + floor(100 × 10^1.3) = 3200 + 1995 = 5195
--    Lv12 = 5195 + floor(100 × 11^1.3) = 5195 + 2258 = 7453
--    ... 以此类推
INSERT INTO island_level_config (level, cumulative_exp, crop_id, recipe_id, material_source_hint, shop_capability_hint, enabled) VALUES
(11, 5195,  NULL, NULL, NULL, NULL, 1),
(12, 7453,  NULL, NULL, NULL, NULL, 1),
(13, 9981,  NULL, NULL, NULL, NULL, 1),
(14, 12787, NULL, NULL, NULL, NULL, 1),
(15, 15877, NULL, NULL, NULL, NULL, 1),
(16, 19257, NULL, NULL, NULL, NULL, 1),
(17, 22932, NULL, NULL, NULL, NULL, 1),
(18, 26909, NULL, NULL, NULL, NULL, 1),
(19, 31193, NULL, NULL, NULL, NULL, 1),
(20, 35788, NULL, NULL, NULL, NULL, 1);
