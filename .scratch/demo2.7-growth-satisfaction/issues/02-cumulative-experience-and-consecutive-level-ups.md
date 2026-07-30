# 02 — 结算累计经验并连续发放升级奖励

**What to build:** 将玩家经验改为累计值，并把现有收获、顾客订单和吧台收益接入同一个连续升级事务。一次经验结算跨越多个等级时，按顺序幂等发放每一级固定作物种植权与等级赠送配方。

**Blocked by:** 01 — 建立累计小岛等级配置与固定奖励

**Status:** resolved

## Acceptance criteria

- [x] 先通过现有收获、顾客订单和吧台 JWT HTTP 公共接缝写出失败测试，再修改经验结算
- [x] 玩家经验升级后保持累计值，不再扣除当前等级阈值
- [x] 当前等级由累计经验阈值决定但只升不降
- [x] 一次收益跨越多个阈值时连续提升全部可达等级
- [x] 连续升级按等级升序发放每一级固定作物种植权、等级赠送配方和提示
- [x] 收获作物按本轮收获经验快照结算一次玩家经验
- [x] 顾客订单按实际交付份数和单份经验快照结算玩家经验
- [x] 吧台收取和下架只按实际已售份数与单份经验快照结算玩家经验
- [x] 单纯制作、上架、暂时没货和退回未售库存不发玩家经验
- [x] 玩家行锁、经验变更、等级变更和全部逐级奖励在同一事务内完成
- [x] 重复或并发结算不会丢失经验、重复升级或重复发放资格
- [x] 返回结果包含结算前后等级、累计经验、下一级阈值和本次逐级奖励
- [x] 配置阈值提高后，已有玩家等级不会被降低
- [x] 测试覆盖恰好达到阈值、一次跨多级、十级封顶、零经验、多玩家隔离和并发收益

## Parent

- `.scratch/demo2.7-growth-satisfaction/spec.md`

## Comments

- 重写 `GamePlayerServiceImpl.applyExperience()`：从旧的 `player_level_config` + 扣除阈值模式切换为 `cumulativeExp` + `island_level_config` + 不扣除阈值模式
- `ExpGainResult` DTO 字段从 `(gainedExp, beforeLevel, afterLevel, currentExp, nextLevelExp, levelsGained, rewardGold)` 改为 `(gainedExp, beforeLevel, afterLevel, cumulativeExp, nextLevelThreshold, levelsGained, levelRewards)`
- 更新所有调用方：`PlayerLandServiceImpl`、`OrderFulfillmentServiceImpl`、`DrinkBarServiceImpl` 及对应 VO
- 三个经验结算入口（收获 `addExp`、顾客订单 `settleDrinkSaleReward`、吧台收取/下架 `settleDrinkSaleReward`）均使用 `selectForUpdate` 行锁
- 升级时按等级升序幂等发放作物种植权和配方，`player_island_level_reward_claim` 唯一约束保证幂等
- 修复 lambda 变量 effectively-final 编译错误（引入 `resolvedLevel`）
- 更新 `DrinkBarControllerHttpTest`：`currentExp` → `cumulativeExp`，`SET exp` → `SET cumulative_exp`，移除旧等级奖励金币断言
- 更新 `OrderFulfillmentServiceImplTest` 和 `PlayerLandServiceImplTest` 的 `ExpGainResult` 构造函数调用
- 69 个测试全部通过
