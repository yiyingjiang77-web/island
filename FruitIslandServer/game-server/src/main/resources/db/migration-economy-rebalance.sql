-- ============================================================
-- Demo3.0 Economy Rebalance Migration
-- Applied to MySQL database: fruit_island
-- Date: 2026-08-07
-- ============================================================

START TRANSACTION;

-- 1. item_config: Delete removed crops
DELETE FROM item_config WHERE id IN ('cabbage', 'chili', 'corn', 'potato');

-- 2. item_config: Update existing item sell_prices and names
UPDATE item_config SET sell_price = 2  WHERE id = 'strawberry';
UPDATE item_config SET sell_price = 2  WHERE id = 'carrot';
UPDATE item_config SET sell_price = 2  WHERE id = 'tomato';
UPDATE item_config SET sell_price = 10 WHERE id = 'moonberry';
UPDATE item_config SET sell_price = 3  WHERE id = 'orange';
UPDATE item_config SET sell_price = 3  WHERE id = 'blueberry';
UPDATE item_config SET sell_price = 4  WHERE id = 'apple';
UPDATE item_config SET sell_price = 5  WHERE id = 'watermelon';
UPDATE item_config SET sell_price = 3  WHERE id = 'wheat';
UPDATE item_config SET sell_price = 5  WHERE id = 'lemon';
UPDATE item_config SET sell_price = 4  WHERE id = 'cucumber';
UPDATE item_config SET sell_price = 2  WHERE id = 'milk';
UPDATE item_config SET sell_price = 2  WHERE id = 'egg';
UPDATE item_config SET sell_price = 10 WHERE id = 'strawberry_juice';
UPDATE item_config SET sell_price = 8  WHERE id = 'rose';
UPDATE item_config SET sell_price = 5  WHERE id = 'chrysanthemum';
UPDATE item_config SET sell_price = 6, name = '茉莉花' WHERE id = 'jasmine';
UPDATE item_config SET sell_price = 10 WHERE id = 'osmanthus';
UPDATE item_config SET sell_price = 12 WHERE id = 'lavender';
UPDATE item_config SET sell_price = 8, name = '洛神花'  WHERE id = 'hibiscus';
UPDATE item_config SET sell_price = 5  WHERE id = 'chamomile';
UPDATE item_config SET sell_price = 15 WHERE id = 'sakura';

-- 3. item_config: Insert missing items (drink products, cake products, materials)
INSERT IGNORE INTO item_config (id, name, type, icon, sell_price) VALUES
('carrot_juice','胡萝卜汁','DRINK','carrot_juice',12),
('orange_juice','橙汁','DRINK','orange_juice',14),
('tomato_juice','番茄汁','DRINK','tomato_juice',14),
('blueberry_juice','蓝莓汁','DRINK','blueberry_juice',12),
('milk_ice_cream','牛奶冰淇淋','DRINK','milk_ice_cream',12),
('apple_carrot_juice','苹果胡萝卜汁','DRINK','apple_carrot_juice',16),
('watermelon_milk_ice_cream','西瓜牛奶冰淇淋','DRINK','watermelon_milk_ice_cream',22),
('lemon_milk_ice_cream','柠檬牛奶冰淇淋','DRINK','lemon_milk_ice_cream',22),
('cucumber_apple_juice','黄瓜苹果汁','DRINK','cucumber_apple_juice',18),
('honey','蜂蜜','MATERIAL','honey',8),
('strawberry_cake','草莓蛋糕','CAKE','strawberry_cake',60),
('carrot_cake','胡萝卜蛋糕','CAKE','carrot_cake',48),
('apple_cake','苹果蛋糕','CAKE','apple_cake',56),
('blueberry_cake','蓝莓蛋糕','CAKE','blueberry_cake',64),
('lemon_cake','柠檬蛋糕','CAKE','lemon_cake',72),
('rose_cake','玫瑰蛋糕','CAKE','rose_cake',60),
('chrysanthemum_cake','菊花酥','CAKE','chrysanthemum_cake',40),
('jasmine_mousse','茉莉慕斯','CAKE','jasmine_mousse',64),
('osmanthus_cake','桂花糕','CAKE','osmanthus_cake',72),
('lavender_macaron','薰衣草马卡龙','CAKE','lavender_macaron',80),
('hibiscus_jelly','洛神花果冻','CAKE','hibiscus_jelly',52),
('sakura_cake','樱花蛋糕','CAKE','sakura_cake',100),
('chamomile_cookie','洋甘菊饼干','CAKE','chamomile_cookie',44),
('mushroom_pie','蘑菇咸派','CAKE','mushroom_pie',48),
('shiitake_bun','香菇芝士包','CAKE','shiitake_bun',56),
('chanterelle_tart','鸡油菌塔','CAKE','chanterelle_tart',64),
('truffle_cake','松露巧克力蛋糕','CAKE','truffle_cake',128),
('mushroom','口蘑','MATERIAL','mushroom',5),
('shiitake','香菇','MATERIAL','shiitake',8),
('chanterelle','鸡油菌','MATERIAL','chanterelle',15),
('truffle','松露','MATERIAL','truffle',40);

