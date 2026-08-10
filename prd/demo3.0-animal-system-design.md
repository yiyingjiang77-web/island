# Demo3.0 小动物系统技术设计文档

> 状态：设计稿（v9 — v8 三次审查：CROP_UNLOCK/FLOWER_UNLOCK 拆分 + grantAnimalReward 修复 + feed decayApplied 修正 + 食材规则改写 + collect-all 跳过说明 + flower_config 字段说明 + recipe_category 映射表）
> 日期：2026-08-10
> 依据 PRD：`prd/demo3.0-npc-and-interactive-world-spec.md` 第 4-5 章
> 前置依赖：Demo3.0 配方商店系统（已完成）、Demo2.8 花卉蜂蜜系统（已完成）

---

## 1. 系统概述

小动物是地图原住民，不是 NPC 摊位。核心玩法循环：

```
动物每日自动产出野生食材 → 玩家去栖息地收取（前日好感结算数量）→ 收取 +2 好感
       ↓                                                                    ↓
  食材进入背包 → 制作配方 / 赠送 NPC            好感升级 → 产量增减 + 季节加成 + 等级奖励
       ↓
  先领礼物 → 再投喂食物 → 额外加速好感度提升
       ↓
  不互动则好感衰减 -2/天（1天宽限，惰性结算，下限 0，等级同步降级）
       ↓
  每 2 级触发等级奖励（食材 / 种子 / 配方交替）
  Lv20 每日额外获得 3 钻石
```

### 1.1 四只小动物

> **设计决策**：所有动物每天出现，分布在地图不同位置。动物有岛屿等级解锁门槛。

| 动物 | animal_id | 栖息地坐标 | 岛屿解锁等级 | 每日礼物 | 礼物模式 | 季节加成 |
|------|-----------|-----------|------------|---------|---------|---------|
| 小松鼠 | `squirrel` | (4, 8) | Lv3 | 榛果/松子/栗子（轮换） | ROTATION | 秋季 ×1.5 |
| 知更鸟 | `robin` | (6, 5) | Lv5 | 蔓越莓/树莓/桑葚（轮换） | ROTATION | 夏季 ×1.5 |
| 刺猬 | `hedgehog` | (27, 10) | Lv8 | 口蘑/香菇/鸡油菌（轮换） | ROTATION | 秋季 ×1.5 |
| 小狐狸 | `fox` | (15, 5) | Lv10 | 紫罗兰/蒲公英/金盏花/迷迭香（确定性随机） | RANDOM | 无 |

### 1.2 动物独家产出

> **设计决策**：坚果和菌菇只能通过动物每日礼物获得，不能种植、不能购买。其他动物产出可通过升级奖励获得种子种植权。

| 动物 | 独家产出（不可种植/购买） | 可种植产出（升级奖励种子） |
|------|----------------------|----------------------|
| 小松鼠 | 榛果、松子、栗子（坚果类） | — |
| 刺猬 | 口蘑、香菇、鸡油菌（菌菇类） | — |
| 知更鸟 | — | 蔓越莓、树莓、桑葙 |
| 小狐狸 | — | 蒲公英、紫罗兰、迷迭香；金盏花可花种子商店购买 |

### 1.3 核心交互规则

1. **前日结算**：今日礼物数量基于领取时的 `effectiveFavorability`（反映截止昨天的互动结果），领取后加的 +2 好感只影响明天的礼物数量。
2. **先领后喂**：如果当天礼物未领取，投喂按钮不可用。必须先 collect，才能 feed。
3. **批量领取**：提供 `POST /animal/collect-all` 端点，一次性领取所有可用动物礼物。部分成功模式，单只失败不影响其他。

---

## 2. 好感度系统（20 级体系）

### 2.1 等级与效果

> **设计决策**：好感度分 20 级，每 2 级触发一次等级奖励。

**好感度公式**：`升下一级需要 = 3 + 等级 × 1`

| 等级 | 单级需要 | 累计好感度 | 每日礼物数 | 等级奖励 |
|------|---------|-----------|-----------|---------|
| Lv1 | 4 | 0 | 2 | — |
| Lv2 | 5 | 4 | 4 | ✅ |
| Lv3 | 6 | 9 | 6 | — |
| Lv4 | 7 | 15 | 8 | ✅ |
| Lv5 | 8 | 22 | 10 | — |
| Lv6 | 9 | 30 | 12 | ✅ |
| Lv7 | 10 | 39 | 14 | — |
| Lv8 | 11 | 49 | 16 | ✅ |
| Lv9 | 12 | 60 | 18 | — |
| Lv10 | 13 | 72 | 20 | ✅ |
| Lv11 | 14 | 85 | 22 | — |
| Lv12 | 15 | 99 | 24 | ✅ |
| Lv13 | 16 | 114 | 26 | — |
| Lv14 | 17 | 130 | 28 | ✅ |
| Lv15 | 18 | 147 | 30 | — |
| Lv16 | 19 | 165 | 32 | ✅ |
| Lv17 | 20 | 184 | 34 | — |
| Lv18 | 21 | 204 | 36 | ✅ |
| Lv19 | 22 | 225 | 38 | — |
| Lv20 | — | 247 | 40 | ✅ + 每日 3 钻石 |

**每日礼物公式**：`每日礼物数 = 动物等级 × 2`

**Lv20 钻石奖励**：
- 维持 Lv20 时，每日领取礼物额外获得 3 钻石
- 降到 Lv19 以下，当日不获得钻石
- 恢复 Lv20 后重新获得

**升级节奏预估**：

| 策略 | 每日好感 | 到 Lv10(72) | 到 Lv20(247) |
|------|---------|-------------|-------------|
| 只领礼物不投喂 | +2 | 36 天 | 124 天 |
| 领礼物 + 投喂最爱 | +7 | 11 天 | 36 天 |

### 2.2 好感值来源与衰减

| 来源 | 好感值 | 频率 |
|------|--------|------|
| 每日领取礼物 | +2 | 每天1次 |
| 每日投喂（最爱） | +5 | 每天1次 |
| 每日投喂（喜欢） | +3 | 每天1次 |
| 每日投喂（其他） | 不消耗，提示"动物不喜欢" | — |
| **每日不互动（衰减）** | **-2** | **每天1次，下限 0，1天宽限期** |

> **衰减规则**：
> - "不互动" = 当天既未领取礼物也未投喂
> - 打开面板（list API）**不算**互动
> - 衰减为**惰性结算**：不修改数据库，读取时虚拟计算
> - 好感值下限为 **0**
> - **1天宽限期**：漏 1 天不衰减，从第 2 天起每天 -2

### 2.3 季节系统

使用服务器 `Asia/Shanghai` 时区的当前月份推断季节（注入 `Clock`）。

- 春季：3, 4, 5 月
- 夏季：6, 7, 8 月
- 秋季：9, 10, 11 月
- 冬季：12, 1, 2 月

季节加成效果：
- 小松鼠 / 刺猬：秋季每日产量 ×1.5
- 知更鸟：夏季每日产量 ×1.5
- Lv2+ 时季节加成生效（Lv1 无季节加成）

### 2.4 等级同步逻辑（双向）

```java
/** 好感值 → 等级（20级体系） */
private int favToLevel(int fav) {
    int level = 1;
    int threshold = 0;
    for (int lv = 1; lv < 20; lv++) {
        threshold += 3 + lv; // 升下一级需要 3 + 等级×1
        if (fav >= threshold) level = lv + 1;
        else break;
    }
    return level;
}

/**
 * 同步等级（双向）。在 collect/feed 持久化好感值后调用。
 * - 升级：直接覆盖 level，偶数等级触发奖励
 * - 降级：直接覆盖 level（衰减导致），不撤销已解锁的配方/种植权
 */
private void syncLevel(PlayerAnimal pa, int effectiveFavorability, LocalDate today) {
    int newLevel = favToLevel(effectiveFavorability);
    int oldLevel = pa.getLevel();
    pa.setLevel(newLevel);

    if (newLevel > oldLevel && newLevel % 2 == 0) {
        // 偶数等级触发奖励
        grantAnimalReward(pa.getPlayerId(), pa.getAnimalId(), newLevel, today);
    }
}
```

### 2.5 衰减计算（惰性虚拟计算）

```java
private int getEffectiveFavorability(PlayerAnimal pa, LocalDate today) {
    if (pa.getLastGiftDate() == null && pa.getLastFeedDate() == null) {
        return pa.getFavorability();
    }
    LocalDate lastInteraction = pa.getLastGiftDate();
    if (pa.getLastFeedDate() != null &&
        (lastInteraction == null || pa.getLastFeedDate().isAfter(lastInteraction))) {
        lastInteraction = pa.getLastFeedDate();
    }
    long missedDays = ChronoUnit.DAYS.between(lastInteraction, today) - 1;
    if (missedDays <= 1) return pa.getFavorability();
    int decayDays = (int)(missedDays - 1);
    return Math.max(0, pa.getFavorability() - decayDays * 2);
}
```

---

## 3. 食物偏好（代码硬编码）

> **设计决策**：不建 `animal_food_preference` 表，偏好逻辑硬编码在 `AnimalServiceImpl` 中。每只动物只有 2 种可投喂食物：最爱(+5) 和 喜欢(+3)，其他物品提示"动物不喜欢"且不消耗。

### 3.1 偏好配置

