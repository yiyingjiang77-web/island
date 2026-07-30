USE fruit_island;

ALTER TABLE recipe_config
    ADD COLUMN bar_sale_interval_seconds INT NOT NULL DEFAULT 180
        COMMENT '吧台单份销售间隔秒数';

UPDATE recipe_config
SET bar_sale_interval_seconds = 180
WHERE id = 'strawberry_juice';

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

INSERT IGNORE INTO drink_bar (player_id, slot_number, opened)
SELECT player.id, slots.slot_number, 1
FROM game_player player
CROSS JOIN (
    SELECT 1 AS slot_number UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) slots;
