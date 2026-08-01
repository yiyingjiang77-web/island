# 项目记忆

## 项目概况
果香小岛游戏项目，Spring Boot + MyBatis-Plus 后端 + TypeScript/HTML 客户端。
采用 TDD 工作流，Issue 以 `.scratch/<feature>/issues/` 下 Markdown 管理。

## Demo2.7 进度（截至 2026-07-30）
- **任务 01（已完成）**：建立 island_level_config 表（1-10级累计经验阈值 + 固定作物/配方）、player_island_level_reward_claim 表、game_player.cumulative_exp 字段、IslandGrowthService.initialize() 在 /game/init 时迁移并补发奖励
- **任务 02（已完成）**：applyExperience() 切换为累计经验模式，升级时发放作物种植权和配方，69 个测试全部通过。分支 demo2.7-cumulative-exp
- 任务 03-07：饮品店装修、等级驱动队列、每日满意度、成长面板 UI、满意度面板 UI

## 关键架构决策
- 双轨制过渡：game_player 同时保留旧 exp 和新 cumulative_exp，任务 01 只在 /game/init 迁移，任务 02 需将线上结算切换到 cumulative_exp
- 三个经验结算入口：收获(addExp)、顾客订单交付(settleDrinkSaleReward)、吧台收取/下架(settleDrinkSaleReward)
- 升级奖励幂等：player_island_level_reward_claim 有 (player_id, island_level) 唯一约束
- grantPermanent() 方法本身幂等，重复调用不会重复创建

## Demo3.0 设计方向（截至 2026-08-01）
- **场景改造**：果香小岛→果香山谷，海→河流，码头→木桥，48×48 网格不变只换叙事层
- **无限等级**：`exp_to_next = 100 × level^1.3`，Lv1-20 内容解锁（表配置），Lv21+ 公式计算（不存表）
- **精通加成有上限**：产量 +100%（Lv100封顶）、售价 +50%（Lv50封顶）、生长 -40%（Lv80封顶）
- **不设转生机制**
- **两套等级合并**：废弃 player_level_config + game_player.exp，统一用 island_level cumulative_exp
- **经济重平衡**：原料售价砍到 1-15（原 5-120），配方售价拉开到 20-250（原 20-80），收获经验砍到 1-8（原 5-40），吧台经验降至 50%
- **作物体系**：删除白菜/土豆/辣椒/玉米，剩 11 种全走岛屿升级赠送（单一渠道），商店不卖作物种子
- **花卉系统**：8 种可食用花卉（7 金币 + 1 钻石樱花），商店购买无等级限制，可升级 Lv1-10
- **蜂蜜系统**：单一物品，蜂箱产蜜 `floor(Σ(花数×系数×倍率))`，2h 周期，蜂箱最多 3 个存储 20/40/60
- **产蜜倍率**：`min(1+0.4×(lv-1)^0.85, 5.0)` 上限 5.0；产量倍率 `1+0.3×(lv-1)` 上限 3.0
- **配方总数**：11 基础 + 8 菌菇 + 16 花卉 = 35 个配方
- **5 个 NPC + 4 只小动物**：NPC 有 5 级好感度，动物有 3 级好感度（双轨制：每日领礼 +2 + 投喂 +1~5）
- **菌菇配方**：8 个配方在交易所金币购买（500-5000），松露系列为后期高价值追求
- 文档：`prd/demo3.0-npc-and-interactive-world-spec.md`、`prd/demo3.0-infinite-level-and-economy-spec.md`、`prd/demo3.0-economy-balance-sheet.md`