| 动物 | 最爱(+5) | 喜欢(+3) |
|------|---------|---------|
| 小松鼠 | 榛果 hazelnut | 草莓 strawberry |
| 刺猬 | 蓝莓 blueberry | 苹果 apple |
| 知更鸟 | 桑葙 mulberry | 松子 pine_nut |
| 小狐狸 | 鸡蛋 egg | 牛奶 milk |

### 3.2 实现代码

```java
private int getPreferenceLevel(String animalId, String itemId) {
    return switch (animalId) {
        case "squirrel" -> itemId.equals("hazelnut") ? 5 :
                           itemId.equals("strawberry") ? 3 : -1;
        case "hedgehog" -> itemId.equals("blueberry") ? 5 :
                           itemId.equals("apple") ? 3 : -1;
        case "robin"    -> itemId.equals("mulberry") ? 5 :
                           itemId.equals("pine_nut") ? 3 : -1;
        case "fox"      -> itemId.equals("egg") ? 5 :
                           itemId.equals("milk") ? 3 : -1;
        default -> -1;
    };
}
```

- 返回 -1 表示"动物不喜欢"，不消耗物品，不增加好感，不更新 last_feed_date
- 返回 3 或 5 时消耗物品，增加好感，更新 last_feed_date

### 3.3 投喂流程

```
POST /animal/{animalId}/feed
  body: { itemId: "hazelnut" }
  ↓
1. 检查动物配置存在且 enabled
2. 获取 player_animal 记录
3. 检查今天是否已领取礼物 → 未领取则返回错误"请先领取今日礼物"
4. 检查今天是否已投喂
5. 检查背包是否有该物品
6. 代码判断 preference_level
7. 如果 preference_level == -1（不喜欢）：
   - 不消耗物品，不增加好感，不更新 last_feed_date
   - 返回 "{name}不喜欢这个食物。"
8. 计算好感值增量: 5(最爱) / 3(喜欢)
9. 背包扣除该物品 ×1
10. favorability = effectiveFav + favorGain，调用 syncLevel(pa, effectiveFav + favorGain, today)
11. 更新 last_feed_date = today
12. 返回投喂结果
```

### 3.4 动物反应台词

| 偏好 | 反应 |
|------|------|
| 最爱(+5) | "{name}开心地接过食物，眼睛亮了起来！" |
| 喜欢(+3) | "{name}愉快地吃掉了食物。" |
| 不喜欢 | "{name}不喜欢这个食物。" |

---

## 4. 等级奖励系统

### 4.1 奖励规则

- 每 2 级触发一次奖励（Lv2/4/6/8/10/12/14/16/18/20）
- **食材奖励**：动物当日产出的物品，按轮换给
- **作物种子奖励**（CROP_UNLOCK）：授予永久作物种植权（幂等）
- **花种子奖励**（FLOWER_UNLOCK）：授予永久花卉种植权（幂等）
- **配方奖励**（RECIPE）：调用 `playerRecipeService.grantPermanent()` 赠送配方（幂等）
- **奖励交替规则**：
  - 松鼠/刺猬（无种子）：Lv2-8 全为食材，Lv10-20 食材↔配方交替
  - 知更鸟/狐狸（有种子）：Lv2-8 全为食材，Lv10-20 种子↔配方交替，不再给食材

### 4.2 小松鼠奖励表（食材↔配方交替）

| Lv | 奖励 |
|----|------|
| 2 | 食材×5 |
| 4 | 食材×5 |
| 6 | 食材×10 |
| 8 | 食材×10 |
| 10 | 配方：松子蜂蜜蛋糕（100金/20exp） |
| 12 | 食材×15 |
| 14 | 配方：栗子蒙布朗（140金/28exp） |
| 16 | 食材×15 |
| 18 | 配方：鸡油菌榛果布丁（160金/35exp） |
| 20 | 食材×25 + 每日3钻石 |

### 4.3 刺猬奖励表（食材↔配方交替）

| Lv | 奖励 |
|----|------|
| 2 | 食材×5 |
| 4 | 食材×5 |
| 6 | 食材×10 |
| 8 | 食材×10 |
| 10 | 配方：香菇牛奶布丁（90金/18exp） |
| 12 | 食材×15 |
| 14 | 配方：口蘑奶酪挞（110金/22exp） |
| 16 | 食材×15 |
| 18 | 配方：桑葙金盏花戚风（280金/60exp） |
| 20 | 食材×25 + 每日3钻石 |

### 4.4 知更鸟奖励表（食材→作物种子↔配方交替）

| Lv | 奖励 |
|----|------|
| 2 | 食材×5 |
| 4 | 食材×5 |
| 6 | 食材×10 |
| 8 | 食材×10 |
| 10 | 树莓作物种子（永久作物种植权） |
| 12 | 配方：树莓酸奶慕斯（90金/18exp） |
| 14 | 蔓越莓作物种子（永久作物种植权） |
| 16 | 配方：蔓越莓松子能量球（120金/25exp） |
| 18 | 桑葙作物种子（永久作物种植权） |
| 20 | 配方：桑葙果酱蛋糕（150金/30exp） + 每日3钻石 |

### 4.5 小狐狸奖励表（食材→花种子↔配方交替）

| Lv | 奖励 |
|----|------|
| 2 | 食材×5 |
| 4 | 食材×5 |
| 6 | 食材×10 |
| 8 | 食材×10 |
| 10 | 蒲公英花种子（永久花卉种植权） |
| 12 | 配方：蒲公英蜂蜜茶（70金/15exp） |
| 14 | 紫罗兰花种子（永久花卉种植权） |
| 16 | 配方：紫罗兰蜂蜜茶（80金/16exp） |
| 18 | 迷迭香花种子（永久花卉种植权） |
| 20 | 配方：树莓迷迭香马卡龙（220金/45exp） + 每日3钻石 |

### 4.6 奖励触发逻辑

```java
private void grantAnimalReward(Long playerId, String animalId, int level, LocalDate today) {
    // 防重：检查是否已领取过该等级奖励
    if (rewardClaimedMapper.exists(playerId, animalId, level)) {
        return; // 已领取，跳过
    }

    AnimalLevelReward reward = animalLevelRewardMapper.findByAnimalAndLevel(animalId, level);
    if (reward == null) return;

    AnimalConfig animalConfig = animalConfigMapper.selectById(animalId);

    switch (reward.getRewardType()) {
        case "FOOD" -> {
            // 给动物当日产出的食材
            String itemId = determineGiftItem(animalConfig, today);
            playerInventoryService.addItem(playerId, itemId, reward.getQuantity());
        }
        case "CROP_UNLOCK" -> {
            // 授予作物永久种植权（幂等）
            playerCropService.grantCropRight(playerId, reward.getRefId());
        }
        case "FLOWER_UNLOCK" -> {
            // 授予花卉永久种植权（幂等）
            playerFlowerRightService.grantRight(playerId, reward.getRefId(), "ANIMAL_FAVOR");
        }
        case "RECIPE" -> {
            // 赠送配方（幂等）
            playerRecipeService.grantPermanent(playerId, reward.getRefId(), "ANIMAL_FAVOR");
        }
    }

    // 记录已领取
    rewardClaimedMapper.insert(playerId, animalId, level);
}
```

---

## 5. 动物配方（12 个）

> **设计决策**：不再使用多动物联动条件。每只动物单独升级到指定等级即可解锁配方。`obtain_channel = 'ANIMAL_FAVOR'`。

### 5.1 配方清单

| 动物 | 等级 | 配方名 | 配方ID | 工作台 | 材料 | sale_gold | sale_exp | sell_price | recipe_category |
|------|------|--------|--------|--------|------|-----------|----------|------------|----------------|
| 松鼠 | Lv10 | 松子蜂蜜蛋糕 | `pine_nut_honey_cake` | cake_shop | 松子×2+蜂蜜×1+小麦×1+鸡蛋×1 | 100 | 20 | 40 | CAKE |
| 松鼠 | Lv14 | 栗子蒙布朗 | `chestnut_mont_blanc` | cake_shop | 栗子×3+牛奶×1+鸡蛋×1 | 140 | 28 | 56 | CAKE |
| 松鼠 | Lv18 | 鸡油菌榛果布丁 | `chanterelle_hazelnut_pudding` | cake_shop | 鸡油菌×1+榛果×2+牛奶×1+鸡蛋×1 | 160 | 35 | 64 | CAKE |
| 刺猬 | Lv10 | 香菇牛奶布丁 | `shiitake_milk_pudding` | drink_bar | 香菇×1+牛奶×2+蜂蜜×1 | 90 | 18 | 36 | DRINK |
| 刺猬 | Lv14 | 口蘑奶酪挞 | `mushroom_cheese_tart` | cake_shop | 口蘑×2+牛奶×1+小麦×1+鸡蛋×1 | 110 | 22 | 44 | CAKE |
| 刺猬 | Lv18 | 桑葙金盏花戚风 | `mulberry_marigold_chiffon` | cake_shop | 桑葙×2+金盏花×1+香菇×1+小麦×2 | 280 | 60 | 112 | CAKE |
| 知更鸟 | Lv12 | 树莓酸奶慕斯 | `raspberry_yogurt_mousse` | drink_bar | 树莓×2+牛奶×1+蜂蜜×1 | 90 | 18 | 36 | DRINK |
| 知更鸟 | Lv16 | 蔓越莓松子能量球 | `cranberry_pine_ball` | cake_shop | 蔓越莓×2+松子×1+蜂蜜×1+小麦×1 | 120 | 25 | 48 | CAKE |
| 知更鸟 | Lv20 | 桑葙果酱蛋糕 | `mulberry_jam_cake` | cake_shop | 桑葙×2+小麦×2+鸡蛋×1 | 150 | 30 | 60 | CAKE |
| 狐狸 | Lv12 | 蒲公英蜂蜜茶 | `dandelion_honey_tea` | drink_bar | 蒲公英×2+蜂蜜×1 | 70 | 15 | 28 | DRINK |
| 狐狸 | Lv16 | 紫罗兰蜂蜜茶 | `violet_honey_tea` | drink_bar | 紫罗兰×2+蜂蜜×1+牛奶×1 | 80 | 16 | 32 | DRINK |
| 狐狸 | Lv20 | 树莓迷迭香马卡龙 | `raspberry_rosemary_macaron` | cake_shop | 树莓×2+迷迭香×1+鸡蛋×1+小麦×1 | 220 | 45 | 88 | CAKE |

