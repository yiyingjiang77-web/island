USE fruit_island;

ALTER TABLE recipe_config
    ADD COLUMN sale_gold INT NOT NULL DEFAULT 0 COMMENT '单份售出金币',
    ADD COLUMN sale_exp INT NOT NULL DEFAULT 0 COMMENT '单份售出玩家经验',
    ADD COLUMN order_weight INT NOT NULL DEFAULT 1 COMMENT '订单配方权重',
    ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用';

ALTER TABLE customer_order
    ADD COLUMN recipe_id VARCHAR(64) COMMENT '配方ID',
    ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '订单份数',
    ADD COLUMN unit_gold_snapshot INT NOT NULL DEFAULT 0 COMMENT '单份金币快照',
    ADD COLUMN unit_exp_snapshot INT NOT NULL DEFAULT 0 COMMENT '单份经验快照',
    ADD COLUMN queue_position INT COMMENT '队列位置',
    ADD COLUMN close_time DATETIME COMMENT '关闭时间',
    ADD COLUMN close_reason VARCHAR(32) COMMENT '关闭原因',
    ADD INDEX idx_waiting_queue (player_id, status, queue_position);

CREATE TABLE customer_arrival_state
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id       BIGINT NOT NULL,
    next_arrival_at DATETIME COMMENT '下一位顾客到店时间；满员时为空',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_arrival_player (player_id)
) COMMENT = '玩家顾客到店状态';

CREATE TABLE order_quantity_weight
(
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    quantity INT NOT NULL,
    weight   INT NOT NULL,
    enabled  TINYINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_order_quantity (quantity),
    CHECK (quantity > 0 AND weight > 0)
) COMMENT = '顾客订单数量权重';

INSERT INTO order_quantity_weight (quantity, weight, enabled) VALUES
(1, 60, 1), (2, 30, 1), (3, 10, 1);

INSERT INTO customer_template (id, name, avatar, type) VALUES
('berry', '莓莓', '👧', 'ISLANDER'),
('sunny', '小晴', '🧒', 'ISLANDER'),
('captain', '船长', '🧔', 'VISITOR'),
('artist', '画家', '👩‍🎨', 'VISITOR'),
('ranger', '巡林员', '🧑‍🌾', 'ISLANDER');

UPDATE recipe_config
SET sale_gold=30, sale_exp=5, order_weight=100, enabled=1
WHERE id='strawberry_juice';
