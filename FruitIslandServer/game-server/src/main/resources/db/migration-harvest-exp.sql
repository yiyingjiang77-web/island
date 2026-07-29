-- Demo2.4：作物等级收获经验、种植经验快照和玩家等级配置。
-- 在已有 fruit_island 数据库执行一次；全新数据库直接使用 schema.sql。
USE fruit_island;

ALTER TABLE crop_level_config
    ADD COLUMN harvest_exp INT NOT NULL DEFAULT 0
        COMMENT '收获该等级作物一次获得的玩家经验'
        AFTER yield_count;

ALTER TABLE player_land
    ADD COLUMN harvest_exp_snapshot INT NULL
        COMMENT '本轮收获经验快照'
        AFTER yield_count_snapshot;

ALTER TABLE crop_plant
    ADD COLUMN harvest_exp_snapshot INT NULL
        COMMENT '种植时收获经验快照'
        AFTER yield_count_snapshot;

CREATE TABLE player_level_config
(
    level           INT PRIMARY KEY COMMENT '当前玩家等级',
    required_exp    INT NOT NULL COMMENT '从当前等级升到下一级所需经验',
    reward_gold     BIGINT NOT NULL DEFAULT 0 COMMENT '升到下一级时奖励金币',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CHECK (level >= 1 AND required_exp > 0 AND reward_gold >= 0)
) COMMENT = '玩家等级成长配置表';

UPDATE crop_level_config
SET harvest_exp = CASE crop_id
    WHEN 'strawberry' THEN CASE crop_level WHEN 1 THEN 5 WHEN 2 THEN 8 ELSE 12 END
    WHEN 'cabbage' THEN CASE crop_level WHEN 1 THEN 8 WHEN 2 THEN 12 ELSE 18 END
    WHEN 'carrot' THEN CASE crop_level WHEN 1 THEN 12 WHEN 2 THEN 18 ELSE 25 END
    WHEN 'tomato' THEN CASE crop_level WHEN 1 THEN 15 WHEN 2 THEN 22 ELSE 30 END
    WHEN 'potato' THEN CASE crop_level WHEN 1 THEN 18 WHEN 2 THEN 26 ELSE 36 END
    WHEN 'chili' THEN CASE crop_level WHEN 1 THEN 25 WHEN 2 THEN 36 ELSE 50 END
    WHEN 'corn' THEN CASE crop_level WHEN 1 THEN 30 WHEN 2 THEN 45 ELSE 65 END
    WHEN 'moonberry' THEN 40
    ELSE 0
END;

INSERT INTO player_level_config (level, required_exp, reward_gold) VALUES
(1,100,50),(2,150,75),(3,220,100),(4,300,125),(5,400,150),
(6,520,180),(7,660,210),(8,820,250),(9,1000,300),(10,1200,350),
(11,1450,400),(12,1700,450),(13,2000,500),(14,2350,550),(15,2750,600),
(16,3200,700),(17,3700,800),(18,4250,900),(19,4850,1000),(20,5500,1200);

-- 已经处于生长中的旧作物补齐快照，保证上线后仍可正常收获经验。
UPDATE player_land pl
JOIN crop_level_config cl
  ON cl.crop_id = pl.crop_id AND cl.crop_level = pl.crop_level
SET pl.harvest_exp_snapshot = cl.harvest_exp
WHERE pl.crop_id IS NOT NULL AND pl.harvest_exp_snapshot IS NULL;

UPDATE crop_plant cp
JOIN crop_level_config cl
  ON cl.crop_id = cp.crop_id AND cl.crop_level = cp.crop_level
SET cp.harvest_exp_snapshot = cl.harvest_exp
WHERE cp.crop_id IS NOT NULL AND cp.harvest_exp_snapshot IS NULL;