> **sell_price = sale_gold × 0.4**，与现有配方比例一致。

---

## 6. 新增食材与作物

### 6.1 新增 item_config 条目

| item_id | 名称 | 类型 | 售价 | obtain_type | obtain_ref_id | 来源说明 |
|---------|------|------|------|------------|---------------|---------|
| `hazelnut` | 榛果 | MATERIAL | 5 | ANIMAL_GIFT | squirrel | 松鼠每日礼物 |
| `pine_nut` | 松子 | MATERIAL | 5 | ANIMAL_GIFT | squirrel | 松鼠每日礼物 |
| `cranberry` | 蔓越莓 | CROP | 5 | ANIMAL_FAVOR | robin | 知更鸟好感度Lv14奖励种子 |
| `raspberry` | 树莓 | CROP | 4 | ANIMAL_FAVOR | robin | 知更鸟好感度Lv10奖励种子 |
| `mulberry` | 桑葙 | CROP | 4 | ANIMAL_FAVOR | robin | 知更鸟好感度Lv18奖励种子 |
| `violet` | 紫罗兰 | FLOWER | 6 | ANIMAL_FAVOR | fox | 狐狸好感度Lv14奖励种子 |
| `dandelion` | 蒲公英 | FLOWER | 4 | ANIMAL_FAVOR | fox | 狐狸好感度Lv10奖励种子 |
| `marigold` | 金盏花 | FLOWER | 7 | FLOWER_SHOP | marigold | 花种子商店(400金) |
| `rosemary` | 迷迭香 | FLOWER | 8 | ANIMAL_FAVOR | fox | 狐狸好感度Lv18奖励种子 |
| `grape` | 葡萄 | CROP | 8 | SEED_SHOP | grape | 种子商店购买 |
| `peach` | 桃子 | CROP | 10 | SEED_SHOP | peach | 种子商店购买 |
| `vanilla` | 香草 | CROP | 12 | SEED_SHOP | vanilla | 种子商店购买 |
| `jasmine` | 茉莉花 | FLOWER | 6 | FLOWER_SHOP | jasmine | 花种子商店购买 |

> **已移除**：`truffle`（松露）、`thyme`（百里香）——从 item_config 中删除。

### 6.2 已存在食材的 obtain_type 补充

| item_id | 名称 | obtain_type | obtain_ref_id | 展示文本 |
|---------|------|------------|---------------|---------|
| `chestnut` | 栗子 | ANIMAL_GIFT | squirrel | 松鼠每日礼物 |
| `mushroom` | 口蘑 | ANIMAL_GIFT | hedgehog | 刺猬每日礼物 |
| `shiitake` | 香菇 | ANIMAL_GIFT | hedgehog | 刺猬每日礼物 |
| `chanterelle` | 鸡油菌 | ANIMAL_GIFT | hedgehog | 刺猬每日礼物 |
| `milk` | 牛奶 | BARN | barn | 牛棚产出（岛屿Lv5开启） |
| `egg` | 鸡蛋 | COOP | coop | 鸡舍产出（岛屿Lv8开启） |
| `honey` | 蜂蜜 | BEEHIVE | null | 蜂箱产出 |
| `wheat` | 小麦 | CROP | wheat | 种植获得（Lv8解锁） |
| `peppermint` | 薄荷 | FLOWER_SHOP | peppermint | 花种子商店（200金） |
| `lavender` | 薰衣草 | FLOWER_SHOP | lavender | 花种子商店（300金） |
| `chamomile` | 洋甘菊 | FLOWER_SHOP | chamomile | 花种子商店（200金） |

### 6.3 新增作物（crop_level_config）

以下作物需要新增 10 级种植配置（种在普通土地上）：

| 作物 | 售价 | Lv1生长 | Lv1产量 | Lv1经验 | 种子来源 |
|------|------|---------|---------|---------|---------|
| 树莓 raspberry | 4金 | 120s | 3 | 1 | 知更鸟 Lv10 奖励 |
| 桑葙 mulberry | 4金 | 150s | 2 | 1 | 知更鸟 Lv18 奖励 |
| 蔓越莓 cranberry | 5金 | 150s | 2 | 1 | 知更鸟 Lv14 奖励 |
| 葡萄 grape | 8金 | 300s | 3 | 2 | 种子商店 500金 |
| 桃子 peach | 10金 | 360s | 2 | 3 | 种子商店 600金 |
| 香草 vanilla | 12金 | 480s | 1 | 2 | 种子商店 800金 |

> 10 级递进规则（统一）：生长时间每级 -5%（Lv10 约为 Lv1 的 55%），产量每 3 级 +1（Lv1/4/7/10），经验每 5 级 +1。

### 6.4 新增花卉（flower_config + flower_level_config）

以下花卉需要新增配置（种在花田里）：

| 花 | 售价 | Lv1生长 | Lv1产量 | 种子来源 |
|----|------|---------|---------|---------|
| 蒲公英 dandelion | 4金 | 90s | 4 | 狐狸 Lv10 奖励 |
| 薄荷 peppermint | 5金 | 120s | 3 | 花种子商店 200金 |
| 茉莉花 jasmine | 6金 | 240s | 2 | 花种子商店 300金 |
| 紫罗兰 violet | 6金 | 200s | 2 | 狐狸 Lv14 奖励 |
| 金盏花 marigold | 7金 | 180s | 2 | 花种子商店 400金 |
| 迷迭香 rosemary | 8金 | 300s | 2 | 狐狸 Lv18 奖励 |

> 花卉 10 级递进规则同作物：生长每级 -5%，产量每 3 级 +1。

---

## 7. 统一商店

> **设计决策**：将配方商店、花种子、果蔬种子合并为一个商店，分 3 个 Tab 展示。

### 7.1 商店结构

```
┌──────────────────────────────────────────────┐
│  商店                                         │
├──────────────────────────────────────────────┤
│  [ 配方 ]  [ 花种子 ]  [ 果蔬种子 ]            │
├──────────────────────────────────────────────┤
│  （当前 Tab 的商品列表）                        │
└──────────────────────────────────────────────┘
```

### 7.2 配方 Tab

原有 6 个菌菇配方 + 新增 8 个独家作物配方：

**原有菌菇配方（已实现）：**

| 配方 | 售价 | 材料 |
|------|------|------|
| 口蘑茶 mushroom_tea | 200金 | 香菇×2+薄荷×1 |
| 口蘑奶昔 mushroom_milkshake | 300金 | 口蘑×1+牛奶×1 |
| 鸡油菌汤 chanterelle_soup | 600金 | 鸡油菌×1+牛奶×1 |
| 口蘑派 mushroom_pie | 400金 | 口蘑×2+小麦×1 |
| 香菇包 shiitake_bun | 500金 | 香菇×2+小麦×1 |
| 鸡油菌挞 chanterelle_tart | 800金 | 鸡油菌×1+小麦×2 |

**新增独家作物配方：**

| 配方 | 配方ID | 工作台 | 材料 | 售价 | 经验 | 类别 |
|------|--------|--------|------|------|------|------|
| 葡萄果汁 | grape_juice | drink_bar | 葡萄×2 | 40 | 10 | DRINK |
| 蜜桃乌龙茶 | peach_oolong_tea | drink_bar | 桃子×1+蜂蜜×1 | 55 | 12 | DRINK |
| 香草拿铁 | vanilla_latte | drink_bar | 香草×1+牛奶×1+蜂蜜×1 | 50 | 12 | DRINK |
| 茉莉花茶 | jasmine_tea | drink_bar | 茉莉花×2+蜂蜜×1 | 45 | 10 | DRINK |
| 葡萄奶酪蛋糕 | grape_cheese_cake | cake_shop | 葡萄×2+小麦×1+鸡蛋×1 | 80 | 16 | CAKE |
| 蜜桃戚风蛋糕 | peach_chiffon_cake | cake_shop | 桃子×2+小麦×2+鸡蛋×1 | 90 | 18 | CAKE |
| 香草奶油蛋糕 | vanilla_cream_cake | cake_shop | 香草×1+小麦×2+牛奶×1+鸡蛋×1 | 100 | 20 | CAKE |
| 茉莉奶绿 | jasmine_milk_tea | drink_bar | 茉莉花×1+牛奶×1 | 50 | 12 | DRINK |

### 7.3 花种子 Tab

| 种子 | 价格 | 货币 | 说明 |
|------|------|------|------|
| 薰衣草 | 300金 | GOLD | 从 flower_config 迁移 |
| 洋甘菊 | 200金 | GOLD | 从 flower_config 迁移 |
| 薄荷 | 200金 | GOLD | 新增 |
| 樱花 | 10钻 | DIAMOND | 从 flower_config 迁移 |
| 金盏花 | 400金 | GOLD | 新增（狐狸每日礼物+商店购买） |
| 茉莉花 | 300金 | GOLD | 新增独家 |

