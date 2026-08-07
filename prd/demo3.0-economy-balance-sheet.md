# Demo3.0 经济数值精确配置表

> 创建日期：2026-08-01
> 状态：设计阶段
> 关联文档：demo3.0-infinite-level-and-economy-spec.md

## 1. 当前问题诊断

### 1.1 五种作物无配方

以下作物在 `recipe_material` 表中没有任何引用，只能卖原料：

| 作物 | 当前售价 | 问题 |
|------|---------|------|
| cabbage 白菜 | 3 | 无配方，只能卖原料 |
| potato 土豆 | 5 | 无配方 |
| chili 辣椒 | 20 | 无配方 |
| corn 玉米 | 40 | 无配方，但售价极高 |
| blueberry 蓝莓 | 12 | 无配方（Lv5 奖励配对的是 milk_ice_cream） |

### 1.2 牛奶鸡蛋售价为 0

`item_config` 中 milk 和 egg 的 `sell_price = 0`，导致 `milk_ice_cream`（材料 milk×2）纯利润 55 金，无成本。

### 1.3 成品无出售价

所有配方成品（strawberry_juice 等）在 `item_config` 中 `sell_price = 0`，背包中多余的成品无法出售回血。

### 1.4 原料售价过高

| 作物 | 售价 | 每小时每格收入* | 问题 |
|------|------|----------------|------|
| strawberry | 5 | 600 | Lv1 作物不该这么高 |
| corn | 40 | 1200 | 比大部分配方还赚钱 |
| moonberry | 120 | 7200 | 核弹级印钞机 |

*每小时每格 = (sell_price × yield / grow_seconds) × 3600

### 1.5 收获经验过高

| 作物 | 收获经验 | 每小时每格经验* |
|------|---------|----------------|
| strawberry | 5 | 300 |
| corn | 30 | 180 |
| moonberry | 40 | 480 |

*4 块草莓田 = 1200 经验/小时，Lv1→2 只需 100 经验 = 5 分钟

### 1.6 吧台经验等于配方经验

吧台直接快照 `sale_exp`，6 槽 × 20 份/小时 = 120 份 × 5~12 经验 = 600~1440 经验/小时（纯被动）。

---

## 2. 新增配方（补齐无配方作物）

### 2.1 新增 5 个配方

| recipe_id | 名称 | 材料 | make_time | unlock_level | sale_gold | sale_exp | bar_sale_interval | order_weight |
|-----------|------|------|-----------|-------------|-----------|----------|-------------------|-------------|
| cabbage_juice | 白菜汁 | cabbage×2 | 0 | 2 | 20 | 8 | 180 | 100 |
| blueberry_juice | 蓝莓汁 | blueberry×2 | 0 | 5 | 30 | 12 | 180 | 100 |
| potato_milk_soup | 土豆牛奶汤 | potato×2 + milk×1 | 0 | 5 | 35 | 14 | 180 | 80 |
| chili_chocolate | 辣椒巧克力 | chili×1 + milk×2 | 0 | 8 | 50 | 20 | 180 | 80 |
| corn_juice | 玉米汁 | corn×2 | 0 | 10 | 55 | 22 | 180 | 100 |

### 2.2 配方总数变更

现有 10 个 → 新增 5 个 → 共 15 个配方（Lv1-10 全覆盖）

---

## 3. item_config 出售价格调整

### 3.1 原料出售价

原则：原料直接出售几乎不赚钱，引导加工。加工利润率 = 300%~700%。

| item_id | 名称 | 现售价 | 新售价 | 变化 |
|---------|------|-------|--------|------|
| strawberry | 草莓 | 5 | 2 | -60% |
| cabbage | 小白菜 | 3 | 1 | -67% |
| carrot | 胡萝卜 | 8 | 2 | -75% |
| tomato | 番茄 | 6 | 2 | -67% |
| potato | 土豆 | 5 | 2 | -60% |
| chili | 辣椒 | 20 | 5 | -75% |
| corn | 玉米 | 40 | 7 | -83% |
| moonberry | 月光莓 | 120 | 10 | -92% |
| orange | 橙子 | 10 | 3 | -70% |
| blueberry | 蓝莓 | 12 | 3 | -75% |
| apple | 苹果 | 14 | 4 | -71% |
| watermelon | 西瓜 | 16 | 5 | -69% |
| wheat | 小麦 | 10 | 3 | -70% |
| lemon | 柠檬 | 14 | 5 | -64% |
| cucumber | 黄瓜 | 12 | 4 | -67% |
| milk | 牛奶 | 0 | 2 | 新定价 |
| egg | 鸡蛋 | 0 | 2 | 新定价 |

### 3.2 成品出售价

原则：成品可从背包出售，价格为配方售价的 40%（低于订单/吧台收益，但不至于完全无用）。

