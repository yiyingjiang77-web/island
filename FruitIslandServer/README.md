# FruitIslandServer

## 作物与种植权模型

种子不再作为背包消耗品。玩家获得某个普通作物后，会永久获得该品种的种植权，
以后可以无限次种植；永久作物可使用金币升级。

| 表 | 用途 |
| --- | --- |
| `crop_config` | 作物名称、稀有度、玩家解锁等级、最高等级、是否允许奖励/升级 |
| `crop_level_config` | 每个作物等级对应的成熟秒数、收获数量和升级金币 |
| `crop_unlock_source` | 金币商店、元宝商店、等级奖励等永久获得渠道 |
| `player_crop` | 玩家永久拥有的作物及当前等级 |
| `player_crop_grant` | 玩家限时拥有的稀有作物权限 |
| `crop_reward_pool_item` | 可配置权重和有效期的稀有作物随机奖励池 |
| `crop_plant` | 每次种植的等级、成熟时间、产量及权限来源快照 |
| `flower_config` | 花卉名称、永久种植权货币与价格、蜂蜜系数 |
| `flower_level_config` | 花卉成熟时间、产量、经验与升级金币 |
| `player_flower_right` | 玩家永久拥有的花卉种植权和当前等级 |

只有满足以下条件的稀有作物才能作为限时奖励：

- `rarity` 不是 `COMMON`
- `reward_eligible = 1`
- 奖励有效期大于 0

限时权限有效期内可以无限次种植，但不能升级。只要在到期前成功种下，即使权限
随后到期，该作物仍可继续生长和收获。

## 数据库初始化与升级

新建数据库直接执行：

`game-server/src/main/resources/db/schema.sql`

已有旧版数据库先备份，再执行一次：

`game-server/src/main/resources/db/migration-crop-system-v2.sql`

Demo2.8 花园与花卉种植执行：

`game-server/src/main/resources/db/migration-demo28-flower-planting.sql`

迁移脚本会把旧背包中的种子转换为对应作物的永久种植权，然后清理旧种子条目。

## 接口

- `GET /game/init`：返回作物基础配置、等级配置、获得渠道、永久权限和有效限时权限。
- `POST /crop/upgrade`：使用配置中的金币价格将永久作物提升一级。
- `POST /farm/plant`：校验永久/限时权限后种植，不消耗背包物品。
- `POST /farm/water`：第一次浇水后按种植快照开始成熟倒计时。
- `POST /farm/harvest`：按种植快照发放收获物。
- `GET /flower/catalog`：返回八种花卉目录与当前玩家永久种植权。
- `POST /flower/buy`：使用配置中的金币或钻石购买永久花卉种植权。