### 7.4 果蔬种子 Tab

| 种子 | 价格 | 作物售价 | 说明 |
|------|------|---------|------|
| 葡萄种子 | 500金 | 8金 | 新增独家 |
| 桃子种子 | 600金 | 10金 | 新增独家 |
| 香草种子 | 800金 | 12金 | 新增独家 |

> **不进商店的物品**：坚果（榛果/松子/栗子）和菌菇（口蘑/香菇/鸡油菌）只能通过动物每日礼物获得。

### 7.5 商店 API

新增 `ShopController`，废弃原有的 `RecipeShopController`（购买端点）和 `FlowerController`（购买端点）。`FlowerController` 的升级功能保留。

> **flower_config 字段说明**：购买价格（`seed_price`/`currency_type`）迁移到 `seed_shop_config`，`flower_config` 保留升级相关字段（`upgrade_price`/`upgrade_effect` 等）供 `FlowerController` 升级端点使用。新增花卉（蒲公英/紫罗兰/迷迭香/金盏花/茉莉花）也需在 `flower_config` 中创建记录并配置升级字段。

**列表端点**：`GET /shop/list`

一次返回全部 3 个 Tab 的数据，前端切换 Tab 不用再请求。

```json
{
  "code": 0,
  "data": {
    "recipes": [
      { "recipeId": "mushroom_tea", "name": "口蘑茶", "price": 200, "purchased": false }
    ],
    "flowerSeeds": [
      { "seedId": "lavender_seed", "name": "薰衣草种子", "price": 300, "currency": "GOLD" }
    ],
    "cropSeeds": [
      { "seedId": "grape_seed", "name": "葡萄种子", "price": 500, "currency": "GOLD" }
    ]
  }
}
```

**购买端点**：`POST /shop/buy`

```json
// Request
{ "type": "RECIPE", "itemId": "mushroom_tea" }
// 或
{ "type": "FLOWER_SEED", "itemId": "lavender_seed" }
// 或
{ "type": "CROP_SEED", "itemId": "grape_seed" }

// Response
{ "code": 0, "data": { "success": true, "message": "购买成功" } }
```

> 购买花种子/果蔬种子后自动授予种植权（不消耗背包格子）。
>
> **授权流程**：
> - 花种子：ShopService 扣钱 → 调 `playerFlowerRightService.grantRight(playerId, flowerId, "SHOP")`（新增方法，不扣钱，与现有 `purchase()` 对称）
> - 果蔬种子：ShopService 扣钱 → 调 `playerCropService.grantPermanent(playerId, cropId, "SHOP")`（已有方法）
> - 配方：ShopService 扣钱 → 调 `playerRecipeService.grantPermanent(playerId, recipeId, "SHOP")`（已有方法）

---

## 8. 配方图谱

> **设计决策**：配方图谱展示游戏中所有配方，已获得的正常显示，未获得的灰色显示。已获得在前，未获得在后。分 3 个 Tab：饮品 / 冰淇淋 / 蛋糕。

### 8.1 API 设计

**端点：** `GET /recipe/gallery`

**无需参数**，一次返回全部 3 个分类。

**响应结构：**

```json
{
  "code": 0,
  "data": {
    "drinks": [
      {
        "recipeId": "strawberry_juice",
        "recipeName": "草莓汁",
        "saleGold": 25,
        "saleExp": 5,
        "owned": true,
        "obtainDesc": "岛屿Lv1解锁",
        "materials": [
          {
            "itemId": "strawberry",
            "itemName": "草莓",
            "quantity": 2,
            "obtainDesc": "种植获得（Lv1解锁）"
          }
        ]
      }
    ],
    "iceCreams": [ ... ],
    "cakes": [ ... ]
  }
}
```

### 8.2 后端处理逻辑

1. 查所有 enabled 的 recipe_config，按 recipe_category 分组
2. 查 player_recipe WHERE player_id = ? AND channel = 'PERMANENT'，得到已拥有配方 ID 集合
3. 组装配方获取途径描述
4. 组装食材获取途径描述
5. 排序：已获得在前，未获得在后，同组内按 saleGold 升序

### 8.3 配方获取途径映射

| obtain_channel | 查询逻辑 | 展示文本 |
|---------------|---------|---------|
| ISLAND_LEVEL | 查 island_level_config WHERE recipe_id = ? | 岛屿LvX解锁 |
| RECIPE_SHOP | 查 recipe_shop_config WHERE recipe_id = ? | 配方商店（XXX金） |
| ANIMAL_FAVOR | 查 animal_level_reward WHERE ref_id = ? | XX好感度LvX奖励 |

### 8.4 食材获取途径映射

> **obtain_type 语义**：表示**种子来源**（如何获得种植权），不是作物来源（如何获得收获物）。不可种植的物品（坚果/菌菇）用 ANIMAL_GIFT，可种植的用 ANIMAL_FAVOR/SEED_SHOP/FLOWER_SHOP。

| obtain_type | 查询逻辑 | 展示文本 |
|------------|---------|---------|
| CROP | 查 island_level_config WHERE crop_id = ? | 种植获得（LvX解锁） |
| ANIMAL_GIFT | obtain_ref_id = animal_id | XX每日礼物（不可种植） |
| ANIMAL_FAVOR | 查 animal_level_reward WHERE ref_id = ? | XX好感度LvX奖励（可种植） |
| BARN | 查 barn_config | 牛棚产出（岛屿LvX开启） |
| COOP | 查 coop_config | 鸡舍产出（岛屿LvX开启） |
| BEEHIVE | — | 蜂箱产出 |
| FLOWER_SHOP | 查 seed_shop_config WHERE seed_type='FLOWER' | 花种子商店（XXX金） |
| SEED_SHOP | 查 seed_shop_config WHERE seed_type='CROP' | 果蔬种子商店（XXX金） |

### 8.5 recipe_category 字段

在 `recipe_config` 表新增 `recipe_category` 字段（VARCHAR(32)）：

| 值 | 说明 | 包含配方 |
|----|------|---------|
| DRINK | 饮品 | 所有 drink_bar 且非冰淇淋 |
| ICE_CREAM | 冰淇淋 | milk_ice_cream / watermelon_milk_ice_cream / lemon_milk_ice_cream |
| CAKE | 蛋糕 | 所有 cake_shop |

### 8.6 现有配方 recipe_category 映射（UPDATE）

> 开发时按此表批量 UPDATE 现有 recipe_config。已删除的 truffle_cocoa / truffle_cake 不在此表中。

**DRINK（10 个）**：

| recipe_id | 名称 |
|-----------|------|
| strawberry_juice | 草莓汁 |
| carrot_juice | 胡萝卜汁 |
| orange_juice | 橙汁 |
| tomato_juice | 番茄汁 |
| blueberry_juice | 蓝莓汁 |
| apple_carrot_juice | 苹果胡萝卜汁 |
| cucumber_apple_juice | 黄瓜苹果汁 |
| mushroom_tea | 蘑菇茶 |
| mushroom_milkshake | 口蘑奶昔 |
| chanterelle_soup | 鸡油菌浓汤 |

**ICE_CREAM（3 个）**：

| recipe_id | 名称 |
|-----------|------|
| milk_ice_cream | 牛奶冰淇淋 |
| watermelon_milk_ice_cream | 西瓜牛奶冰淇淋 |
| lemon_milk_ice_cream | 柠檬牛奶冰淇淋 |

**CAKE（16 个）**：

| recipe_id | 名称 |
|-----------|------|
| strawberry_cake | 草莓蛋糕 |
| carrot_cake | 胡萝卜蛋糕 |
| apple_cake | 苹果蛋糕 |
| blueberry_cake | 蓝莓蛋糕 |
| lemon_cake | 柠檬蛋糕 |
| rose_cake | 玫瑰蛋糕 |
| chrysanthemum_cake | 菊花酥 |
| jasmine_mousse | 茉莉慕斯 |
| osmanthus_cake | 桂花糕 |
| lavender_macaron | 薰衣草马卡龙 |
| hibiscus_jelly | 洛神花果冻 |
| sakura_cake | 樱花蛋糕 |
| chamomile_cookie | 洋甘菊饼干 |
| mushroom_pie | 蘑菇咸派 |
| shiitake_bun | 香菇芝士包 |
| chanterelle_tart | 鸡油菌塔 |

> **新增配方**（12 动物配方 + 8 商店配方）在 INSERT 时直接写入 recipe_category，无需 UPDATE。

---

## 9. 数据库设计

### 9.1 新增表

#### 9.1.1 `animal_config` — 动物配置表

```sql
CREATE TABLE animal_config (
    id              VARCHAR(32)  PRIMARY KEY,
    name            VARCHAR(32)  NOT NULL,
    habitat_x       INT          NOT NULL,
    habitat_y       INT          NOT NULL,
    unlock_level    INT          NOT NULL DEFAULT 1,  -- 岛屿等级解锁门槛
    gift_items      VARCHAR(256) NOT NULL,
    gift_mode       VARCHAR(16)  NOT NULL DEFAULT 'ROTATION',
    seasonal_bonus  VARCHAR(16),
    sort_order      INT          DEFAULT 0,
    enabled         INT          NOT NULL DEFAULT 1,
    description     VARCHAR(256)
);
```