| item_id | 名称 | 配方售价 | 背包出售价(40%) |
|---------|------|---------|----------------|
| strawberry_juice | 草莓汁 | 25 | 10 |
| carrot_juice | 胡萝卜汁 | 30 | 12 |
| orange_juice | 橙汁 | 35 | 14 |
| tomato_juice | 番茄汁 | 35 | 14 |
| cabbage_juice | 白菜汁 | 20 | 8 |
| blueberry_juice | 蓝莓汁 | 30 | 12 |
| milk_ice_cream | 牛奶冰淇淋 | 30 | 12 |
| apple_carrot_juice | 苹果胡萝卜汁 | 40 | 16 |
| potato_milk_soup | 土豆牛奶汤 | 35 | 14 |
| watermelon_milk_ice_cream | 西瓜牛奶冰淇淋 | 55 | 22 |
| chili_chocolate | 辣椒巧克力 | 50 | 20 |
| strawberry_cake | 草莓蛋糕 | 80 | 32 |
| lemon_milk_ice_cream | 柠檬牛奶冰淇淋 | 55 | 22 |
| cucumber_apple_juice | 黄瓜苹果汁 | 45 | 18 |
| corn_juice | 玉米汁 | 55 | 22 |

### 3.3 新增食材（Demo3.0 动物/菌菇）

| item_id | 名称 | 出售价 | 来源 |
|---------|------|--------|------|
| hazelnut | 榛果 | 5 | 小松鼠每日礼物 |
| pine_nut | 松子 | 8 | 小松鼠每日礼物 |
| chestnut | 栗子 | 6 | 小松鼠每日礼物 |
| mushroom | 口蘑 | 5 | 刺猬每日礼物 |
| shiitake | 香菇 | 8 | 刺猬每日礼物 |
| chanterelle | 鸡油菌 | 15 | 刺猬每日礼物 |
| truffle | 松露 | 40 | 刺猬好感 Lv3 稀有产出 |
| blueberry_wild | 蓝莓(野) | 4 | 知更鸟每日礼物 |
| raspberry | 树莓 | 5 | 知更鸟每日礼物 |
| mulberry | 桑葚 | 4 | 知更鸟每日礼物 |
| mint | 薄荷 | 3 | 小狐狸/花园 |
| thyme | 百里香 | 5 | 小狐狸/花园 |
| honey | 蜂蜜 | 8 | 蜂巢产出 |

---

## 4. 配方配置调整（recipe_config）

### 4.1 全部 15 个配方

| recipe_id | 材料(数量) | 材料成本 | 售价 | 利润 | 利润率 | 经验 | 解锁等级 |
|-----------|-----------|---------|------|------|--------|------|---------|
| strawberry_juice | strawberry×2 | 4 | 25 | 21 | 525% | 10 | 1 |
| cabbage_juice | cabbage×2 | 2 | 20 | 18 | 900% | 8 | 2 |
| carrot_juice | carrot×2 | 4 | 30 | 26 | 650% | 12 | 2 |
| orange_juice | orange×2 | 6 | 35 | 29 | 483% | 15 | 3 |
| tomato_juice | tomato×2 | 4 | 35 | 31 | 775% | 15 | 4 |
| blueberry_juice | blueberry×2 | 6 | 30 | 24 | 400% | 12 | 5 |
| milk_ice_cream | milk×2 | 4 | 30 | 26 | 650% | 18 | 5 |
| potato_milk_soup | potato×2+milk×1 | 6 | 35 | 29 | 483% | 14 | 5 |
| apple_carrot_juice | apple×1+carrot×1 | 6 | 40 | 34 | 567% | 20 | 6 |
| watermelon_milk_ice_cream | watermelon×2+milk×1 | 12 | 55 | 43 | 358% | 25 | 7 |
| chili_chocolate | chili×1+milk×2 | 9 | 50 | 41 | 456% | 20 | 8 |
| strawberry_cake | strawberry×2+wheat×2+egg×1 | 12 | 80 | 68 | 567% | 35 | 8 |
| lemon_milk_ice_cream | lemon×2+milk×1 | 12 | 55 | 43 | 358% | 30 | 9 |
| cucumber_apple_juice | cucumber×1+apple×1 | 8 | 45 | 37 | 463% | 25 | 10 |
| corn_juice | corn×2 | 14 | 55 | 41 | 293% | 22 | 10 |

### 4.2 配方定价原则验证

- 最低利润率：corn_juice 293%（材料贵但产量大）
- 最高利润率：cabbage_juice 900%（材料极便宜）
- 平均利润率：~520%
- 趋势：等级越高利润率略降，但绝对利润升高（从 18 → 68 金币）

---

## 5. 作物收获经验调整（crop_level_config）

### 5.1 新收获经验

