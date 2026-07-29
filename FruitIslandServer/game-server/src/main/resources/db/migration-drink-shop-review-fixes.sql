USE fruit_island;

ALTER TABLE player_recipe
    DROP INDEX uk_player_recipe,
    ADD COLUMN qualification_type VARCHAR(16) NOT NULL DEFAULT 'PERMANENT' AFTER recipe_id,
    ADD COLUMN valid_from DATETIME NULL AFTER unlock_time,
    ADD COLUMN valid_until DATETIME NULL AFTER valid_from,
    ADD UNIQUE KEY uk_player_recipe (player_id, recipe_id, qualification_type);