#### 9.1.2 `player_animal` — 玩家-动物关系表

```sql
CREATE TABLE player_animal (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id               BIGINT      NOT NULL,
    animal_id               VARCHAR(32) NOT NULL,
    favorability            INT         DEFAULT 0,
    level                   INT         DEFAULT 1,
    last_gift_date          DATE,
    last_feed_date          DATE,
    UNIQUE (player_id, animal_id)
);
```

#### 9.1.3 `animal_level_reward` — 动物等级奖励配置表

```sql
CREATE TABLE animal_level_reward (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    animal_id     VARCHAR(32) NOT NULL,
    level         INT         NOT NULL,          -- 触发等级（偶数：2/4/6/.../20）
    reward_type   VARCHAR(32) NOT NULL,          -- FOOD / CROP_UNLOCK / FLOWER_UNLOCK / RECIPE
    ref_id        VARCHAR(64),                    -- CROP_UNLOCK=crop_id, FLOWER_UNLOCK=flower_id, RECIPE=recipe_id
    quantity      INT         DEFAULT 0,          -- FOOD 时为食材数量
    UNIQUE (animal_id, level)
);
```

#### 9.1.4 `player_animal_reward_claimed` — 奖励领取记录表

> 防止降级后再升级重复触发食材奖励。配方和种植权本身幂等，但食材奖励需要此表保证只发一次。

```sql
CREATE TABLE player_animal_reward_claimed (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id   BIGINT      NOT NULL,
    animal_id   VARCHAR(32) NOT NULL,
    level       INT         NOT NULL,
    claimed_at  DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (player_id, animal_id, level)
);
```

#### 9.1.5 `seed_shop_config` — 种子商店配置表

> 统一管理花种子和果蔬种子的商店数据。花种子从 flower_config 迁移过来。

```sql
CREATE TABLE seed_shop_config (
    id          VARCHAR(64) PRIMARY KEY,         -- 种子ID（如 grape_seed）
    item_id     VARCHAR(64) NOT NULL,            -- 种植后产出的作物 item_id
    name        VARCHAR(64) NOT NULL,            -- 种子名称
    price       INT         NOT NULL,
    currency    VARCHAR(16) NOT NULL DEFAULT 'GOLD',  -- GOLD / DIAMOND
    seed_type   VARCHAR(16) NOT NULL,            -- FLOWER / CROP
    sort_order  INT         DEFAULT 0,
    enabled     INT         NOT NULL DEFAULT 1
);
```

### 9.2 种子数据

#### 9.2.1 animal_config

```sql
INSERT INTO animal_config (id, name, habitat_x, habitat_y, unlock_level, gift_items, gift_mode, seasonal_bonus, sort_order, enabled, description) VALUES
('squirrel', '小松鼠', 4,  8,  3,  'hazelnut,pine_nut,chestnut',          'ROTATION', 'AUTUMN', 1, 1, '果园旁树洞，活泼好动'),
('robin',    '知更鸟', 6,  5,  5,  'cranberry,raspberry,mulberry',       'ROTATION', 'SUMMER', 2, 1, '饮品店屋檐，警觉爱唱歌'),
('hedgehog', '刺猬',   27, 10, 8,  'mushroom,shiitake,chanterelle',      'ROTATION', 'AUTUMN', 3, 1, '花园灌木下，胆小慢热'),
('fox',      '小狐狸', 15, 5,  10, 'violet,dandelion,marigold,rosemary', 'RANDOM',   NULL,     4, 1, '密林边缘，狡黠高冷');
```

#### 9.2.2 seed_shop_config

```sql
INSERT INTO seed_shop_config (id, item_id, name, price, currency, seed_type, sort_order, enabled) VALUES
('lavender_seed',    'lavender',    '薰衣草种子', 300, 'GOLD',    'FLOWER', 1, 1),
('chamomile_seed',   'chamomile',   '洋甘菊种子', 200, 'GOLD',    'FLOWER', 2, 1),
('peppermint_seed',  'peppermint',  '薄荷种子',   200, 'GOLD',    'FLOWER', 3, 1),
('sakura_seed',      'sakura',      '樱花种子',   10,  'DIAMOND', 'FLOWER', 4, 1),
('marigold_seed',    'marigold',    '金盏花种子', 400, 'GOLD',    'FLOWER', 5, 1),
('jasmine_seed',     'jasmine',     '茉莉花种子', 300, 'GOLD',    'FLOWER', 6, 1),
('grape_seed',       'grape',       '葡萄种子',   500, 'GOLD',    'CROP',   1, 1),
('peach_seed',       'peach',       '桃子种子',   600, 'GOLD',    'CROP',   2, 1),
('vanilla_seed',     'vanilla',     '香草种子',   800, 'GOLD',    'CROP',   3, 1);
```

#### 9.2.3 animal_level_reward

```sql
-- 小松鼠（食材↔配方交替）
INSERT INTO animal_level_reward (animal_id, level, reward_type, ref_id, quantity) VALUES
('squirrel', 2,  'FOOD', NULL, 5),
('squirrel', 4,  'FOOD', NULL, 5),
('squirrel', 6,  'FOOD', NULL, 10),
('squirrel', 8,  'FOOD', NULL, 10),
('squirrel', 10, 'RECIPE', 'pine_nut_honey_cake', 0),
('squirrel', 12, 'FOOD', NULL, 15),
('squirrel', 14, 'RECIPE', 'chestnut_mont_blanc', 0),
('squirrel', 16, 'FOOD', NULL, 15),
('squirrel', 18, 'RECIPE', 'chanterelle_hazelnut_pudding', 0),
('squirrel', 20, 'FOOD', NULL, 25);

-- 刺猬（食材↔配方交替）
INSERT INTO animal_level_reward (animal_id, level, reward_type, ref_id, quantity) VALUES
('hedgehog', 2,  'FOOD', NULL, 5),
('hedgehog', 4,  'FOOD', NULL, 5),
('hedgehog', 6,  'FOOD', NULL, 10),
('hedgehog', 8,  'FOOD', NULL, 10),
('hedgehog', 10, 'RECIPE', 'shiitake_milk_pudding', 0),
('hedgehog', 12, 'FOOD', NULL, 15),
('hedgehog', 14, 'RECIPE', 'mushroom_cheese_tart', 0),
('hedgehog', 16, 'FOOD', NULL, 15),
('hedgehog', 18, 'RECIPE', 'mulberry_marigold_chiffon', 0),
('hedgehog', 20, 'FOOD', NULL, 25);

-- 知更鸟（食材→作物种子↔配方交替）
INSERT INTO animal_level_reward (animal_id, level, reward_type, ref_id, quantity) VALUES
('robin', 2,  'FOOD', NULL, 5),
('robin', 4,  'FOOD', NULL, 5),
('robin', 6,  'FOOD', NULL, 10),
('robin', 8,  'FOOD', NULL, 10),
('robin', 10, 'CROP_UNLOCK', 'raspberry', 0),
('robin', 12, 'RECIPE', 'raspberry_yogurt_mousse', 0),
('robin', 14, 'CROP_UNLOCK', 'cranberry', 0),
('robin', 16, 'RECIPE', 'cranberry_pine_ball', 0),
('robin', 18, 'CROP_UNLOCK', 'mulberry', 0),
('robin', 20, 'RECIPE', 'mulberry_jam_cake', 0);

-- 小狐狸（食材→花种子↔配方交替）
INSERT INTO animal_level_reward (animal_id, level, reward_type, ref_id, quantity) VALUES
('fox', 2,  'FOOD', NULL, 5),
('fox', 4,  'FOOD', NULL, 5),
('fox', 6,  'FOOD', NULL, 10),
('fox', 8,  'FOOD', NULL, 10),
('fox', 10, 'FLOWER_UNLOCK', 'dandelion', 0),
('fox', 12, 'RECIPE', 'dandelion_honey_tea', 0),
('fox', 14, 'FLOWER_UNLOCK', 'violet', 0),
('fox', 16, 'RECIPE', 'violet_honey_tea', 0),
('fox', 18, 'FLOWER_UNLOCK', 'rosemary', 0),
('fox', 20, 'RECIPE', 'raspberry_rosemary_macaron', 0);
```

### 9.3 现有表修改

#### 9.3.1 item_config 新增字段

```sql
ALTER TABLE item_config ADD COLUMN obtain_type VARCHAR(32);
ALTER TABLE item_config ADD COLUMN obtain_ref_id VARCHAR(64);
```

#### 9.3.2 recipe_config 新增字段

```sql
ALTER TABLE recipe_config ADD COLUMN recipe_category VARCHAR(32);
```

#### 9.3.3 移除松露相关数据

```sql
DELETE FROM item_config WHERE id = 'truffle';
DELETE FROM recipe_config WHERE id IN ('truffle_cocoa', 'truffle_cake');
DELETE FROM recipe_material WHERE recipe_id IN ('truffle_cocoa', 'truffle_cake');
DELETE FROM recipe_shop_config WHERE recipe_id IN ('truffle_cocoa', 'truffle_cake');
```

#### 9.3.4 移除百里香

```sql
DELETE FROM item_config WHERE id = 'thyme';
```

### 9.4 gift_mode 说明

- `ROTATION`：按天轮换。`gift_items[dayOfYear % len]`
- `SEASONAL`：按季节切换（当前无动物使用，代码保留）
- `RANDOM`：确定性随机。`items[(dayOfYear * 31 + animalId.hashCode()) % items.length]`

---