原则：收获是辅助经验源，不是主要来源。经验主要来自订单交付。

**可升级作物（3 级）：**

| crop_id | Lv1 经验 | Lv2 经验 | Lv3 经验 | 现Lv1→新Lv1 |
|---------|---------|---------|---------|-------------|
| strawberry | 1 | 2 | 3 | 5→1 |
| cabbage | 2 | 3 | 4 | 8→2 |
| carrot | 2 | 3 | 4 | 12→2 |
| tomato | 3 | 4 | 6 | 15→3 |
| potato | 4 | 6 | 8 | 18→4 |
| chili | 5 | 8 | 10 | 25→5 |
| corn | 6 | 9 | 12 | 30→6 |

**不可升级作物（1 级）：**

| crop_id | 新经验 | 旧经验 | 变化 |
|---------|--------|--------|------|
| moonberry | 5 | 40 | -87% |
| orange | 3 | 15 | -80% |
| blueberry | 3 | 18 | -83% |
| apple | 4 | 22 | -82% |
| watermelon | 5 | 25 | -80% |
| wheat | 4 | 20 | -80% |
| lemon | 5 | 25 | -80% |
| cucumber | 5 | 22 | -77% |

---

## 6. 吧台经验调整

### 6.1 新规则

吧台经验 = 配方 `sale_exp` × 0.5（向下取整），金币不变。

### 6.2 调整后吧台每份收益

| 配方 | 吧台金币 | 吧台经验(50%) | 原吧台经验 |
|------|---------|-------------|-----------|
| strawberry_juice | 25 | 5 | 10 |
| carrot_juice | 30 | 6 | 12 |
| orange_juice | 35 | 7 | 15 |
| tomato_juice | 35 | 7 | 15 |
| cabbage_juice | 20 | 4 | 8 |
| blueberry_juice | 30 | 6 | 12 |
| milk_ice_cream | 30 | 9 | 18 |
| potato_milk_soup | 35 | 7 | 14 |
| apple_carrot_juice | 40 | 10 | 20 |
| watermelon_milk_ice_cream | 55 | 12 | 25 |
| chili_chocolate | 50 | 10 | 20 |
| strawberry_cake | 80 | 17 | 35 |
| lemon_milk_ice_cream | 55 | 15 | 30 |
| cucumber_apple_juice | 45 | 12 | 25 |
| corn_juice | 55 | 11 | 22 |

### 6.3 实现方式

`DrinkBarServiceImpl` 上架快照时：`unit_exp_snapshot = recipe.sale_exp / 2`

或新增 `recipe_config.bar_sale_exp` 列，SQL 配置，默认 `sale_exp / 2`。

---

## 7. 经济模型验证

### 7.1 Lv5 模型（8 格田，草莓+胡萝卜，活跃 30 分钟/小时）

**生产：**
- 4 格草莓 × 2 个/60s × 6 轮 = 48 个/30min → 96 个/小时
- 4 格胡萝卜 × 3 个/180s × 3 轮 = 36 个/30min → 72 个/小时
- 制作：48 个草莓汁 + 36 个胡萝卜汁 = 84 杯/小时

**分配：**
- 顾客订单：~25 单/小时 × 1.5 均量 = 37.5 杯
- 吧台上架：6 槽 × 10 = 60 杯（每 30 分钟补一次）
- 剩余：84 - 37.5 - 46.5 = 0（刚好用完）

**每小时收入：**
| 来源 | 金币 | 经验 |
|------|------|------|
| 收获 | 0（不卖原料） | 96×1 + 72×2 = 240 |
| 订单 | 25 × 1.5 × 27.5(均) = 1031 | 25 × 1.5 × 11(均) = 412 |
| 吧台 | 46.5 × 27.5 = 1279 | 46.5 × 5.5(50%) = 256 |
| **合计** | **2310** | **908** |

**Lv5→6 需 810 经验 → ~53 分钟** ✓

**每小时支出估算：**
- 种子费：8 格 × 5 金 = 40/小时（假设每 2 小时补种）
  实际：草莓 60s 一轮，每小时种 10 次 × 4 格 = 40 株 × 2 金 = 80
  胡萝卜 180s 一轮，每小时种 3 次 × 4 格 = 12 株 × 2 金 = 24
  合计种子费：~104 金/小时
- 净收入：2310 - 104 = 2206 金/小时
- 可购土地：Farm-C 每格 200-400 → 约 5-10 格/小时

### 7.2 Lv10 模型（16 格田，多作物，活跃 30 分钟/小时）

**生产：**
- 8 格中阶作物（橙子/番茄/蓝莓）均 3.5 个/300s × 6 轮 = 168 个/30min → 336 个/小时
- 8 格高阶作物（小麦/玉米）均 5 个/450s × 4 轮 = 160 个/30min → 320 个/小时
- 制作：168 杯 + 64 杯（玉米 2:1）= 232 杯/小时

