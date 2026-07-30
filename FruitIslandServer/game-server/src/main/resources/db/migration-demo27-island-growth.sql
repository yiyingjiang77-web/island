-- Demo2.7 Task 01: cumulative island growth configuration and fixed rewards.
-- Run once against an existing fruit_island database before deploying the matching server.
USE fruit_island;

-- Keep the legacy per-level exp column working until Task 02 switches every reward path.
ALTER TABLE game_player
    ADD COLUMN cumulative_exp INT NULL COMMENT '累计玩家经验；小岛成长权威读模型'
    AFTER exp;

CREATE TABLE IF NOT EXISTS island_level_config
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

CREATE TABLE IF NOT EXISTS player_island_level_reward_claim
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id    BIGINT NOT NULL COMMENT '玩家角色ID',
    island_level INT NOT NULL COMMENT '已领取的小岛等级',
    claimed_at   DATETIME NOT NULL COMMENT '领取时间',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_player_island_level_reward (player_id, island_level),
    INDEX idx_island_level_reward_player (player_id)
) COMMENT = '玩家逐级固定奖励领取记录';

INSERT INTO item_config (id, name, type, icon, sell_price) VALUES
('orange','橙子','CROP','orange',10),
('blueberry','蓝莓','CROP','blueberry',12),
('apple','苹果','CROP','apple',14),
('watermelon','西瓜','CROP','watermelon',16),
('wheat','小麦','CROP','wheat',10),
('lemon','柠檬','CROP','lemon',14),
('cucumber','黄瓜','CROP','cucumber',12),
('milk','牛奶','MATERIAL','milk',0),
('egg','鸡蛋','MATERIAL','egg',0)
ON DUPLICATE KEY UPDATE
name=VALUES(name), type=VALUES(type), icon=VALUES(icon), sell_price=VALUES(sell_price);

UPDATE crop_config
SET player_unlock_level = 2
WHERE crop_id = 'carrot';

INSERT INTO crop_config
(crop_id,name,rarity,reward_eligible,permanent_unlock_enabled,upgrade_enabled,
 player_unlock_level,max_crop_level,enabled) VALUES
('orange','橙子','COMMON',0,1,0,3,1,1),
('blueberry','蓝莓','COMMON',0,1,0,5,1,1),
('apple','苹果','COMMON',0,1,0,6,1,1),
('watermelon','西瓜','COMMON',0,1,0,7,1,1),
('wheat','小麦','COMMON',0,1,0,8,1,1),
('lemon','柠檬','COMMON',0,1,0,9,1,1),
('cucumber','黄瓜','COMMON',0,1,0,10,1,1)
ON DUPLICATE KEY UPDATE
name=VALUES(name),
permanent_unlock_enabled=VALUES(permanent_unlock_enabled),
player_unlock_level=VALUES(player_unlock_level),
enabled=VALUES(enabled);

INSERT INTO crop_level_config
(crop_id,crop_level,grow_seconds,yield_count,harvest_exp,upgrade_gold) VALUES
('orange',1,240,3,15,0),
('blueberry',1,300,4,18,0),
('apple',1,360,4,22,0),
('watermelon',1,480,3,25,0),
('wheat',1,300,5,20,0),
('lemon',1,420,4,25,0),
('cucumber',1,360,4,22,0)
ON DUPLICATE KEY UPDATE
grow_seconds=VALUES(grow_seconds),
yield_count=VALUES(yield_count),
harvest_exp=VALUES(harvest_exp);

INSERT INTO recipe_config
(id,name,output_item,make_time,unlock_level,sale_gold,sale_exp,
 bar_sale_interval_seconds,order_weight,enabled) VALUES
('carrot_juice','胡萝卜汁','carrot_juice',0,2,35,5,180,100,1),
('orange_juice','橙汁','orange_juice',0,3,40,6,180,100,1),
('tomato_juice','番茄汁','tomato_juice',0,4,40,6,180,100,1),
('milk_ice_cream','牛奶冰淇淋','milk_ice_cream',0,5,55,8,180,100,1),
('apple_carrot_juice','苹果胡萝卜汁','apple_carrot_juice',0,6,50,7,180,100,1),
('watermelon_milk_ice_cream','西瓜牛奶冰淇淋','watermelon_milk_ice_cream',0,7,65,9,180,100,1),
('strawberry_cake','草莓蛋糕','strawberry_cake',0,8,80,12,180,100,1),
('lemon_milk_ice_cream','柠檬牛奶冰淇淋','lemon_milk_ice_cream',0,9,70,10,180,100,1),
('cucumber_apple_juice','黄瓜苹果汁','cucumber_apple_juice',0,10,60,8,180,100,1)
ON DUPLICATE KEY UPDATE
name=VALUES(name),
output_item=VALUES(output_item),
unlock_level=VALUES(unlock_level),
enabled=VALUES(enabled);

