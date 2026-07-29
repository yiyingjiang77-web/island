USE fruit_island;

CREATE TABLE IF NOT EXISTS player_recipe
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

INSERT INTO item_config (id, name, type, icon, sell_price)
VALUES ('strawberry_juice', '草莓汁', 'DRINK', 'strawberry_juice', 0)
ON DUPLICATE KEY UPDATE name=VALUES(name), type=VALUES(type), icon=VALUES(icon);

INSERT INTO recipe_config (id, name, output_item, make_time, unlock_level)
VALUES ('strawberry_juice', '草莓汁', 'strawberry_juice', 0, 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), output_item=VALUES(output_item), make_time=VALUES(make_time);

INSERT INTO recipe_material (recipe_id, item_id, count)
SELECT 'strawberry_juice', 'strawberry', 2
WHERE NOT EXISTS (
    SELECT 1 FROM recipe_material
    WHERE recipe_id='strawberry_juice' AND item_id='strawberry'
);

INSERT INTO player_recipe (player_id, recipe_id, unlock_source, unlock_time)
SELECT id, 'strawberry_juice', 'MIGRATION_DEFAULT', NOW()
FROM game_player
ON DUPLICATE KEY UPDATE recipe_id=VALUES(recipe_id);
