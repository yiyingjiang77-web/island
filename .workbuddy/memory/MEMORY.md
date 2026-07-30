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
