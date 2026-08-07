-- ============================================================
-- Demo3.0 配方商店系统 MySQL 迁移
-- 执行日期: 2026-08-07
-- 内容:
--   1. 新增 6 个 item_config 条目（薄荷、栗子、4个菌菇饮品）
--   2. 新增 4 个菌菇饮品 recipe_config 条目
--   3. 新增菌菇饮品 recipe_material 条目
--   4. 创建 recipe_shop_config 表 + 8 条种子数据
--   5. 创建 player_recipe_purchase 表
-- ============================================================

-- 1. 新增 item_config 条目
INSERT INTO item_config (id, name, type, icon, sell_price) VALUES
('peppermint', '薄荷', 'MATERIAL', 'peppermint', 5),
('chestnut', '栗子', 'MATERIAL', 'chestnut', 8),
('mushroom_tea', '蘑菇茶', 'DRINK', 'mushroom_tea', 18),
('mushroom_milkshake', '口蘑奶昔', 'DRINK', 'mushroom_milkshake', 24),
('chanterelle_soup', '鸡油菌浓汤', 'DRINK', 'chanterelle_soup', 36),
('truffle_cocoa', '松露热可可', 'DRINK', 'truffle_cocoa', 72)
ON DUPLICATE KEY UPDATE name=VALUES(name), type=VALUES(type), icon=VALUES(icon), sell_price=VALUES(sell_price);

-- 2. 新增菌菇饮品 recipe_config 条目
INSERT INTO recipe_config
(id, name, output_item, make_time, unlock_level, sale_gold, sale_exp,
 bar_sale_interval_seconds, order_weight, enabled, craft_station, obtain_channel) VALUES
('mushroom_tea', '蘑菇茶', 'mushroom_tea', 0, 1, 45, 12, 180, 80, 1, 'drink_bar', 'exchange_shop'),
('mushroom_milkshake', '口蘑奶昔', 'mushroom_milkshake', 0, 1, 60, 15, 180, 80, 1, 'drink_bar', 'exchange_shop'),
('chanterelle_soup', '鸡油菌浓汤', 'chanterelle_soup', 0, 1, 90, 22, 180, 60, 1, 'drink_bar', 'exchange_shop'),
('truffle_cocoa', '松露热可可', 'truffle_cocoa', 0, 1, 180, 40, 180, 40, 1, 'drink_bar', 'exchange_shop')
ON DUPLICATE KEY UPDATE name=VALUES(name), output_item=VALUES(output_item), sale_gold=VALUES(sale_gold),
 sale_exp=VALUES(sale_exp), enabled=VALUES(enabled), craft_station=VALUES(craft_station), obtain_channel=VALUES(obtain_channel);

-- 3. 新增菌菇饮品 recipe_material 条目
INSERT INTO recipe_material (recipe_id, item_id, count) VALUES
('mushroom_tea', 'shiitake', 2), ('mushroom_tea', 'peppermint', 1),
('mushroom_milkshake', 'mushroom', 2), ('mushroom_milkshake', 'milk', 1), ('mushroom_milkshake', 'honey', 1),
('chanterelle_soup', 'chanterelle', 2), ('chanterelle_soup', 'milk', 1), ('chanterelle_soup', 'mushroom', 1),
('truffle_cocoa', 'truffle', 1), ('truffle_cocoa', 'milk', 1), ('truffle_cocoa', 'honey', 1)
ON DUPLICATE KEY UPDATE count=VALUES(count);

-- 4. 创建 recipe_shop_config 表
CREATE TABLE IF NOT EXISTS recipe_shop_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id VARCHAR(64) NOT NULL COMMENT '配方标识',
    recipe_name VARCHAR(64) NOT NULL COMMENT '配方名称',
    shop_type VARCHAR(16) NOT NULL COMMENT '配方类型: drink/cake',
    price INT NOT NULL COMMENT '购买价格(金币)',
    category VARCHAR(32) NOT NULL DEFAULT 'mushroom' COMMENT '分类: mushroom/nut/berry/herb/basic',
    sort_order INT DEFAULT 0 COMMENT '排序',
    enabled INT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_recipe (recipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配方商店配置';

-- 5. 创建 player_recipe_purchase 表
CREATE TABLE IF NOT EXISTS player_recipe_purchase (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    recipe_id VARCHAR(64) NOT NULL COMMENT '购买的配方标识',
    price_paid INT NOT NULL COMMENT '实际支付价格',
    purchased_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_player_recipe (player_id, recipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家配方购买记录';

-- 6. 插入配方商店种子数据
INSERT INTO recipe_shop_config (recipe_id, recipe_name, shop_type, price, category, sort_order, enabled) VALUES
('mushroom_tea', '蘑菇茶', 'drink', 200, 'mushroom', 1, 1),
('mushroom_milkshake', '口蘑奶昔', 'drink', 300, 'mushroom', 2, 1),
('chanterelle_soup', '鸡油菌浓汤', 'drink', 600, 'mushroom', 3, 1),
('truffle_cocoa', '松露热可可', 'drink', 1500, 'mushroom', 4, 1),
('mushroom_pie', '蘑菇咸派', 'cake', 400, 'mushroom', 5, 1),
('shiitake_bun', '香菇芝士包', 'cake', 500, 'mushroom', 6, 1),
('chanterelle_tart', '鸡油菌塔', 'cake', 800, 'mushroom', 7, 1),
('truffle_cake', '松露巧克力蛋糕', 'cake', 2000, 'mushroom', 8, 1)
ON DUPLICATE KEY UPDATE recipe_name=VALUES(recipe_name), shop_type=VALUES(shop_type),
 price=VALUES(price), category=VALUES(category), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

-- 验证
SELECT '=== item_config 新增条目 ===' AS info;
SELECT id, name, type, sell_price FROM item_config WHERE id IN ('peppermint','chestnut','mushroom_tea','mushroom_milkshake','chanterelle_soup','truffle_cocoa');

SELECT '=== recipe_config 菌菇饮品 ===' AS info;
SELECT id, name, sale_gold, sale_exp, craft_station, obtain_channel FROM recipe_config WHERE id IN ('mushroom_tea','mushroom_milkshake','chanterelle_soup','truffle_cocoa');

SELECT '=== recipe_material 菌菇饮品材料 ===' AS info;
SELECT recipe_id, item_id, count FROM recipe_material WHERE recipe_id IN ('mushroom_tea','mushroom_milkshake','chanterelle_soup','truffle_cocoa') ORDER BY recipe_id, item_id;

SELECT '=== recipe_shop_config ===' AS info;
SELECT recipe_id, recipe_name, shop_type, price, category, sort_order FROM recipe_shop_config ORDER BY sort_order;

SELECT '=== player_recipe_purchase 表 ===' AS info;
SELECT COUNT(*) AS total_purchases FROM player_recipe_purchase;