## 10. 每日礼物产出逻辑

### 10.1 礼物物品计算

```java
private String determineGiftItem(AnimalConfig animal, LocalDate today) {
    String[] items = animal.getGiftItems().split(",");
    switch (animal.getGiftMode()) {
        case "ROTATION":
            int dayOfYear = today.getDayOfYear();
            return items[dayOfYear % items.length];
        case "SEASONAL":
            int seasonIndex = getSeasonIndex(today); // 春=0, 夏=1, 秋=2, 冬=3
            return items[Math.min(seasonIndex, items.length - 1)];
        case "RANDOM":
            int idx = Math.floorMod(today.getDayOfYear() * 31 + animal.getId().hashCode(), items.length);
            return items[idx];
        default:
            return items[0];
    }
}
```

### 10.2 礼物数量计算

```java
/**
 * 每日礼物数 = 动物等级 × 2
 * 季节加成: Lv2+ 时 bonus 季节 ×1.5
 * 等级基于 effectiveFavorability（前日结算）
 */
private int calculateGiftCount(AnimalConfig animal, int effectiveLevel, LocalDate today) {
    int baseCount = effectiveLevel * 2;  // Lv1=2, Lv20=40

    if (effectiveLevel >= 2 && animal.getSeasonalBonus() != null
        && isBonusSeason(animal.getSeasonalBonus(), today)) {
        baseCount = (int)(baseCount * 1.5);
    }

    return baseCount;
}
```

### 10.3 领取礼物流程

```
POST /animal/{animalId}/collect
  ↓
1. 检查动物配置存在且 enabled
2. 检查玩家岛屿等级 >= animal_config.unlock_level
3. 获取 player_animal 记录
4. 计算有效好感值 effectiveFav（含衰减 + 1天宽限期）
5. 检查今天是否已领取（last_gift_date == today）
6. 计算礼物物品 + 数量（基于 effectiveFav 对应的等级）
7. 物品进入背包
8. 如果 effectiveLevel == 20：额外获得 3 钻石
9. favorability = effectiveFav + 2，调用 syncLevel(pa, effectiveFav + 2, today) 同步等级
10. 更新 last_gift_date = today
11. 返回领取结果
```

---

## 11. API 设计

### 11.1 端点总览

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/animal/list` | 列出所有动物及玩家好感状态 | JWT |
| POST | `/animal/{animalId}/collect` | 领取单只动物每日礼物 | JWT |
| POST | `/animal/collect-all` | 批量领取所有可用动物礼物 | JWT |
| POST | `/animal/{animalId}/feed` | 投喂动物（需先领取礼物） | JWT |
| GET | `/recipe/gallery` | 配方图谱 | JWT |
| GET | `/shop/list` | 统一商店列表（3 Tab） | JWT |
| POST | `/shop/buy` | 统一商店购买 | JWT |

### 11.2 GET /animal/list

**响应**：
```json
{
  "code": 0,
  "data": {
    "animals": [
      {
        "animalId": "squirrel",
        "name": "小松鼠",
        "description": "果园旁树洞，活泼好动",
        "habitatX": 4,
        "habitatY": 8,
        "unlockLevel": 3,
        "locked": false,
        "favorability": 18,
        "effectiveFavorability": 16,
        "level": 4,
        "nextLevelFavorability": 22,
        "giftAvailable": true,
        "giftItem": "hazelnut",
        "giftCount": 8,
        "feedAvailable": true,
        "seasonalBonus": "AUTUMN",
        "isBonusSeason": false,
        "decayDays": 1,
        "decayAmount": 2
      }
    ]
  }
}
```

### 11.3 POST /animal/{animalId}/collect

**响应**：
```json
{
  "code": 0,
  "data": {
    "animalId": "squirrel",
    "giftItem": "hazelnut",
    "giftCount": 8,
    "favorabilityGained": 2,
    "favorability": 18,
    "level": 4,
    "levelUp": false,
    "decayApplied": 2,
    "diamondsGained": 0,
    "rewardGranted": null
  }
}
```

**新增字段**：
- `diamondsGained`：Lv20 时为 3，其他为 0
- `rewardGranted`：升级触发的奖励信息（类型 + 内容），无则为 null

### 11.4 POST /animal/collect-all

部分成功模式。单只动物操作失败不影响其他动物，失败的动物不在 results 中返回。未解锁的动物（岛屿等级不足）自动跳过，不报错也不返回。当天已领取的动物也跳过。

**响应**：
```json
{
  "code": 0,
  "data": {
    "results": [
      {
        "animalId": "squirrel",
        "giftItem": "hazelnut",
        "giftCount": 8,
        "favorabilityGained": 2,
        "favorability": 18,
        "level": 4,
        "levelUp": false,
        "decayApplied": 2,
        "diamondsGained": 0,
        "rewardGranted": null
      },
      {
        "animalId": "robin",
        "giftItem": "raspberry",
        "giftCount": 4,
        "favorabilityGained": 2,
        "favorability": 6,
        "level": 2,
        "levelUp": true,
        "decayApplied": 0,
        "diamondsGained": 0,
        "rewardGranted": { "type": "FOOD", "desc": "食材×5" }
      }
    ],
    "totalDiamonds": 0,
    "totalItems": [
      { "itemId": "hazelnut", "quantity": 8 },
      { "itemId": "raspberry", "quantity": 4 }
    ]
  }
}
```

- `results`：只包含成功领取的动物（未解锁的动物和当天已领取的动物自动跳过，不报错也不返回）
- `totalDiamonds`：所有动物钻石合计
- `totalItems`：所有动物礼物物品汇总

### 11.5 POST /animal/{animalId}/feed

**请求**：`{ "itemId": "hazelnut" }`

**响应**：
```json
{
  "code": 0,
  "data": {
    "animalId": "squirrel",
    "itemId": "hazelnut",
    "preferenceLevel": 5,
    "preferenceName": "最爱",
    "favorabilityGained": 5,
    "favorability": 23,
    "level": 5,
    "levelUp": true,
    "consumed": true,
    "reaction": "小松鼠开心地接过食物，眼睛亮了起来！",
    "decayApplied": 0,
    "rewardGranted": null
  }
}
```

如果物品不在偏好列表中：
```json
{
  "code": 0,
  "data": {
    "animalId": "squirrel",
    "itemId": "carrot",
    "preferenceLevel": -1,
    "preferenceName": "不喜欢",
    "favorabilityGained": 0,
    "consumed": false,
    "reaction": "小松鼠不喜欢这个食物。"
  }
}
```

### 11.6 错误响应汇总

所有错误统一返回 `code != 0` + `message`：

| 场景 | code | message |
|------|------|---------|
| 动物未解锁（岛屿等级不足） | 4001 | 该动物尚未解锁，需要岛屿等级LvX |
| 当天礼物已领取 | 4002 | 今日已领取过该动物的礼物 |
| 未领取礼物就投喂 | 4003 | 请先领取今日礼物后再投喂 |
| 当天已投喂 | 4004 | 今日已投喂过该动物 |
| 背包物品不足（投喂） | 4005 | 背包中没有该物品 |
| 商店金币不足 | 4006 | 金币不足 |
| 商店钻石不足 | 4007 | 钻石不足 |
| 商店已拥有（配方/种植权） | 4008 | 已拥有该物品 |
| 动物不存在或已禁用 | 4009 | 动物不存在 |

**示例**：
```json
{
  "code": 4001,
  "message": "该动物尚未解锁，需要岛屿等级Lv5"
}
```

### 11.7 GET /recipe/gallery

见第 8 节。

### 11.8 GET /shop/list

见第 7.5 节。

### 11.9 POST /shop/buy

**成功响应**：
```json
{
  "code": 0,
  "data": {
    "success": true,
    "message": "购买成功",
    "type": "RECIPE",
    "itemId": "grape_juice",
    "goldSpent": 40,
    "diamondSpent": 0
  }
}
```

**购买种子时额外返回**：
```json
{
  "code": 0,
  "data": {
    "success": true,
    "message": "购买成功，已获得种植权",
    "type": "FLOWER_SEED",
    "itemId": "marigold_seed",
    "goldSpent": 400,
    "diamondSpent": 0
  }
}
```

---

## 12. 玩家初始化

在 `GameController.gameInit()` 中，为玩家创建 `player_animal` 记录。**幂等设计**：遍历所有 enabled 动物，缺失的补建。动物有岛屿等级解锁门槛，未达标的动物不创建记录（或创建但标记 locked）。

```java
List<AnimalConfig> animals = animalConfigService.lambdaQuery().eq(AnimalConfig::getEnabled, 1).list();
int islandLevel = playerIslandLevel; // 玩家当前岛屿等级
for (AnimalConfig animal : animals) {
    if (islandLevel < animal.getUnlockLevel()) continue; // 未解锁的动物跳过
    // 检查是否已有记录，没有则创建
    PlayerAnimal existing = playerAnimalMapper.selectOne(...);
    if (existing == null) {
        PlayerAnimal pa = new PlayerAnimal();
        pa.setPlayerId(playerId);
        pa.setAnimalId(animal.getId());
        pa.setFavorability(0);
        pa.setLevel(1);
        playerAnimalMapper.insert(pa);
    }
}
```

> **注意**：玩家岛屿升级解锁新动物时，需要在 `GamePlayerServiceImpl.applyExperience()` 的升级逻辑中补建 player_animal 记录：

```java
// applyExperience() 中，岛屿等级升级后：
List<AnimalConfig> animals = animalConfigMapper.selectEnabledByUnlockLevel(newIslandLevel);
for (AnimalConfig animal : animals) {
    playerAnimalService.getOrCreate(playerId, animal.getId()); // 幂等
}
```

`animal_config.unlock_level` 字段驱动解锁，不写死在代码里。`gameInit()` 和 `applyExperience()` 两处都会检查并补建。

---

## 13. 客户端设计

### 13.1 TypeScript 类型

```typescript
interface AnimalStatus {
  animalId: string;
  name: string;
  description: string;
  habitatX: number;
  habitatY: number;
  unlockLevel: number;
  locked: boolean;
  favorability: number;
  effectiveFavorability: number;
  level: number;
  nextLevelFavorability: number;
  giftAvailable: boolean;
  giftItem: string;
  giftCount: number;
  feedAvailable: boolean;
  seasonalBonus: string | null;
  isBonusSeason: boolean;
  decayDays: number;
  decayAmount: number;
}