**每小时收入：**
| 来源 | 金币 | 经验 |
|------|------|------|
| 收获 | 0 | 336×3.5 + 320×5 = 1176 + 1600 = 2776 |
  *修正：收获经验 = 168×3.5avg + 320×5avg = 588 + 1600 = 2188*
| 订单 | 25 × 1.5 × 42(均) = 1575 | 25 × 1.5 × 18(均) = 675 |
| 吧台 | 100 × 42 = 4200 | 100 × 9(50%) = 900 |
| **合计** | **5775** | **3763** |

**Lv10→11 需 1995 经验 → ~32 分钟**

*注：这个速率偏高，但 Lv10 玩家有 15 种配方+16 格田，操作量大，30 分钟活跃/小时已是较肝的节奏。*

**每小时支出估算：**
- 种子费：~300 金/小时（更多格子+更贵的种子）
- 配方商店：分期购买，均摊 ~500/小时
- 净收入：5775 - 800 = 4975 金/小时

### 7.3 Lv20 模型（24 格田，全配方，活跃 30 分钟/小时）

**生产：**
- 24 格全高阶作物，均 4.5 个/400s × 4.5 轮 = 486 个/30min → 972 个/小时
- 制作：~400 杯/小时

**每小时收入（含精通 Lv20 加成：产量+20%，售价+20%）：**
| 来源 | 金币 | 经验 |
|------|------|------|
| 收获 | 0 | 972 × 5(均) × 1.2(精通) = 5832 |
  *修正：收获经验不受精通影响*
  *972 × 5 = 4860*
| 订单 | 25 × 1.5 × 55(均) × 1.2 = 2475 | 25 × 1.5 × 25(均) = 937 |
| 吧台 | 150 × 55 × 1.2 = 9900 | 150 × 12.5(50%) = 1875 |
| **合计** | **12375** | **7672** |

**Lv20→21 需 4912 经验 → ~38 分钟**

*注：Lv20 时产量+20%意味着同样材料产出更多成品，吧台+订单金币按 ×1.2(售价加成) 计算。经验不享受精通加成。*

**每小时支出估算：**
- 种子费：~600 金/小时
- 土地维护费：24 格 × 30 金 = 720 金/天 → 30/小时
- 配方商店（菌菇系列）：均摊 ~1000/小时
- NPC 礼物：~200/小时
- 装饰物：均摊 ~500/小时
- 净收入：12375 - 2330 = 10045 金/小时

### 7.4 升级节奏总览

| 等级段 | 经验/小时 | 单级经验 | 每级耗时 | 体感 |
|--------|----------|---------|---------|------|
| Lv1-5 | ~500 | 100-810 | 12-97 分钟 | 快速上手 |
| Lv5-10 | ~1500 | 810-1995 | 32-133 分钟 | 稳步成长 |
| Lv10-15 | ~3000 | 1995-3240 | 40-65 分钟 | 需要肝一点 |
| Lv15-20 | ~5000 | 3240-4912 | 39-59 分钟 | 持续投入 |
| Lv20-50 | ~7000 | 4912-11180 | 42-95 分钟 | 长线追求 |
| Lv50-100 | ~8000 | 11180-19953 | 84-150 分钟 | 慢速积累 |

*假设活跃 30 分钟/小时，Lv20 后精通加成提升产出但不提升经验*

---

## 8. 种子费用设计

### 8.1 种子定价

当前种子免费（level_reward）或一次性购买（gold_shop）。新方案：每次种植收种子费。

| 作物等级 | 种子费/株 | 说明 |
|---------|----------|------|
| Lv1-2 作物 | 1-2 金 | 草莓/白菜/胡萝卜 |
| Lv3-4 作物 | 2-3 金 | 橙子/番茄 |
| Lv5-6 作物 | 3-5 金 | 蓝莓/苹果/土豆 |
| Lv7-8 作物 | 5-8 金 | 西瓜/辣椒/小麦 |
| Lv9-10 作物 | 8-15 金 | 柠檬/黄瓜/玉米 |
| 月光莓 | 20 金 | 稀有作物 |

### 8.2 实现

在 `player_land` 种植时扣金币，或在 `crop_config` 新增 `seed_cost` 字段。

---

## 9. 配方商店定价

### 9.1 菌菇配方（金币购买）