-- 4. recipe_config: Update drink recipe economy values
UPDATE recipe_config SET sale_gold = 25, sale_exp = 10 WHERE id = 'strawberry_juice';
UPDATE recipe_config SET sale_gold = 30, sale_exp = 12 WHERE id = 'carrot_juice';
UPDATE recipe_config SET sale_gold = 35, sale_exp = 15 WHERE id = 'orange_juice';
UPDATE recipe_config SET sale_gold = 35, sale_exp = 15 WHERE id = 'tomato_juice';
UPDATE recipe_config SET sale_gold = 30, sale_exp = 18 WHERE id = 'milk_ice_cream';
UPDATE recipe_config SET sale_gold = 40, sale_exp = 20 WHERE id = 'apple_carrot_juice';
UPDATE recipe_config SET sale_gold = 55, sale_exp = 25 WHERE id = 'watermelon_milk_ice_cream';
UPDATE recipe_config SET sale_gold = 55, sale_exp = 30 WHERE id = 'lemon_milk_ice_cream';
UPDATE recipe_config SET sale_gold = 45, sale_exp = 25 WHERE id = 'cucumber_apple_juice';

-- 5. recipe_config: Insert blueberry_juice recipe
INSERT IGNORE INTO recipe_config
(id, name, output_item, make_time, unlock_level, sale_gold, sale_exp,
 bar_sale_interval_seconds, order_weight, enabled, craft_station, obtain_channel)
VALUES
('blueberry_juice','蓝莓汁','blueberry_juice',0,5,30,12,180,100,1,'drink_bar','island_level');

-- 6. recipe_material: Clear and rebuild all entries to match H2 schema exactly
DELETE FROM recipe_material;

INSERT INTO recipe_material (recipe_id, item_id, count) VALUES
-- Drink recipes
('strawberry_juice','strawberry',2),
('carrot_juice','carrot',2),
('orange_juice','orange',2),
('tomato_juice','tomato',2),
('blueberry_juice','blueberry',2),
('milk_ice_cream','milk',2),
('apple_carrot_juice','apple',1),
('apple_carrot_juice','carrot',1),
('watermelon_milk_ice_cream','watermelon',2),
('watermelon_milk_ice_cream','milk',1),
('strawberry_cake','strawberry',3),
('strawberry_cake','wheat',3),
('strawberry_cake','egg',2),
('strawberry_cake','milk',1),
('lemon_milk_ice_cream','lemon',2),
('lemon_milk_ice_cream','milk',1),
('cucumber_apple_juice','cucumber',1),
('cucumber_apple_juice','apple',1),
-- Cake recipes (Demo2.10)
('carrot_cake','carrot',3),('carrot_cake','wheat',3),('carrot_cake','egg',2),('carrot_cake','milk',1),
('apple_cake','apple',3),('apple_cake','wheat',3),('apple_cake','egg',1),('apple_cake','milk',1),
('blueberry_cake','blueberry',3),('blueberry_cake','wheat',3),('blueberry_cake','egg',1),('blueberry_cake','milk',1),
('lemon_cake','lemon',2),('lemon_cake','wheat',3),('lemon_cake','apple',1),('lemon_cake','milk',1),
('rose_cake','rose',2),('rose_cake','wheat',2),('rose_cake','egg',1),
('chrysanthemum_cake','chrysanthemum',2),('chrysanthemum_cake','wheat',2),('chrysanthemum_cake','milk',1),
('jasmine_mousse','jasmine',2),('jasmine_mousse','wheat',1),('jasmine_mousse','egg',2),('jasmine_mousse','milk',1),
('osmanthus_cake','osmanthus',2),('osmanthus_cake','wheat',2),('osmanthus_cake','honey',1),
('lavender_macaron','lavender',1),('lavender_macaron','wheat',1),('lavender_macaron','egg',2),('lavender_macaron','milk',1),
('hibiscus_jelly','hibiscus',2),('hibiscus_jelly','honey',1),('hibiscus_jelly','milk',1),
('sakura_cake','sakura',2),('sakura_cake','wheat',2),('sakura_cake','egg',1),('sakura_cake','milk',1),
('chamomile_cookie','chamomile',2),('chamomile_cookie','wheat',2),('chamomile_cookie','egg',1),
('mushroom_pie','mushroom',3),('mushroom_pie','wheat',2),('mushroom_pie','egg',1),('mushroom_pie','milk',1),
('shiitake_bun','shiitake',3),('shiitake_bun','wheat',2),('shiitake_bun','milk',1),
('chanterelle_tart','chanterelle',2),('chanterelle_tart','wheat',1),('chanterelle_tart','egg',1),('chanterelle_tart','milk',1),
('truffle_cake','truffle',1),('truffle_cake','wheat',2),('truffle_cake','egg',1),('truffle_cake','milk',1);