interface CollectResult {
  animalId: string;
  giftItem: string;
  giftCount: number;
  favorabilityGained: number;
  favorability: number;
  level: number;
  levelUp: boolean;
  decayApplied: number;
  diamondsGained: number;
  rewardGranted: { type: string; desc: string } | null;
}

interface FeedResult {
  animalId: string;
  itemId: string;
  preferenceLevel: number;
  preferenceName: string;
  favorabilityGained: number;
  favorability: number;
  level: number;
  levelUp: boolean;
  consumed: boolean;
  reaction: string;
  decayApplied: number;
  rewardGranted: { type: string; desc: string } | null;
}

interface GalleryItem {
  recipeId: string;
  recipeName: string;
  saleGold: number;
  saleExp: number;
  owned: boolean;
  obtainDesc: string;
  materials: {
    itemId: string;
    itemName: string;
    quantity: number;
    obtainDesc: string;
  }[];
}

interface GalleryVO {
  drinks: GalleryItem[];
  iceCreams: GalleryItem[];
  cakes: GalleryItem[];
}

interface ShopVO {
  recipes: { recipeId: string; name: string; price: number; purchased: boolean }[];
  flowerSeeds: { seedId: string; name: string; price: number; currency: string }[];
  cropSeeds: { seedId: string; name: string; price: number; currency: string }[];
}

interface ShopBuyResult {
  success: boolean;
  message: string;
  type: 'RECIPE' | 'FLOWER_SEED' | 'CROP_SEED';
  itemId: string;
  goldSpent: number;
  diamondSpent: number;
}

