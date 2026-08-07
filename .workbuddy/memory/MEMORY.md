# 项目记忆

## 项目概况
果香小岛游戏项目，Spring Boot + MyBatis-Plus 后端 + TypeScript/HTML 客户端。
采用 TDD 工作流，Issue 以 `.scratch/<feature>/issues/` 下 Markdown 管理。

## Demo3.0 无限等级体系（截至 2026-08-07）
- **LevelFormulaUtil**: `expToNext(level) = 100 × level^1.3`，MAX_TABLE_LEVEL=20，Lv1-20表配置+Lv21+公式递推
- **MasteryBonusUtil**: 产量+floor(Lv/5)×5%封顶+100%(Lv100)、售价+floor(Lv/10)×10%封顶+50%(Lv50)、生长-Lv×0.5%封底-40%(Lv80)
- **应用点**: 种植时快照生长时间(growth)、收获时计算产量(yield)、上架时快照售价(price)
- **island_level_config**: 扩展至Lv1-20，crop_id/recipe_id改为可空(Lv11+为NULL)，CHECK改为level>=1
- **Lv11-20 cumulative_exp**: 5195, 7453, 9981, 12787, 15877, 19257, 22932, 26909, 31193, 35788
- **GamePlayerServiceImpl.applyExperience()**: 支持Lv21+公式计算，null检查cropId/recipeId，Lv5额外授予milk_ice_cream
- **IslandGrowthServiceImpl.initialize()**: 从cumulative_exp重算等级，null检查，formula-based nextThreshold
- **PlayerLandServiceImpl**: 种植时应用生长加成、收获时应用产量加成
- **DrinkBarServiceImpl + PlayerCakeRackServiceImpl**: 上架时应用售价加成
- **测试**: 167测试全通过（4处断言修复 + 1个测试重命名）
- **MySQL迁移**: migration-demo30-infinite-level.sql 已执行，20行数据验证通过
- **Maven注意**: `mvn test -pl game-server` 必须先 `mvn install -pl common -DskipTests`，否则用旧JAR

## Demo3.0 经济重平衡（截至 2026-08-07）
- **已完成**：schema-h2.sql + schema.sql(MySQL) 全量经济数值更新 + MySQL 数据库迁移
- **item_config**: 53行，原料售价1-10（原5-120），成品出售价10-128
- **recipe_config**: 10饮品+17蛋糕配方，饮品价格25-55（原30-70），经验10-30（原5-10）
- **crop_level_config**: 收获经验砍77-87%（strawberry Lv1: 4→1, Lv10: 11→3）
- **吧台/蛋糕架经验**: 配方经验的50%（整数除法）
- **新增配方**: blueberry_juice（Lv5解锁，30gold/12exp），island_level_config Lv5改为blueberry_juice，milk_ice_cream在代码中额外授予
- **测试**: 167测试全通过（DrinkBarControllerHttpTest 6处断言修复）
- **MySQL迁移**: migration-economy-rebalance.sql 已执行，53 items/27 recipes/76 materials/101 crop_levels 全部验证通过
- **待办**: ~~game-server 需重启~~ ✅ 已于 20:12 重启完成（PID 38842, 端口8082）
- **MySQL schema.sql 注意**: 仍缺Demo2.7/2.9/2.10表定义，需后续补建
- **MySQL 花卉 schema 修复**: migration-demo28-flower-schema.sql（flower_config 列重命名+补充，player_flower_right 列重命名）