-- 7. crop_level_config: Delete removed crops
DELETE FROM crop_level_config WHERE crop_id IN ('cabbage', 'chili', 'corn', 'potato');

-- 8. crop_level_config: Delete remaining crops and re-insert with new harvest_exp
DELETE FROM crop_level_config WHERE crop_id IN (
    'strawberry','carrot','orange','tomato','blueberry','apple','watermelon','wheat','lemon','cucumber','moonberry'
);

INSERT INTO crop_level_config
(crop_id, crop_level, grow_seconds, yield_count, harvest_exp, upgrade_gold) VALUES
('strawberry',1,60,2,1,0),('strawberry',2,57,3,1,200),('strawberry',3,54,3,1,400),('strawberry',4,51,4,2,800),('strawberry',5,48,4,2,1400),
('strawberry',6,45,5,2,2200),('strawberry',7,42,5,2,3200),('strawberry',8,39,6,3,4400),('strawberry',9,36,6,3,5800),('strawberry',10,30,7,3,7400),
('carrot',1,180,3,2,0),('carrot',2,171,4,2,400),('carrot',3,162,4,2,800),('carrot',4,153,5,3,1600),('carrot',5,144,5,3,2800),
('carrot',6,135,6,3,4400),('carrot',7,126,6,3,6400),('carrot',8,117,7,4,8800),('carrot',9,108,7,4,11600),('carrot',10,90,8,4,14800),
('orange',1,240,3,3,0),('orange',2,228,4,3,500),('orange',3,216,4,3,1000),('orange',4,204,5,3,2000),('orange',5,192,5,4,3500),
('orange',6,180,6,4,5500),('orange',7,168,6,4,8000),('orange',8,156,7,4,11000),('orange',9,144,7,4,14500),('orange',10,120,8,4,18500),
('tomato',1,240,3,3,0),('tomato',2,228,4,3,500),('tomato',3,216,4,3,1000),('tomato',4,204,5,4,2000),('tomato',5,192,5,4,3500),
('tomato',6,180,6,4,5500),('tomato',7,168,6,4,8000),('tomato',8,156,7,5,11000),('tomato',9,144,7,5,14500),('tomato',10,120,8,6,18500),
('blueberry',1,300,4,3,0),('blueberry',2,285,5,3,600),('blueberry',3,270,5,3,1200),('blueberry',4,255,6,3,2400),('blueberry',5,240,6,4,4200),
('blueberry',6,225,7,4,6600),('blueberry',7,210,7,4,9600),('blueberry',8,195,8,4,13200),('blueberry',9,180,8,4,17400),('blueberry',10,150,8,4,22200),
('apple',1,360,4,4,0),('apple',2,342,5,4,700),('apple',3,324,5,4,1400),('apple',4,306,6,4,2800),('apple',5,288,6,4,4900),
('apple',6,270,7,5,7700),('apple',7,252,7,5,11200),('apple',8,234,8,5,15400),('apple',9,216,8,5,20300),('apple',10,180,8,5,25900),
('watermelon',1,480,3,5,0),('watermelon',2,456,4,5,900),('watermelon',3,432,4,5,1800),('watermelon',4,408,5,5,3600),('watermelon',5,384,5,5,6300),
('watermelon',6,360,6,5,9900),('watermelon',7,336,6,6,14400),('watermelon',8,312,7,6,19800),('watermelon',9,288,7,6,26100),('watermelon',10,240,8,6,33300),
('wheat',1,300,5,4,0),('wheat',2,285,6,4,800),('wheat',3,270,6,4,1600),('wheat',4,255,7,4,3200),('wheat',5,240,7,4,5600),
('wheat',6,225,8,5,8800),('wheat',7,210,8,5,12800),('wheat',8,195,8,5,17600),('wheat',9,180,8,5,23200),('wheat',10,150,8,5,29600),
('lemon',1,420,4,5,0),('lemon',2,399,5,5,900),('lemon',3,378,5,5,1800),('lemon',4,357,6,5,3600),('lemon',5,336,6,5,6300),
('lemon',6,315,7,5,9900),('lemon',7,294,7,6,14400),('lemon',8,273,8,6,19800),('lemon',9,252,8,6,26100),('lemon',10,210,8,6,33300),
('cucumber',1,360,4,5,0),('cucumber',2,342,5,5,800),('cucumber',3,324,5,5,1600),('cucumber',4,306,6,5,3200),('cucumber',5,288,6,5,5600),
('cucumber',6,270,7,5,8800),('cucumber',7,252,7,6,12800),('cucumber',8,234,8,6,17600),('cucumber',9,216,8,6,23200),('cucumber',10,180,8,6,29600),
('moonberry',1,600,2,5,0);

-- 9. island_level_config: Update Lv5 recipe from milk_ice_cream to blueberry_juice
UPDATE island_level_config
SET recipe_id = 'blueberry_juice',
    material_source_hint = NULL,
    shop_capability_hint = NULL
WHERE level = 5;

COMMIT;

-- ============================================================
-- Migration complete - verify with queries below
-- ============================================================