interface CollectAllResult {
  results: CollectResult[];
  totalDiamonds: number;
  totalItems: { itemId: string; quantity: number }[];
}
```

### 13.2 HTML 面板

在 `demo2.8-island.html` 新增：
- `animal-drawer`：动物好感度管理面板
- `recipe-gallery-drawer`：配方图画面板
- `shop-drawer`：统一商店面板（替代原配方商店 drawer）

---

## 14. 测试计划

### 14.1 测试用例清单

| # | 测试名 | 描述 |
|---|--------|------|
| 1 | listAnimalsReturnsAllAnimals | 列出全部 4 只动物 |
| 2 | lockedAnimalNotShown | 未达岛屿等级的动物不显示 |
| 3 | collectGiftSuccess | 领取礼物成功，物品入背包 |
| 4 | collectGiftGivesCorrectCount | 礼物数量 = 等级×2 |
| 5 | collectGiftUsesPreviousDayFavorability | 礼物数量基于前日好感（前日结算） |
| 6 | collectGiftAddsFavorability | 领取后好感+2 |
| 7 | duplicateCollectFails | 同日重复领取失败 |
| 8 | collectAllSucceeds | 批量领取所有可用动物礼物 |
| 9 | collectAllPartialSuccess | 部分成功，失败跳过 |
| 10 | feedFavoriteGivesMaxFavor | 最爱食物 +5 |
| 11 | feedLikeGivesMidFavor | 喜欢食物 +3 |
| 12 | feedDislikedDoesNotConsume | 不喜欢食物不消耗、不加好感、可再次投喂 |
| 13 | feedBeforeCollectFails | 未领取礼物时投喂返回错误 |
| 14 | duplicateFeedFails | 同日重复投喂失败 |
| 15 | favorDecaysWhenNotInteracting | 不互动天数导致好感衰减（2天起） |
| 16 | favorDecayGracePeriod | 漏1天不衰减（宽限期） |
| 17 | favorDecayFloorsAtZero | 衰减不低于 0 |
| 18 | favorDecaySyncsLevelDown | 衰减导致降级时 level 同步降低 |
| 19 | listDoesNotCountAsInteraction | 仅打开面板不阻止衰减 |
| 20 | randomGiftIsDeterministic | 狐狸随机礼物全服同日相同 |
| 21 | seasonalBonusIncreasesGift | 季节加成时礼物数量 ×1.5 |
| 22 | levelUpGrantsFoodReward | 升到偶数等级触发食材奖励 |
| 23 | levelUpGrantsCropUnlock | 升到种子等级授予种植权 |
| 24 | levelUpGrantsRecipe | 升到配方等级赠送配方 |
| 25 | cropUnlockIsIdempotent | 重复触发种植权不重复授予 |
| 26 | recipeGrantIsIdempotent | 重复触发配方赠送不重复 |
| 27 | level20GivesDiamonds | Lv20 每日领取额外 3 钻石 |
| 28 | levelBelow20NoDiamonds | 降到 Lv19 以下不获得钻石 |
| 29 | galleryReturnsAllRecipes | 图谱返回全部配方，分 3 类 |
| 30 | galleryOwnedFirst | 已获得配方排在前面 |
| 31 | galleryShowsMaterialObtainDesc | 食材获取途径正确显示 |
| 32 | unauthorizedRequestBlocked | 未认证请求被拦截 |
| 33 | shopListReturnsThreeTabs | 商店列表返回 3 个 Tab 数据 |
| 34 | shopBuyRecipeSuccess | 购买配方成功 |
| 35 | shopBuyFlowerSeedSuccess | 购买花种子成功并授予种植权 |
| 36 | shopBuyCropSeedSuccess | 购买果蔬种子成功并授予种植权 |
| 37 | rewardNotRepeatedOnRelevel | 降级再升级不重复触发食材奖励 |
| 38 | animalUnlockOnIslandLevelUp | 岛屿升级时自动解锁新动物 |

### 14.2 测试策略

- 使用 H2 内存数据库，`@BeforeEach` 重置相关表
- USER_A=8401 用于主测试，USER_B=8402 用于玩家隔离测试
- 季节加成测试：mock `Clock` 固定到特定月份
- 衰减测试：设置 `last_gift_date` 为 N 天前
- 等级奖励测试：直接设置 `favorability` 到目标阈值，验证奖励触发

---

## 15. 文件清单

### 15.1 后端新增文件

| 文件 | 说明 |
|------|------|
| `entity/AnimalConfig.java` | 动物配置 Entity（含 unlock_level） |
| `entity/PlayerAnimal.java` | 玩家-动物关系 Entity |
| `entity/AnimalLevelReward.java` | 动物等级奖励配置 Entity |
| `entity/PlayerAnimalRewardClaimed.java` | 奖励领取记录 Entity |
| `entity/SeedShopConfig.java` | 种子商店配置 Entity |
| `mapper/AnimalConfigMapper.java` | Mapper |
| `mapper/PlayerAnimalMapper.java` | Mapper |
| `mapper/AnimalLevelRewardMapper.java` | Mapper |
| `mapper/PlayerAnimalRewardClaimedMapper.java` | Mapper |
| `mapper/SeedShopConfigMapper.java` | Mapper |
| `dto/AnimalStatusVO.java` | 动物状态 VO |
| `dto/GalleryVO.java` | 配方图谱 VO |
| `dto/ShopVO.java` | 统一商店 VO |
| `service/AnimalService.java` | Service 接口 |
| `service/impl/AnimalServiceImpl.java` | Service 实现（含食物偏好硬编码） |
| `service/RecipeGalleryService.java` | 配方图谱 Service |
| `service/ShopService.java` | 统一商店 Service |
| `controller/AnimalController.java` | 动物 Controller |
| `controller/RecipeGalleryController.java` | 配方图谱 Controller |
| `controller/ShopController.java` | 统一商店 Controller |
| `test/.../AnimalHttpTest.java` | HTTP 集成测试 |

### 15.2 后端修改文件

| 文件 | 修改内容 |
|------|---------|
| `common/.../schema-h2.sql` | 新增 5 张表(animal_config/player_animal/animal_level_reward/player_animal_reward_claimed/seed_shop_config) + item_config 加 obtain_type/obtain_ref_id + recipe_config 加 recipe_category + 新增 12 个动物配方 item_config/recipe_config/recipe_material + 8 个商店配方 + 6 种新作物 crop_level_config(60行) + 6 种新花 flower_config/flower_level_config + seed_shop_config 数据 + 种子数据 + 移除 truffle/thyme |
| `game-server/.../db/schema.sql` | 同上（MySQL 语法） |
| `common/.../WebConfig.java` | JWT 拦截器添加 `/animal/**`、`/recipe/gallery`、`/shop/**` 路径 |
| `game-server/.../controller/GameController.java` | gameInit() 中初始化 player_animal 记录（含岛屿等级检查） |
| `game-server/.../service/impl/GamePlayerServiceImpl.java` | applyExperience() 岛屿升级时补建 player_animal 记录 |
| `game-server/.../service/impl/PlayerCropServiceImpl.java` | 新增 grantCropRight() 方法（或复用 grantPermanent()） |
| `game-server/.../service/PlayerFlowerRightService.java` | 新增 grantRight() 方法（不扣钱的免费授权，与 purchase() 对称） |
| 统一商店 ShopController/ShopService | 合并配方商店+花店+种子商店为统一商店，扣钱后调 grantRight/grantPermanent |

### 15.3 前端修改文件

| 文件 | 修改内容 |
|------|---------|
| `assets/scripts/types/index.ts` | 新增 AnimalStatus/GalleryVO 等类型 |
| `assets/scripts/network/Api.ts` | 新增端点常量 |
| `demo2.8-island.html` | 新增动物 drawer + 配方图谱 drawer + 统一商店 drawer |

---

## 16. 实现顺序

```
Task 1: 数据库 schema 设计（5 张新表 + 字段修改 + 种子数据）
  ↓
Task 2: Entity + Mapper 创建（含 SeedShopConfig / PlayerAnimalRewardClaimed）
  ↓
Task 3: AnimalService + Controller 实现（含奖励防重 + 岛屿解锁触发）
  ↓
Task 4: RecipeGalleryService + Controller 实现
  ↓
Task 5: 统一商店 ShopController/ShopService 实现（合并配方商店+花店+种子商店）
  ↓
Task 6: 新作物/新花种植配置（crop_level_config + flower_config/flower_level_config）
  ↓
Task 7: HTTP 集成测试编写（38 个测试用例）
  ↓
Task 8: 客户端面板开发（动物/图谱/商店）
  ↓
Task 9: MySQL 迁移 + 全量测试 + 服务器重启
```

---

## 17. 已确认设计决策

| # | 决策点 | 结论 | 理由 |
|---|--------|------|------|
| 1 | 礼物累积模型 | "不领就丢"，无存储上限 | 简单明了 |
| 2 | 季节/时区 | Asia/Shanghai + Clock 注入 | 与 SatisfactionServiceImpl 保持一致 |
| 3 | RANDOM 礼物确定性 | 全服同日同动物相同物品 | 降低对齐复杂度 |
| 4 | 好感度体系 | 20 级，公式 `3+level×1`，每日礼物 `level×2` | 11 天到 Lv10，36 天满级 |
| 5 | 好感度衰减 | 每日不互动 -2，下限 0，1天宽限期，惰性结算 | 鼓励每日互动，偶尔漏1天不惩罚 |
| 6 | 动物出现 | 所有动物每天出现 | 简化设计 |
| 7 | 动物解锁等级 | 松鼠Lv3/知更鸟Lv5/刺猬Lv8/狐狸Lv10 | 与岛屿进度同步 |
| 8 | 互动定义 | 仅 collect/feed 算互动 | 打开面板不算 |
| 9 | 前日结算 | 礼物数量基于 effectiveFavorability | 防止 exploit |
| 10 | 先领后喂 | 未领取礼物时不可投喂 | 配合前日结算 |
| 11 | 批量领取 | collect-all 部分成功模式 | 用户体验更好 |
| 12 | 等级双向同步 | syncLevel() 直接覆盖等级 | 修复衰减后等级不同步 |
| 13 | 食物偏好简化 | 代码硬编码，每只动物 2 种食物（最爱+5/喜欢+3），其他不消耗 | 4只动物偏好固定，无需配表 |
| 14 | 动物独家产出 | 坚果(松鼠)和菌菇(刺猬)只能动物带来 | 坚果无种子概念，菌菇无种子概念 |
| 15 | 知更鸟蓝莓→蔓越莓 | blueberry 替换为 cranberry | 蓝莓是可种植作物，重叠价值低 |
| 16 | 狐狸礼物更换 | 薄荷/百里香/薰衣草/洋甘菊 → 紫罗兰/蒲公英/金盏花/迷迭香 | 薰衣草/洋甘菊已有花种子商店，薄荷改花种子商店购买 |
| 17 | 松露完全移除 | 删除 truffle + truffle_cocoa + truffle_cake | 游戏主题是甜品，松露不符合定位 |
| 18 | 百里香移除 | 删除 thyme | 狐狸不再带百里香，无来源 |
| 19 | 动物配方 | 12 个，单只动物到等级即赠送，不再需要多动物联动 | 简化解锁逻辑 |
| 20 | 奖励交替 | 食材↔配方(松鼠/刺猬)，食材→种子↔配方(知更鸟/狐狸) | 食材不出现在种子/配方之后 |
| 21 | sell_price 比例 | sale_gold × 0.4 | 与现有配方一致 |
| 22 | recipe_category | 新增字段 DRINK/ICE_CREAM/CAKE | 配方图谱分 Tab 展示 |
| 23 | item_config 获取途径 | 新增 obtain_type + obtain_ref_id | 配方图谱动态展示食材来源 |
| 24 | 统一商店 | 配方/花种子/果蔬种子 3 Tab | 合并配方商店+花店+种子商店 |
| 25 | 种子商店独家作物 | 葡萄/桃子/香草（果蔬）+ 茉莉花（花） | 只能购买，不能通过其他途径获得 |
| 26 | 金盏花进商店 | 狐狸独家→花种子商店购买(400金) | 没有种子来源的鲜花放到商店 |
| 27 | 薄荷进商店 | 狐狸不再带，花种子商店购买(200金) | 保持薄荷有来源 |
| 28 | Lv20 钻石奖励 | 维持 Lv20 每日 3 钻石，降级不发 | 满级被动收入 |
| 29 | 配方图谱 | GET /recipe/gallery，3 Tab，已获得在前 | 配方图鉴功能 |
| 30 | 配方触发等级 | `newLevel % 2 == 0` 时触发奖励 | 每 2 级一次奖励 |
| 31 | **v7: 好感度公式修正** | `3+level×1`（原 `5+level×2` 数值错误） | 36 天满级，节奏合理 |
| 32 | **v7: seed_shop_config 表** | 花种子从 flower_config 迁移，新建统一种子商店表 | 花种子和果蔬种子统一管理 |
| 33 | **v7: 统一商店 API** | `GET /shop/list` + `POST /shop/buy`，废弃旧端点 | 前端一次请求获取 3 Tab 数据 |
| 34 | **v7: 作物/花卉分配** | 蒲公英/紫罗兰/迷迭香→flower_config（花田），6作物→crop_level_config | 花走花田系统 |
| 35 | **v7: 每种作物不同基础值** | 售价高的长得慢产量低，10 级递进规则统一 | 增加种植策略性 |
| 36 | **v7: 奖励防重** | 新增 player_animal_reward_claimed 表，UNIQUE 约束 | 降级再升级不重复发食材 |
| 37 | **v7: 动物解锁触发** | animal_config 加 unlock_level，applyExperience() + gameInit() 补建 | 配置驱动，不写死代码 |
| 38 | **v7: 蜂箱价格保持硬编码** | BEEHIVE_PRICES 不挪配置表，图谱显示"蜂箱产出" | 不为此单独建表 |
| 39 | **v8: obtain_type 语义** | 表示种子来源（种植权获取方式），不是作物来源 | 图谱展示如何获得种植权，不是如何获得收获物 |
| 40 | **v8: ANIMAL_FAVOR 类型** | 新增 obtain_type 值，区分"动物每日礼物"(ANIMAL_GIFT)和"动物好感度奖励种子"(ANIMAL_FAVOR) | 可种植与不可种植分开 |
| 41 | **v8: 删除 max_level_ever** | 钻石判定直接用 effectiveLevel == 20，max_level_ever 设了不读 | 冗余字段 |
| 42 | **v8: ShopService 授权** | 新增 PlayerFlowerRightService.grantRight()，与 PlayerCropService.grantPermanent() 对称 | ShopService 扣钱后调免费授权，职责分离 |
| 43 | **v8: API 示例数值修正** | 11.2/11.3/11.5 示例改为 v7 公式一致数值 | 原示例用旧公式 |
| 44 | **v8: 补全 API 响应** | collect-all 响应结构 + 错误码汇总 + shop/buy 成功响应 | 原文档缺失 |
| 45 | **v9: CROP_UNLOCK/FLOWER_UNLOCK 拆分** | reward_type 拆为 CROP_UNLOCK(作物)+FLOWER_UNLOCK(花卉)，分别调 playerCropService/playerFlowerRightService | 狐狸奖励的花走错服务 |
| 46 | **v9: grantAnimalReward 修复** | 方法签名加 LocalDate today，方法内查 animalConfig | 原代码引用未定义变量 |
| 47 | **v9: feed decayApplied 修正** | 投喂响应 decayApplied 改为 0（当天已领取礼物，无衰减） | 原示例数值错误 |
| 48 | **v9: 食材奖励规则改写** | 松鼠/刺猬全程食材↔配方交替；知更鸟/狐狸 Lv10 后种子↔配方交替不再给食材 | 原描述与表数据矛盾 |
| 49 | **v9: collect-all 跳过说明** | 未解锁动物和当天已领取的动物自动跳过 | 原文档未明确 |
| 50 | **v9: flower_config 字段说明** | 购买价格迁移到 seed_shop_config，升级字段保留在 flower_config | FlowerController 升级端点仍读 flower_config |
| 51 | **v9: recipe_category 映射表** | 补充 29 个现有配方的 DRINK/ICE_CREAM/CAKE 分类 | 开发时批量 UPDATE 有据可依 |