| 配方 | 售价 | 材料 | 说明 |
|------|------|------|------|
| 蘑菇茶 | 800 | 香菇×2+薄荷×1 | 入门菌菇 |
| 口蘑奶昔 | 1000 | 口蘑×2+牛奶×1+蜂蜜×1 | |
| 鸡油菌浓汤 | 2000 | 鸡油菌×2+牛奶×1+口蘑×1 | |
| 松露热可可 | 5000 | 松露×1+牛奶×1+蜂蜜×1 | 终极配方 |
| 鸡油菌蛋挞 | 2500 | 鸡油菌×2+小麦×1+鸡蛋×1+牛奶×1 | |
| 蘑菇松露派 | 4500 | 口蘑×2+松露×1+小麦×2+鸡蛋×1 | |
| 松露蛋糕 | 5000 | 松露×1+小麦×2+鸡蛋×1+牛奶×1 | 终极配方 |
| 香菇栗子蛋糕 | 3000 | 香菇×1+栗子×2+小麦×2+鸡蛋×1 | |

### 9.2 联动配方（好感度奖励，不卖）

| 配方 | 解锁条件 | 售价 | 经验 |
|------|---------|------|------|
| 菌菇坚果浓汤 | 刺猬Lv2+松鼠Lv2 | 160 | 35 |
| 浆果松露挞 | 刺猬Lv3+知更鸟Lv2 | 320 | 70 |
| 香草菌菇茶 | 刺猬Lv2+狐狸Lv2 | 100 | 20 |

---

## 10. SQL 变更清单

### 10.1 item_config 更新

```sql
-- 原料降价
UPDATE item_config SET sell_price=2 WHERE item_id IN('strawberry','cabbage','carrot','tomato','potato');
UPDATE item_config SET sell_price=5 WHERE item_id='chili';
UPDATE item_config SET sell_price=7 WHERE item_id='corn';
UPDATE item_config SET sell_price=10 WHERE item_id='moonberry';
UPDATE item_config SET sell_price=3 WHERE item_id IN('orange','blueberry','wheat');
UPDATE item_config SET sell_price=4 WHERE item_id IN('apple','cucumber');
UPDATE item_config SET sell_price=5 WHERE item_id IN('watermelon','lemon');
UPDATE item_config SET sell_price=2 WHERE item_id IN('milk','egg');

-- 成品定价（新增 item_config 记录或更新）
-- ... 见 3.2 节
```

### 10.2 recipe_config 新增

```sql
INSERT INTO recipe_config (id,name,output_item,make_time,unlock_level,sale_gold,sale_exp,bar_sale_interval_seconds,order_weight,enabled) VALUES
('cabbage_juice','白菜汁','cabbage_juice',0,2,20,8,180,100,1),
('blueberry_juice','蓝莓汁','blueberry_juice',0,5,30,12,180,100,1),
('potato_milk_soup','土豆牛奶汤','potato_milk_soup',0,5,35,14,180,80,1),
('chili_chocolate','辣椒巧克力','chili_chocolate',0,8,50,20,180,80,1),
('corn_juice','玉米汁','corn_juice',0,10,55,22,180,100,1);
```

### 10.3 recipe_material 新增

```sql
INSERT INTO recipe_material (recipe_id,item_id,count) VALUES
('cabbage_juice','cabbage',2),
('blueberry_juice','blueberry',2),
('potato_milk_soup','potato',2),
('potato_milk_soup','milk',1),
('chili_chocolate','chili',1),
('chili_chocolate','milk',2),
('corn_juice','corn',2);
```

### 10.4 recipe_config 更新（现有配方调价）

```sql
UPDATE recipe_config SET sale_gold=25, sale_exp=10 WHERE id='strawberry_juice';
UPDATE recipe_config SET sale_gold=30, sale_exp=12 WHERE id='carrot_juice';
UPDATE recipe_config SET sale_gold=35, sale_exp=15 WHERE id='orange_juice';
UPDATE recipe_config SET sale_gold=35, sale_exp=15 WHERE id='tomato_juice';
UPDATE recipe_config SET sale_gold=30, sale_exp=18 WHERE id='milk_ice_cream';
UPDATE recipe_config SET sale_gold=40, sale_exp=20 WHERE id='apple_carrot_juice';
UPDATE recipe_config SET sale_gold=55, sale_exp=25 WHERE id='watermelon_milk_ice_cream';
UPDATE recipe_config SET sale_gold=80, sale_exp=35 WHERE id='strawberry_cake';
UPDATE recipe_config SET sale_gold=55, sale_exp=30 WHERE id='lemon_milk_ice_cream';
UPDATE recipe_config SET sale_gold=45, sale_exp=25 WHERE id='cucumber_apple_juice';
```

### 10.5 crop_level_config 更新（收获经验）

```sql
-- 可升级作物
UPDATE crop_level_config SET harvest_exp=1 WHERE crop_id='strawberry' AND crop_level=1;
UPDATE crop_level_config SET harvest_exp=2 WHERE crop_id='strawberry' AND crop_level=2;
UPDATE crop_level_config SET harvest_exp=3 WHERE crop_id='strawberry' AND crop_level=3;
-- ... 其余作物见第 5 节

-- 不可升级作物
UPDATE crop_level_config SET harvest_exp=5 WHERE crop_id='moonberry';
UPDATE crop_level_config SET harvest_exp=3 WHERE crop_id='orange';
-- ... 其余见第 5.1 节
```

