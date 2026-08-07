CREATE TABLE IF NOT EXISTS satisfaction_gift_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, tier_code VARCHAR(16) NOT NULL,
    minimum_percent TINYINT NOT NULL, minimum_delivered_quantity INT NOT NULL,
    reward_gold BIGINT NOT NULL, config_version INT NOT NULL DEFAULT 1,
    effective_from DATE NOT NULL, enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_satisfaction_gift_version (tier_code, config_version)
);
CREATE TABLE IF NOT EXISTS daily_satisfaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, player_id BIGINT NOT NULL,
    business_date DATE NOT NULL, delivered_orders INT NOT NULL, rejected_orders INT NOT NULL,
    closed_orders INT NOT NULL, delivered_quantity INT NOT NULL, satisfaction_percent TINYINT NOT NULL,
    gift_tier_snapshot VARCHAR(16), reward_gold_snapshot BIGINT NOT NULL DEFAULT 0,
    reward_status VARCHAR(24) NOT NULL, settled_at DATETIME NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_daily_satisfaction_player_date (player_id, business_date)
);
INSERT INTO satisfaction_gift_config
(tier_code,minimum_percent,minimum_delivered_quantity,reward_gold,config_version,effective_from,enabled) VALUES
('S60',60,20,100,1,'2020-01-01',1),('S70',70,20,200,1,'2020-01-01',1),
('S80',80,20,300,1,'2020-01-01',1),('S90',90,20,400,1,'2020-01-01',1),
('S100',100,20,500,1,'2020-01-01',1)
ON DUPLICATE KEY UPDATE reward_gold=VALUES(reward_gold);
