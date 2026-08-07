-- Demo2.8：花园使用独立花卉配置与永久种植权；土地状态机继续复用 player_land。
CREATE TABLE IF NOT EXISTS flower_config
(
    flower_id          VARCHAR(64) PRIMARY KEY COMMENT '花卉编码，也是收获物品ID',
    name               VARCHAR(64) NOT NULL COMMENT '显示名称',
    purchase_currency  VARCHAR(16) NOT NULL COMMENT 'GOLD/DIAMOND',
    purchase_price     BIGINT NOT NULL COMMENT '永久种植权价格',
    honey_coefficient  INT NOT NULL DEFAULT 1 COMMENT '后续蜂蜜产量系数',
    max_flower_level   INT NOT NULL DEFAULT 10 COMMENT '最高花卉等级',
    enabled            TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time        DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK (purchase_price >= 0 AND honey_coefficient > 0)
) COMMENT = '花卉基础配置表';

CREATE TABLE IF NOT EXISTS flower_level_config
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    flower_id      VARCHAR(64) NOT NULL,
    flower_level   INT NOT NULL,
    grow_seconds   INT NOT NULL,
    yield_count    INT NOT NULL,
    harvest_exp    INT NOT NULL DEFAULT 0,
    upgrade_gold   BIGINT NOT NULL DEFAULT 0,
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_flower_level (flower_id, flower_level)
) COMMENT = '花卉等级数值配置表';

CREATE TABLE IF NOT EXISTS player_flower_right
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id          BIGINT NOT NULL,
    flower_id          VARCHAR(64) NOT NULL,
    flower_level       INT NOT NULL DEFAULT 1,
    purchase_currency  VARCHAR(16) NOT NULL,
    purchase_time      DATETIME NOT NULL,
    create_time        DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_player_flower (player_id, flower_id),
    INDEX idx_player_flower_player (player_id)
) COMMENT = '玩家永久花卉种植权';

INSERT IGNORE INTO item_config (id, name, type, icon, sell_price) VALUES
('rose', '玫瑰', 'FLOWER', 'rose', 8),
('chrysanthemum', '菊花', 'FLOWER', 'chrysanthemum', 6),
('jasmine', '茉莉', 'FLOWER', 'jasmine', 7),
('osmanthus', '桂花', 'FLOWER', 'osmanthus', 9),
('lavender', '薰衣草', 'FLOWER', 'lavender', 10),
('hibiscus', '扶桑花', 'FLOWER', 'hibiscus', 8),
('chamomile', '洋甘菊', 'FLOWER', 'chamomile', 6),
('sakura', '樱花', 'FLOWER', 'sakura', 18);

INSERT IGNORE INTO flower_config
(flower_id, name, purchase_currency, purchase_price, honey_coefficient, max_flower_level, enabled) VALUES
('rose', '玫瑰', 'GOLD', 500, 1, 10, 1),
('chrysanthemum', '菊花', 'GOLD', 300, 1, 10, 1),
('jasmine', '茉莉', 'GOLD', 400, 1, 10, 1),
('osmanthus', '桂花', 'GOLD', 600, 1, 10, 1),
('lavender', '薰衣草', 'GOLD', 800, 1, 10, 1),
('hibiscus', '扶桑花', 'GOLD', 500, 1, 10, 1),
('chamomile', '洋甘菊', 'GOLD', 300, 1, 10, 1),
('sakura', '樱花', 'DIAMOND', 10, 2, 10, 1);

INSERT IGNORE INTO flower_level_config
(flower_id, flower_level, grow_seconds, yield_count, harvest_exp, upgrade_gold) VALUES
('rose', 1, 300, 2, 4, 0),
('chrysanthemum', 1, 240, 3, 3, 0),
('jasmine', 1, 300, 3, 3, 0),
('osmanthus', 1, 360, 3, 4, 0),
('lavender', 1, 420, 2, 5, 0),
('hibiscus', 1, 300, 3, 4, 0),
('chamomile', 1, 240, 3, 3, 0),
('sakura', 1, 480, 2, 5, 0);