### 10.6 吧台经验调整

方案 A（代码修改）：
```java
// DrinkBarServiceImpl.java 上架时
order.setUnitExpSnapshot(recipe.getSaleExp() / 2);
```

方案 B（SQL 新增列）：
```sql
ALTER TABLE recipe_config ADD COLUMN bar_sale_exp INT DEFAULT 0;
UPDATE recipe_config SET bar_sale_exp = sale_exp / 2;
```

---

## 11. 数值调整总结

| 维度 | 旧值范围 | 新值范围 | 变化 |
|------|---------|---------|------|
| 原料售价 | 3-120 | 1-10 | -80~92% |
| 成品售价 | 0(无) | 8-32(背包) / 20-80(配方) | 新增 |
| 配方金币 | 30-80 | 20-80 | 微调 |
| 配方经验 | 5-12 | 8-35 | +60~190% |
| 收获经验 | 5-40 | 1-6 | -80~87% |
| 吧台经验 | =配方经验 | =配方经验×50% | -50% |
| 种子费 | 0(免费) | 1-20/株 | 新增 |
| 配方数 | 10 | 15 | +5 |
| 金币消耗口 | 2(土地+升级) | 6+(土地+种子+配方店+维护+礼物+装饰) | +4 |

---

## 12. 作物体系调整（2026-08-01 更新）

### 12.1 删除 4 种作物

删除白菜(cabbage)、土豆(potato)、辣椒(chili)、玉米(corn)，清理以下表：

| 表 | 删除记录 |
|----|---------|
| crop_config | cabbage / potato / chili / corn |
| crop_level_config | 这 4 种的 3 级配置（共 12 行） |
| crop_unlock_source | cabbage(GOLD_SHOP)、potato(LEVEL_REWARD)、chili(GOLD_SHOP)、corn(LEVEL_REWARD) |
| item_config | 这 4 种的物品定义 |

同时删除上文第 2 节中为这 4 种作物新增的配方：cabbage_juice / potato_milk_soup / chili_chocolate / corn_juice。保留 blueberry_juice。

### 12.2 作物来源渠道（单一渠道原则）

每个作物只有一个来源渠道，商店购买和岛屿升级赠送互斥：

| 作物 | 来源渠道 | 说明 |
|------|---------|------|
| strawberry 草莓 | 岛Lv1 赠送 | 初始作物 |
| carrot 胡萝卜 | 岛Lv2 赠送 | 取消商店购买 |
| orange 橙子 | 岛Lv3 赠送 | |
| tomato 番茄 | 岛Lv4 赠送 | 取消商店购买 |
| blueberry 蓝莓 | 岛Lv5 赠送 | |
| apple 苹果 | 岛Lv6 赠送 | |
| watermelon 西瓜 | 岛Lv7 赠送 | |
| wheat 小麦 | 岛Lv8 赠送 | |
| lemon 柠檬 | 岛Lv9 赠送 | |
| cucumber 黄瓜 | 岛Lv10 赠送 | |
| moonberry 月光莓 | 每日稀有作物随机池 | |

共 11 种作物，全部通过岛屿升级或随机池获取。商店不卖作物种子（改卖花卉种子，见第 13 节）。

### 12.3 取消商店购买等级限制

crop_unlock_source 中 GOLD_SHOP / DIAMOND_SHOP 类型记录的 `required_player_level` 字段不再限制购买等级。商店物品随时可买（但作物种子已全部移出商店）。

### 12.4 配方总数变更（修正）

原有 10 个配方 + 新增 blueberry_juice = **11 个基础配方**。另新增 16 个花卉配方（见第 15 节），总计 **27 个配方**。

---

## 13. 花卉系统

### 13.1 概述

8 种可食用花卉，全部通过商店购买种子（无等级限制），不通过岛屿升级赠送。花卉有三重用途：配方材料、产蜜原料、NPC 赠礼。

### 13.2 花卉种子配置

| 花卉 | item_id | 购买货币 | 种子价格 | 生长时间 | 产量 | 收获经验 | 产蜜系数 |
|------|---------|---------|---------|---------|------|---------|---------|
| 玫瑰 | rose | 金币 | 500 | 300s | ×2 | 20 | 1 |
| 菊花 | chrysanthemum | 金币 | 300 | 240s | ×3 | 15 | 1 |
| 茉莉花 | jasmine | 金币 | 400 | 300s | ×3 | 18 | 1 |
| 桂花 | osmanthus | 金币 | 600 | 360s | ×3 | 22 | 1 |
| 薰衣草 | lavender | 金币 | 800 | 420s | ×2 | 25 | 1 |
| 洛神花 | roselle | 金币 | 500 | 300s | ×3 | 20 | 1 |
| 洋甘菊 | chamomile | 金币 | 300 | 240s | ×3 | 15 | 1 |
| 樱花 | sakura | 钻石 | 10 | 480s | ×2 | 25 | 2 |