## Demo2.10 进度（截至 2026-08-07）
- **Issue 01-04（已完成）**：蛋糕店后端 — recipe_config扩展(craft_station/obtain_channel)、3张表(cake_shop_config/player_cake_shop/player_cake_rack)、3个Entity+Mapper、6个Service+Impl、CakeShopStatusVO、CakeShopController(8端点)、41个HTTP集成测试
- **蛋糕店**：岛屿Lv8解锁，5000金币，10级(架位8-15, 周期480-360s)
- **17个蛋糕配方**：5个岛屿升级赠送 + 12个交易所购买
- **惰性销售结算**：sold = elapsedSinceList / saleIntervalSnapshot (floor)，快照锁定上架时参数
- **架位状态**：EMPTY → SELLING → SOLD_OUT → EMPTY (after collect)
- **下架**：退回未售蛋糕 + 结算已售收益(settleDrinkSaleReward)
- **WebConfig**：JWT拦截器添加 /cake-shop/** 路径
- **Issue 05（已完成）**：客户端 — TS类型+API+demo2.8-island.html蛋糕店管理面板+demo2.8-world.html建筑渲染增强
- **Bug修复**：demo2.8-island.html 新增 apiCall() 辅助函数，修复 Demo2.9 livestock 代码引用未定义函数的 bug
- **总测试**：167测试全通过（126旧+41新蛋糕店）
- **未提交**：因 macOS Documents 保护无法从沙箱同步到主仓库，需手动同步并提交
- 文件：schema-h2.sql、CakeShopController、PlayerCakeShopServiceImpl、PlayerCakeRackServiceImpl、CakeShopHttpTest、demo2.8-island.html、demo2.8-world.html

## Demo2.9 进度（截至 2026-08-07）
- **Issue 01-05（已完成）**：畜牧系统后端 — 4张表(barn_config/coop_config/player_barn/player_coop)、4个Entity+Mapper、4个Service+Impl、LivestockStatusVO、LivestockController(5端点)、25个HTTP集成测试
- **牛棚**：岛屿Lv5解锁，1000金币，600s周期，10级(capacity 1-6, milk_per_cow 10-28)，解锁赠1牛+10牛奶
- **鸡舍**：岛屿Lv8解锁，3000金币，600s/570s(Lv4+)周期，10级(capacity 1-8, Lv7+奖励蛋)，解锁赠1鸡+5鸡蛋
- **惰性结算**：completedCycles = elapsedSeconds / cycleSeconds，快照锁定当前周期
- **动物上限**：8只硬上限（capacity 和 8 取 min）
- **WebConfig**：JWT拦截器添加 /livestock/** 路径
- **Issue 06（已完成）**：客户端 — TS类型+API+demo2.8-island.html畜牧管理面板+demo2.8-world.html建筑渲染增强
- **总测试**：126测试全通过（101旧+25新畜牧）
- **未提交**：因 macOS Documents 保护无法从沙箱 git commit，代码已同步到主仓库，需手动提交
- 文件：schema-h2.sql、LivestockController、PlayerBarnServiceImpl、PlayerCoopServiceImpl、LivestockHttpTest、demo2.8-island.html、demo2.8-world.html

## Demo2.8 进度（截至 2026-08-07）
- **任务 1-2（已完成）**：重建作物目录（删4种→11种，每种10级配置）、花卉系统（8种花+80级配置+player_flower_right表）
- **任务 3（已完成）**：蜂蜜系统 — BeehiveController(purchase/collect/status)、PlayerBeehiveServiceImpl(2h周期/惰性结算/存储上限20-40-60)
- **任务 4（已完成）**：GameController.gameInit() 整合花卉蜂蜜数据（保留Demo2.7满意度奖励）；FlowerController(purchase/upgrade)、BeehiveController 已接入
- **任务 5（已完成）**：FlowerBeehiveHttpTest 17个测试全通过
- **Schema 合并（已完成）**：schema-h2.sql 合并 Demo2.7 表(daily_satisfaction/drink_shop_level_config/satisfaction_gift_config/player_drink_shop) + Demo2.8 表(flower_config/flower_level_config/player_flower_right/player_beehive)，共36表652行
- **Demo2.7 代码恢复（已完成）**：rsync 从旧 worktree 同步时丢失了 15 个文件的 Demo2.7 代码，从 git HEAD 恢复（DrinkShopController/DrinkShopServiceImpl/DrinkBarServiceImpl/OrderFulfillmentServiceImpl 等）
- **关键修复**：WebConfig JWT拦截器需覆盖 `/flower/**`、`/beehive/**`、`/crop/**` 路径
- **测试经验**：MockMvc 不支持真正的并发请求测试，并发幂等改为顺序测试验证相同业务规则
- **客户端（已完成）**：TS 类型+API+DataManager 更新；demo2.8-island.html（1530行，6个drawer全功能：成长/背包/制作台/吧台/花卉/蜂箱）；demo2.8-world.html（花园种植+蜂箱渲染）；删除10个过期HTML
- **全量测试**：101 测试全部通过（0失败0错误），已提交到 demo2.8-crop-progression 分支（commit dabc6b6）
- **Git 分支**：demo2.8-crop-progression（基于 demo2.7-cumulative-exp）
- 文件：schema-h2.sql、FlowerController/BeehiveController/PlayerFlowerRightServiceImpl/PlayerBeehiveServiceImpl、demo2.8-island.html/demo2.8-world.html

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