INSERT INTO recipe_material (recipe_id,item_id,count)
SELECT material.recipe_id, material.item_id, material.item_count
FROM (
    SELECT 'carrot_juice' recipe_id, 'carrot' item_id, 2 item_count
    UNION ALL SELECT 'orange_juice','orange',2
    UNION ALL SELECT 'tomato_juice','tomato',2
    UNION ALL SELECT 'milk_ice_cream','milk',2
    UNION ALL SELECT 'apple_carrot_juice','apple',1
    UNION ALL SELECT 'apple_carrot_juice','carrot',1
    UNION ALL SELECT 'watermelon_milk_ice_cream','watermelon',2
    UNION ALL SELECT 'watermelon_milk_ice_cream','milk',1
    UNION ALL SELECT 'strawberry_cake','strawberry',2
    UNION ALL SELECT 'strawberry_cake','wheat',2
    UNION ALL SELECT 'strawberry_cake','egg',1
    UNION ALL SELECT 'lemon_milk_ice_cream','lemon',2
    UNION ALL SELECT 'lemon_milk_ice_cream','milk',1
    UNION ALL SELECT 'cucumber_apple_juice','cucumber',1
    UNION ALL SELECT 'cucumber_apple_juice','apple',1
) material
WHERE NOT EXISTS (
    SELECT 1
    FROM recipe_material existing
    WHERE existing.recipe_id = material.recipe_id
      AND existing.item_id = material.item_id
);

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
(10,3200,'cucumber','cucumber_apple_juice',NULL,NULL,1)
ON DUPLICATE KEY UPDATE
cumulative_exp=VALUES(cumulative_exp),
crop_id=VALUES(crop_id),
recipe_id=VALUES(recipe_id),
material_source_hint=VALUES(material_source_hint),
shop_capability_hint=VALUES(shop_capability_hint),
enabled=VALUES(enabled);

INSERT INTO crop_unlock_source
(crop_id,source_type,currency_type,price,required_player_level,source_ref_id,enabled) VALUES
('carrot','LEVEL_REWARD','NONE',0,2,'ISLAND_LEVEL_2',1),
('orange','LEVEL_REWARD','NONE',0,3,'ISLAND_LEVEL_3',1),
('tomato','LEVEL_REWARD','NONE',0,4,'ISLAND_LEVEL_4',1),
('blueberry','LEVEL_REWARD','NONE',0,5,'ISLAND_LEVEL_5',1),
('apple','LEVEL_REWARD','NONE',0,6,'ISLAND_LEVEL_6',1),
('watermelon','LEVEL_REWARD','NONE',0,7,'ISLAND_LEVEL_7',1),
('wheat','LEVEL_REWARD','NONE',0,8,'ISLAND_LEVEL_8',1),
('lemon','LEVEL_REWARD','NONE',0,9,'ISLAND_LEVEL_9',1),
('cucumber','LEVEL_REWARD','NONE',0,10,'ISLAND_LEVEL_10',1)
ON DUPLICATE KEY UPDATE
currency_type=VALUES(currency_type),
price=VALUES(price),
required_player_level=VALUES(required_player_level),
enabled=VALUES(enabled);

-- Convert the old "experience remaining inside the current level" without
-- changing that legacy column yet. Task 02 will atomically switch reward paths.
UPDATE game_player player
SET player.cumulative_exp = GREATEST(
    COALESCE(player.exp, 0) + COALESCE((
        SELECT SUM(config.required_exp)
        FROM player_level_config config
        WHERE config.level < player.level
    ), 0),
    CASE
        WHEN player.level >= 10 THEN 3200
        WHEN player.level = 9 THEN 2500
        WHEN player.level = 8 THEN 1900
        WHEN player.level = 7 THEN 1400
        WHEN player.level = 6 THEN 1000
        WHEN player.level = 5 THEN 700
        WHEN player.level = 4 THEN 450
        WHEN player.level = 3 THEN 250
        WHEN player.level = 2 THEN 100
        ELSE 0
    END
)
WHERE player.cumulative_exp IS NULL;

ALTER TABLE game_player
    MODIFY COLUMN cumulative_exp INT NOT NULL DEFAULT 0
    COMMENT '累计玩家经验；小岛成长权威读模型';

-- Existing players are backfilled lazily by /game/init so qualifications and
-- claim records remain in one application transaction with unique-key safety.