- 金币花产蜜系数 = 1，钻石花产蜜系数 = 2
- 花卉可升级，等级范围 Lv1-10（后续可扩展）
- 花卉等级影响：产量倍率 + 产蜜倍率（见第 14 节）

### 13.3 花卉等级与升级

花卉使用公式驱动等级倍率，而非硬编码表：

**产量倍率** = `1 + 0.3 × (level - 1)`，上限 3.0（Lv7+ 封顶）

| 花等级 | 产量倍率 | 说明 |
|--------|---------|------|
| Lv1 | 1.0 | 基础 |
| Lv2 | 1.3 | |
| Lv3 | 1.6 | |
| Lv5 | 2.2 | |
| Lv7+ | 3.0（封顶） | |

**产蜜倍率** = `min(1 + 0.4 × (level - 1)^0.85, 5.0)`，上限 5.0（约 Lv16 封顶）

| 花等级 | 产蜜倍率 | 金币花贡献/朵 | 钻石花贡献/朵 |
|--------|---------|-------------|-------------|
| Lv1 | 1.00 | 1.0 | 2.0 |
| Lv2 | 1.40 | 1.4 | 2.8 |
| Lv3 | 1.72 | 1.7 | 3.4 |
| Lv5 | 2.31 | 2.3 | 4.6 |
| Lv8 | 3.12 | 3.1 | 6.2 |
| Lv10 | 3.63 | 3.6 | 7.3 |
| ~Lv16 | 5.00（封顶） | 5.0 | 10.0 |

- 0.85 指数：亚线性增长，等级越高每级提升越小
- 5.0 硬上限：当前 Lv1-10 摸不到上限，扩展到 Lv20/50 也不会失控
- 升级费用参照作物升级体系，后续在 crop_level_config 中配置

### 13.4 花卉出售价

| 花卉 | 出售价 | 说明 |
|------|--------|------|
| 玫瑰 | 8 | |
| 菊花 | 5 | |
| 茉莉花 | 6 | |
| 桂花 | 10 | |
| 薰衣草 | 12 | |
| 洛神花 | 8 | |
| 洋甘菊 | 5 | |
| 樱花 | 15 | 钻石花售价更高 |

---

## 14. 蜂蜜系统

### 14.1 概述

蜂蜜是单一物品（item_id: honey），不分种类。蜂蜜由蜂箱产出，产量取决于玩家种植的花卉数量、花卉购买货币系数和花卉等级。

### 14.2 产蜜公式

```
每周期产蜜量 = Σ（每种花: 数量 × 产蜜系数 × 等级倍率）
产蜜系数：金币花 = 1，钻石花 = 2
等级倍率 = min(1 + 0.4 × (level - 1)^0.85, 5.0)
```

计算结果向下取整。

### 14.3 产蜜规则

| 规则 | 说明 |
|------|------|
| 产蜜周期 | 每 2 小时结算一次 |
| 结算条件 | 遍历所有已成熟的花卉，未成熟的不计入 |
| 无花处理 | 没有已成熟花卉时不产蜜 |
| 存储上限 | 蜂箱存储满时停止产蜜（不溢出丢弃） |
| 蜜蜂 NPC 加成 | 暂不纳入（如后续加入蜜蜂小动物 NPC，可追加加成系数） |

### 14.4 蜂箱配置

| 蜂箱数量 | 存储上限 | 购买价格 |
|---------|---------|---------|
| 1 个 | 20 蜂蜜 | 1000 金币 |
| 2 个 | 40 蜂蜜 | 2000 金币 |
| 3 个（上限） | 60 蜂蜜 | 3000 金币 |

- 每个玩家最多 3 个蜂箱
- 蜂箱只决定存储上限，不影响产蜜速度
- 存储满后停止产蜜，玩家收取后恢复生产

### 14.5 产蜜计算示例

玩家种了：玫瑰×3(Lv5, 金币) + 菊花×2(Lv1, 金币) + 樱花×1(Lv3, 钻石)

```
产蜜 = 3 × 1 × 2.31  (玫瑰 Lv5)
     + 2 × 1 × 1.00  (菊花 Lv1)
     + 1 × 2 × 1.72  (樱花 Lv3)
     = 6.93 + 2.00 + 3.44
     = 12.37 → 12 蜂蜜/周期
```

### 14.6 蜂蜜出售价

| 物品 | 出售价 |
|------|--------|
| honey 蜂蜜 | 8 金币 |

---

## 15. 花卉与蜂蜜配方

### 15.1 概述

16 个花卉配方（8 饮品 + 8 蛋糕），全部在交易所配方商店用金币购买。配方随时可买，但能否制作取决于玩家是否种了对应花卉和拥有蜂蜜。

### 15.2 饮品店配方

| 配方名 | recipe_id | 材料 | 购买价格 | 售价 | 经验 |
|--------|-----------|------|---------|------|------|
| 玫瑰花茶 | rose_tea | rose×2 + honey×1 | 1000 | 80 | 20 |
| 菊花茶 | chrysanthemum_tea | chrysanthemum×2 | 500 | 40 | 12 |
| 茉莉花茶 | jasmine_tea | jasmine×2 | 800 | 50 | 15 |
| 桂花茶 | osmanthus_tea | osmanthus×2 + honey×1 | 1000 | 70 | 18 |
| 薰衣草拿铁 | lavender_latte | lavender×1 + milk×1 + honey×1 | 1500 | 120 | 30 |
| 洛神花茶 | roselle_tea | roselle×2 | 800 | 50 | 15 |
| 樱花茶 | sakura_tea | sakura×2 + honey×1 | 2000 | 100 | 25 |
| 洋甘菊茶 | chamomile_tea | chamomile×2 | 500 | 40 | 12 |

### 15.3 蛋糕店配方

| 配方名 | recipe_id | 材料 | 购买价格 | 售价 | 经验 |
|--------|-----------|------|---------|------|------|
| 玫瑰蛋糕 | rose_cake | rose×2 + wheat×2 + egg×1 | 1500 | 150 | 35 |
| 菊花酥 | chrysanthemum_cookie | chrysanthemum×2 + wheat×1 + egg×1 | 800 | 80 | 20 |
| 茉莉慕斯 | jasmine_mousse | jasmine×2 + milk×1 + egg×1 | 1200 | 100 | 25 |
| 桂花糕 | osmanthus_cake | osmanthus×2 + wheat×1 + egg×1 | 1000 | 90 | 22 |
| 薰衣草马卡龙 | lavender_macaron | lavender×1 + wheat×1 + egg×2 + milk×1 | 2500 | 200 | 45 |
| 洛神花果冻 | roselle_jelly | roselle×2 + egg×1 | 800 | 70 | 18 |
| 樱花蛋糕 | sakura_cake | sakura×2 + wheat×2 + egg×1 + milk×1 | 3000 | 250 | 50 |
| 洋甘菊饼干 | chamomile_cookie | chamomile×1 + wheat×1 + egg×1 | 500 | 60 | 15 |

### 15.4 配方定价逻辑

- 花卉配方售价（40-250）显著高于普通水果配方（20-80），因为种子成本高 + 产量低
- 需要蜂蜜的配方售价更高（如玫瑰花茶 80 vs 菊花茶 40），体现加工链价值
- 樱花系列是最高价值配方（钻石种子 + 需要蜂蜜），作为后期追求
- 薰衣草系列定位高端（种子最贵 800 金 + 需要蜂蜜 + 牛奶）
- 配方购买价格 = 售价 × 10~15 倍，高售价配方购买门槛更高

---

## 16. 更新后的数值总结（2026-08-01 修订）

| 维度 | 旧值范围 | 新值范围 | 变化 |
|------|---------|---------|------|
| 作物种类 | 15 种 | 11 种 + 8 种花卉 | -4 作物 +8 花卉 |
| 作物来源 | 混合（商店+等级+奖励） | 单一渠道（岛屿赠送/随机池） | 简化 |
| 花卉来源 | 无 | 商店购买（无等级限制） | 新增 |
| 花卉等级 | 无 | Lv1-10（可扩展），公式驱动 | 新增 |
| 蜂蜜 | 无 | 单一物品，公式产蜜，上限 5.0 倍率 | 新增 |
| 蜂箱 | 无 | 最多 3 个，存储上限 20/40/60 | 新增 |
| 原料售价 | 3-120 | 1-15 | -80~92% |
| 成品售价 | 0(无) | 8-32(背包) / 20-250(配方) | 新增 |
| 配方金币 | 30-80 | 20-250 | 拉开差距 |
| 配方经验 | 5-12 | 8-50 | +60~317% |
| 配方总数 | 10 | 27（11 基础 + 16 花卉） | +17 |
| 收获经验 | 5-40 | 1-8 | -80~87% |
| 吧台经验 | =配方经验 | =配方经验×50% | -50% |
| 种子费 | 0(免费) | 1-20/株(作物) + 300-800金/10钻(花卉) | 新增 |
| 金币消耗口 | 2 | 8+(土地+种子+花卉种子+配方店+蜂箱+维护+礼物+装饰) | +6 |
