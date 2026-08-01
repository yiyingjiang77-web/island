# Demo3.0 NPC 系统与互动世界规格

> 状态：草案  
> 整理日期：2026-07-31（初稿），2026-08-01（场景改造 + 小动物 NPC + 菌菇食谱）  
> 当前客户端入口：`FruitIslandClient/demo2.4-island.html`  
> 领域词汇：`CONTEXT.md`  
> 前置文档：`prd/island-phasing-plan.md`、`prd/demo2.4-current-progress.md`

## 1. 版本目标

在现有种植 → 制作 → 售卖的经济闭环之上，引入 NPC 居民、小动物和互动区域，让山谷从"生产经营工具"变成"有温度的社区"：

```text
日常种植/制作/售卖
  ↓
产出多余的作物/饮品/蛋糕
  ↓
赠送给 NPC 建立好感度 → 解锁限定配方/稀有种子/特殊装饰
  ↓
小动物每日送来野生食材（坚果/菌菇/浆果）→ 丰富配方深度
  ↓
好感度升级解锁隐藏互动区域（密林深处）
  ↓
新区域产出新材料 → 反哺制作和经营
```

本版本不实现 P7 探索岛，所有 NPC 和互动区域都在主山谷 48×48 范围内。

## 2. 场景设定与世界布局

### 2.1 场景变更说明

Demo2.x 阶段场景为"果香小岛"（热带海岛），Demo3.0 起改为**"果香山谷"（温带山林谷地）**。

| 维度 | 旧（果香小岛） | 新（果香山谷） |
|------|--------------|--------------|
| 气候 | 热带/亚热带 | 温带四季 |
| 水域 | 海 | 河流（贯穿山谷底部） |
| 入口 | 码头上岸 | 木桥入谷 |
| 树墙 | 海岸防风林 | 未探索的密林深处 |
| 钓鱼 | 海钓 | 河钓/溪钓 |
| 菠萝 | 原生作物 | 温室限定（花姨 Lv3 解锁） |
| 椰子 | 原生作物 | 砍掉或改为进口商品 |
| 小动物 | 无 | 松鼠/刺猬/知更鸟/小狐狸（原住民） |

**改名原因**：游戏涉及小麦、坚果、菌菇、鸡蛋牛奶等温带食材，热带海岛设定下这些食材不合理。改为山谷林地后，所有食材天然合理，小动物是原住民而非违和存在，"砍树解锁密林"也更顺理成章。

### 2.2 48×48 山谷布局

| 区域 | 坐标范围（约） | 内容 |
|------|---------------|------|
| 密林深处 | Y=1-4 | 未探索区域，树木遮挡，砍树解锁后通往隐藏区域 |
| 左区·饮品产业 | Y=5-16, X=1-10 | 饮品店 + 6 块农田/果园环绕 |
| 右区·甜品产业 | Y=5-16, X=21-30 | 蛋糕店 + 花园 + 蜂巢 + 鸡舍/牛棚 |
| 中央主干道 | Y=5-16, X=11-20 | 连接左右两区的石板路，两侧有 NPC 摊位 |
| 下区左·河岸 | Y=17-44, X=1-10 | 河流 + 木桥（入口）+ 河边钓点 + 阿海小屋 |
| 下区中·装饰区 | Y=17-30, X=11-20 | 长椅/花坛/喷泉，纯装饰可摆放 |
| 下区右·公共区 | Y=17-30, X=21-30 | 交易所 + 糖糖品鉴桌 + 小铃帐篷 |
| 密林后方 | Y=1-4 全宽 | 隐藏互动区域（温泉/秘密钓点/秘密花园/精灵树） |

### 2.3 小动物栖息地分布

小动物不是 NPC 摊位，是地图原住民，住在各自自然栖息地：

| 动物 | 栖息地 | 坐标 | 视觉 |
|------|--------|------|------|
| 小松鼠 | 果园旁树洞 | 左区 Y=8, X=4 | 大树根部的小洞，旁边堆着橡果 |
| 刺猬 | 花园灌木下 | 右区 Y=10, X=27 | 灌木丛下的小窝，铺着落叶 |
| 知更鸟 | 饮品店屋檐 | 左区 Y=5, X=6 | 屋檐下的鸟巢，偶尔探出头 |
| 小狐狸 | 密林边缘 | 顶部 Y=5, X=15 | 树丛间隙偶尔露出的红色身影 |

每天在动物栖息地旁会出现一个礼物气泡，玩家走过去点击即可收取。

## 3. 人物 NPC 角色

### 3.1 NPC 总览

共 5 名人物 NPC，每人有独立摊位、好感度系统和回报阶梯。

| NPC | 摊位名称 | 位置 | 性格关键词 | 核心功能 |
|-----|---------|------|-----------|---------|
| 阿海 | 渔夫小屋 | 木桥旁河岸 | 沉默寡言、外冷内热 | 解锁钓鱼、送鱼获 |
| 小铃 | 旅行帐篷 | 装饰区入口 | 活泼贪吃、精明 | 卖稀有种子和装饰 |
| 花姨 | 花匠铺 | 花园旁 | 温柔唠叨、爱花成痴 | 送花种、教嫁接、解锁温室 |
| 糖糖 | 品鉴桌 | 交易所旁 | 傲娇挑剔、嘴硬心软 | 甜品打分、解锁星级配方 |
| 小灰 | 邮差站 | 木桥到主干道之间 | 勤劳迷糊、爱丢信 | 每日随机任务 |

### 3.2 NPC 详细设定

#### 阿海 — 老渔夫

| 属性 | 内容 |
|------|------|
| 外形 | 戴草帽、穿蓝色围裙的老爷爷，旁边停一艘小木船 |
| 摊位 | 木桥左侧的河岸小木屋，门口晾着渔网 |
| 对话风格 | 话少，但说到河和鱼就停不下来 |
| 日常台词 | "今天的河水很清。" / "别小看这条河。" |
| 好感故事线 | 年轻时是远洋船长，退休后在山谷河边安家，内心想再出海一次 |
| 场景调整 | 从"海钓"改为"河钓"，鱼种为淡水鱼（鳟鱼/鲤鱼/河蟹/银鱼） |

#### 小铃 — 猫商人

| 属性 | 内容 |
|------|------|
| 外形 | 穿斗篷的猫人，推一辆堆满杂货的手推车，头上顶着一个小铃铛 |
| 摊位 | 装饰区入口处的彩色帐篷 |
| 对话风格 | 热情推销，但价格公道，偶尔大放送 |
| 日常台词 | "客官来看看！今天到了好货！" / "这个价格已经是友情价了哦~" |
| 好感故事线 | 小铃是旅行商人世家的独女，到处收集稀奇货物，其实是在找失散的妹妹 |
| 场景调整 | 菠萝/椰子等热带食材改为小铃商店的进口商品 |

#### 花姨 — 花匠老奶奶

| 属性 | 内容 |
|------|------|
| 外形 | 穿碎花围裙的老奶奶，围裙口袋里插着剪刀和花种 |
| 摊位 | 花园旁边的木架花棚 |
| 对话风格 | 温柔但唠叨，总想把花种塞给玩家 |
| 日常台词 | "来来来，这颗种子你拿着。" / "花开的时候记得回来看啊。" |
| 好感故事线 | 花姨的丈夫是曾经的山谷园艺师，花姨守着花园等他回来 |
| 场景调整 | 好感 Lv3 解锁温室，温室可种菠萝等热带作物 |

#### 糖糖 — 甜品鉴赏家

| 属性 | 内容 |
|------|------|
| 外形 | 戴墨镜、穿时尚风衣的年轻人，手里永远拿着一本黑色笔记本 |
| 摊位 | 交易所旁边的品鉴桌，桌上摆着刀叉和放大镜 |
| 对话风格 | 傲娇挑剔，从不直接夸你，但偷偷在笔记本上画星星 |
| 日常台词 | "勉强能入口。" / "别以为多放糖就能糊弄我。" |
| 好感故事线 | 糖糖其实是著名美食评论家，隐姓埋名来山谷找灵感 |

#### 小灰 — 邮差鸽子

| 属性 | 内容 |
|------|------|
| 外形 | 一只灰白色鸽子，背着绿色邮包，歪着头看人 |
| 摊位 | 木桥到主干道之间的木质信箱站 |
| 对话风格 | 聒噪、健忘，经常丢三落四 |
| 日常台词 | "咕！信...信放哪了？" / "今天有好几封信呢，大概。" |
| 好感故事线 | 小灰是新上任的邮差，总被山风吹丢信件，其实它以前是赛鸽冠军 |

## 4. 小动物 NPC

### 4.1 概述

小动物与人物 NPC 不同，是地图原住民。它们没有摊位、不卖东西、不接任务。核心机制是**每日自动产出野生食材**，玩家只需去栖息地收取。同时可投喂食物提升好感度，好感度越高产出品质越好。

### 4.2 小动物总览

| 动物 | 栖息地 | 每日礼物 | 好感满级 | 满级回报 |
|------|--------|----------|---------|---------|
| 小松鼠 | 果园旁树洞 | 榛果/松子/栗子（轮换） | 亲密 | 偶尔带来金松子（稀有食材） |
| 刺猬 | 花园灌木下 | 蘑菇（按季节变种类） | 亲密 | 偶尔带来松露（稀有食材） |
| 知更鸟 | 饮品店屋檐 | 蓝莓/树莓/桑葚（轮换） | 亲密 | 偶尔带来金浆果（稀有食材） |
| 小狐狸 | 密林边缘 | 每周来 2-3 次，带稀有草药 | 亲密 | 带玩家找隐藏宝藏点 |

### 4.3 小动物详细设定

#### 小松鼠

| 属性 | 内容 |
|------|------|
| 外形 | 棕红色小松鼠，蓬松大尾巴，嘴里常叼着橡果 |
| 栖息地 | 果园旁大树根部的树洞（左区 Y=8, X=4） |
| 性格 | 活泼好动，怕生但好奇，熟了会主动靠近 |
| 每日礼物 | 榛果、松子、栗子（三种轮换，每天一种 ×2） |
| 季节加成 | 秋季每日产量 ×2 |
| 好感满级 | 亲密 → 每周额外带来 1 颗金松子（高级甜品的稀有材料） |
| 投喂偏好 | 最爱：坚果类；喜欢：浆果类；普通：水果；不喜欢：蔬菜 |

#### 刺猬

| 属性 | 内容 |
|------|------|
| 外形 | 小刺猬，圆滚滚的，鼻子不停地嗅地面 |
| 栖息地 | 花园灌木丛下的小窝，铺着落叶（右区 Y=10, X=27） |
| 性格 | 胆小慢热，但一旦信任你会翻肚皮 |
| 每日礼物 | 蘑菇（春季口蘑、夏季香菇、秋季鸡油菌，每种 ×2） |
| 季节加成 | 秋季每日产量 ×2（菌菇丰收季） |
| 好感满级 | 亲密 → 每周额外带来 1 个松露（顶级甜品材料，无法种植） |
| 投喂偏好 | 最爱：浆果类；喜欢：水果；普通：坚果；不喜欢：饮品 |

#### 知更鸟

| 属性 | 内容 |
|------|------|
| 外形 | 胸前橙色羽毛的小鸟，清晨在屋檐唱歌 |
| 栖息地 | 饮品店屋檐下的鸟巢（左区 Y=5, X=6） |
| 性格 | 警觉但爱唱歌，熟悉后会站在你肩上 |
| 每日礼物 | 蓝莓、树莓、桑葚（三种轮换，每天一种 ×2） |
| 季节加成 | 夏季每日产量 ×2（浆果成熟季） |
| 好感满级 | 亲密 → 每周额外带来 1 颗金浆果（高级饮品材料） |
| 投喂偏好 | 最爱：浆果类；喜欢：坚果类；普通：种子；不喜欢：蛋糕 |

#### 小狐狸

| 属性 | 内容 |
|------|------|
| 外形 | 红棕色小狐狸，眼神机灵，尾巴尖是白色 |
| 栖息地 | 密林边缘树丛间隙（顶部 Y=5, X=15），不固定出现 |
| 性格 | 狡黠高冷，每周只来 2-3 次，但带来的东西都稀有 |
| 每周礼物 | 野生薄荷、百里香、薰衣草、洋甘菊中随机 1 种 ×2 |
| 出现规律 | 每周一/三/五出现在密林边缘，其余天不在 |
| 好感满级 | 亲密 → 带玩家找到 1 个隐藏宝藏点（随机获得金币/稀有物品） |
| 投喂偏好 | 最爱：鸡蛋、牛奶；喜欢：肉类料理；普通：水果；不喜欢：蔬菜 |

### 4.4 小动物好感度系统（双轨制）

小动物的好感度比人物 NPC 更简单，共 3 级。采用**双轨制**：每日领取礼物即可稳定涨好感，投喂可加速。

#### 好感等级与效果

| 好感等级 | 名称 | 所需好感值 | 每日礼物数量 | 额外效果 |
|---------|------|-----------|------------|---------|
| Lv1 | 陌生 | 0 | ×2 | 基础 |
| Lv2 | 熟悉 | 30 | ×3 | 季节加成 +50% |
| Lv3 | 亲密 | 80 | ×4 | 每周额外 1 个稀有食材 |

#### 好感值来源（双轨制）

| 来源 | 好感值 | 说明 |
|------|--------|------|
| 每日领取礼物 | +2 | 当天去领了礼物就有，鼓励每天登录 |
| 每日投喂 | +1~+5 | 最爱 +5、喜欢 +3、普通 +1、不喜欢 0 |

#### 升级速度参考

| 路线 | Lv1→Lv2 (30点) | Lv2→Lv3 (80点) | 总计 |
|------|----------------|----------------|------|
| 只领礼物不投喂（每天 +2） | 15 天 | 再 25 天 | 40 天 |
| 每天投喂最爱（每天 +7） | 5 天 | 再 8 天 | 13 天 |
| 每天投喂喜欢（每天 +5） | 6 天 | 再 10 天 | 16 天 |

#### 投喂规则

| 规则 | 说明 |
|------|------|
| 每日上限 | 每只动物每天可投喂 1 次 |
| 投喂物品 | 背包中的作物、饮品、蛋糕等 |
| 好感值变化 | 最爱 +5、喜欢 +3、普通 +1、不喜欢 0 |
| 刷新时间 | 每日 0 点重置投喂次数 |
| 每日礼物 | 不需要投喂也会自动出现，领礼物 +2 好感，投喂只额外加速 |

### 4.5 新增食材品类

小动物系统引入以下新食材：

| 品类 | 食材 | 来源 |
|------|------|------|
| 坚果类 | 榛果、松子、栗子、核桃 | 小松鼠每日礼物 |
| 菌菇类 | 口蘑、香菇、鸡油菌、松露 | 刺猬每日礼物（松露仅好感满级） |
| 野浆果 | 蓝莓、树莓、桑葚、黑莓 | 知更鸟每日礼物 |
| 野生香草 | 薄荷、百里香、薰衣草、洋甘菊 | 小狐狸每周礼物 |
| 稀有食材 | 金松子、金浆果、松露 | 对应动物好感满级每周额外产出 |

## 5. 菌菇类食谱

### 5.1 菌菇简介

四种菌菇由刺猬每日产出，品质从普通到稀有递增。松露仅刺猬好感 Lv3（亲密）后才有概率产出，是最稀有的食材之一。

| 菌菇 | 稀有度 | 季节 | 来源 |
|------|--------|------|------|
| 口蘑 | 普通 | 春季 | 刺猬每日礼物 |
| 香菇 | 普通 | 夏季 | 刺猬每日礼物 |
| 鸡油菌 | 稀有 | 秋季 | 刺猬每日礼物 |
| 松露 | 极品 | 不限季节 | 刺猬好感 Lv3 每周额外产出 |

### 5.2 配方商店

菌菇类配方统一在交易所的"配方商店"出售，用金币购买。配方随时可买，但能否制作取决于玩家是否拥有对应食材（如松露配方可早买，但松露只有刺猬好感满级后才产出）。

联动配方（跨动物食材）不放入商店，作为对应动物好感度升级的奖励自动解锁。

### 5.3 饮品店配方（配方商店购买）

| 配方名 | 材料 | 购买价格 | 制作售价 | 经验 |
|--------|------|---------|---------|------|
| 蘑菇茶 | 香菇×2 + 薄荷×1 | 200 金币 | 45 | 12 |
| 口蘑奶昔 | 口蘑×2 + 牛奶×1 + 蜂蜜×1 | 300 金币 | 60 | 15 |
| 鸡油菌浓汤 | 鸡油菌×2 + 牛奶×1 + 口蘑×1 | 600 金币 | 90 | 22 |
| 松露热可可 | 松露×1 + 牛奶×1 + 蜂蜜×1 | 1500 金币 | 180 | 40 |

### 5.4 蛋糕店配方（配方商店购买）

| 配方名 | 材料 | 购买价格 | 制作售价 | 经验 |
|--------|------|---------|---------|------|
| 鸡油菌蛋挞 | 鸡油菌×2 + 小麦×1 + 鸡蛋×1 + 牛奶×1 | 500 金币 | 120 | 25 |
| 蘑菇松露派 | 口蘑×2 + 松露×1 + 小麦×2 + 鸡蛋×1 | 1200 金币 | 200 | 45 |
| 松露蛋糕 | 松露×1 + 小麦×2 + 鸡蛋×1 + 牛奶×1 | 2000 金币 | 280 | 60 |
| 香菇栗子蛋糕 | 香菇×1 + 栗子×2 + 小麦×2 + 鸡蛋×1 | 400 金币 | 150 | 30 |

### 5.5 联动配方（好感度奖励解锁，非商店购买）

跨动物食材配方不放入商店，作为动物好感度升级时的奖励自动获得：

| 配方名 | 材料 | 来源动物 | 解锁条件 | 制作售价 | 经验 |
|--------|------|---------|---------|---------|------|
| 菌菇坚果浓汤 | 口蘑×2 + 榛果×1 + 牛奶×1 | 刺猬+松鼠 | 刺猬 Lv2 + 松鼠 Lv2 | 160 | 35 |
| 浆果松露挞 | 松露×1 + 蓝莓×2 + 小麦×2 + 鸡蛋×1 | 刺猬+知更鸟 | 刺猬 Lv3 + 知更鸟 Lv2 | 320 | 70 |
| 香草菌菇茶 | 香菇×2 + 薄荷×1 + 蜂蜜×1 | 刺猬+狐狸 | 刺猬 Lv2 + 狐狸 Lv2 | 100 | 20 |

## 6. 人物 NPC 好感度系统

### 6.1 好感度等级

每个人物 NPC 有独立的好感度条，共 5 级：

| 好感等级 | 名称 | 所需好感值 | 解锁内容 |
|---------|------|-----------|---------|
| Lv1 | 路人 | 0 | 基础对话、摊位功能开放 |
| Lv2 | 熟客 | 20 | 每日小任务（送指定物品即可） |
| Lv3 | 朋友 | 40 | 限定配方或稀有种子 |
| Lv4 | 挚友 | 70 | 专属装饰物 + NPC 主动送礼物 |
| Lv5 | 知己 | 100 | 隐藏区域解锁 + 专属故事结局 |

### 6.2 赠送规则

| 规则 | 说明 |
|------|------|
| 每日上限 | 每个 NPC 每天可赠送 1 次 |
| 赠送物品 | 作物、饮品、蛋糕、蜂蜜、鱼获等背包物品 |
| 好感值变化 | 最爱 +15、喜欢 +8、普通 +3、不喜欢 +1 |
| 刷新时间 | 每日 0 点重置赠送次数 |
| 不可赠送 | 种子、工具、装饰物、任务物品 |

### 6.3 NPC 偏好表

| NPC | 最爱 | 喜欢 | 普通 | 不喜欢 |
|-----|------|------|------|--------|
| 阿海 | 咖啡、烤鱼 | 果汁、茶 | 水果、蔬菜 | 甜点、蛋糕 |
| 小铃 | 果酱、果酒 | 水果、果汁 | 蔬菜、蛋糕 | 鱼、鸡蛋 |
| 花姨 | 蜂蜜、花束 | 水果、茶 | 蔬菜、果汁 | 鱼、烤肉 |
| 糖糖 | 蛋糕、布丁 | 甜品、果酒 | 水果、果汁 | 苦味饮品、蔬菜 |
| 小灰 | 面包、小麦 | 水果、种子 | 蔬菜、果汁 | 鱼、烤肉 |

### 6.4 好感度回报明细

#### 阿海

| 等级 | 回报 |
|------|------|
| Lv2 | 每日任务：送一条鱼给阿海，回报金币 |
| Lv3 | 解锁钓鱼功能（河边钓点） |
| Lv4 | 赠送稀有鱼竿装饰 + 每周三送一条稀有鱼 |
| Lv5 | 解锁密林深处的"秘密钓点"区域 |

#### 小铃

| 等级 | 回报 |
|------|------|
| Lv2 | 每日任务：送指定物品，回报折扣券 |
| Lv3 | 商店解锁稀有种子栏位（薰衣草、向日葵等） |
| Lv4 | 解锁限定装饰物（铃铛风灯、旅行帐篷摆件） |
| Lv5 | 商店概率刷出"神秘盲盒"（随机稀有物品） |

#### 花姨

| 等级 | 回报 |
|------|------|
| Lv2 | 每日任务：送花给花姨，回报花种 |
| Lv3 | 解锁温室功能（可种菠萝等热带作物）+ 嫁接功能（两种花合成新品种） |
| Lv4 | 赠送稀有花种 + 花园产出速度加成 5% |
| Lv5 | 解锁密林深处的"秘密花园"区域 |

#### 糖糖

| 等级 | 回报 |
|------|------|
| Lv2 | 每日任务：制作指定甜品给糖糖品鉴，回报经验 |
| Lv3 | 解锁星级配方（三星蛋糕、五星甜品） |
| Lv4 | 糖糖给你的甜品打高分时，交易所售价 +10% |
| Lv5 | 解锁"甜品大赛"周常活动 |

#### 小灰

| 等级 | 回报 |
|------|------|
| Lv2 | 每日任务：送面包/小麦，回报随机来信（含小礼物） |
| Lv3 | 解锁每日 3 封信，每封信附带一个小任务 |
| Lv4 | 赠送邮票收藏册装饰 + 任务奖励翻倍 |
| Lv5 | 解锁"漂流瓶"系统（随机收到其他玩家/NPC 的信） |

## 7. 隐藏互动区域

密林深处（Y=1-4）隐藏 4 个互动区域，通过 NPC 好感度 + 玩家等级 + 砍树费解锁：

### 7.1 区域总览

| 区域 | 解锁条件 | 核心功能 |
|------|---------|---------|
| 温泉 | 阿海 Lv3 + 玩家 Lv8 + 200 金币 | 恢复体力、加速作物生长 |
| 秘密钓点 | 阿海 Lv5 + 玩家 Lv10 + 300 金币 | 钓稀有鱼，解锁鱼系配方 |
| 秘密花园 | 花姨 Lv5 + 玩家 Lv9 + 250 金币 | 种稀有花，产特殊香料 |
| 精灵树 | 糖糖 Lv5 + 小灰 Lv5 + 玩家 Lv12 + 500 金币 | 每日许愿，概率获稀有物品 |

### 7.2 温泉

| 属性 | 内容 |
|------|------|
| 位置 | 密林左上方 |
| 功能 | 每天泡一次温泉，6 小时内作物生长速度 +15% |
| 视觉 | 冒热气的石头水池，旁边有木桶和毛巾 |
| 额外 | 泡温泉时播放治愈音效，画面柔化 |

### 7.3 秘密钓点

| 属性 | 内容 |
|------|------|
| 位置 | 密林左上方（温泉旁的山涧溪流） |
| 功能 | 钓鱼小游戏：点击 → 等待咬钩 → QTE 拉杆 → 获鱼 |
| 鱼种 | 普通鱼（鲫鱼/鲤鱼）、稀有鱼（彩虹鳟/银鱼）、特殊鱼（夜间限定） |
| 用途 | 鱼可直接送给阿海或做成烤鱼菜品 |

### 7.4 秘密花园

| 属性 | 内容 |
|------|------|
| 位置 | 密林右上方 |
| 功能 | 种植稀有花卉（月光花、星辰花、七色堇），花期 3-7 天 |
| 产出 | 花瓣可做香料、花束、染料 |
| 联动 | 花姨好感度 Lv4 后，秘密花园产出速度 +5% |

### 7.5 精灵树

| 属性 | 内容 |
|------|------|
| 位置 | 密林正中央 |
| 功能 | 每日许愿一次，从三种愿望中选一种 |
| 愿望类型 | "想要材料"（随机作物/鱼/花）、"想要金币"（小额）、"想要惊喜"（概率稀有物品） |
| 视觉 | 发光的大树，树洞里有小精灵 |
| 限制 | 每日只能许一次，不可刷新 |

## 8. 装饰区

装饰区位于主干道和交易所之间，纯视觉无功能，玩家可自由摆放装饰物。

### 8.1 装饰物来源

| 来源 | 装饰物 |
|------|--------|
| NPC 好感回报 | 铃铛风灯、花架、鱼竿摆件、邮票相框、品鉴桌 |
| 小铃商店 | 长椅、花坛、喷泉、路灯、篱笆、风车 |
| 活动奖励 | 节日限定装饰（圣诞树、灯笼等） |

### 8.2 摆放规则

- 装饰区有固定槽位（约 12 个），每个槽位放一个装饰物。
- 装饰物可随时更换，不消耗资源。
- 摆放结果存入服务端，换设备登录后保持一致。
- 其他玩家串门时可以看到对方的装饰布局。

## 9. 花卉与蜂蜜系统

### 9.1 概述

花卉是商店购买的新种子品类（不通过岛屿升级赠送），可食用，有三重用途：配方材料、产蜜原料、NPC 赠礼。蜂蜜是单一物品，由蜂箱根据玩家种植的花卉自动产出。

### 9.2 花卉种子（商店购买，无等级限制）

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
- 花卉可升级 Lv1-10（后续可扩展），升级提升产量和产蜜倍率
- 产量倍率：`1 + 0.3 × (level - 1)`，上限 3.0
- 产蜜倍率：`min(1 + 0.4 × (level - 1)^0.85, 5.0)`

### 9.3 蜂蜜产出机制

```
每周期产蜜量 = floor(Σ（每种花: 数量 × 产蜜系数 × 等级倍率))
```

| 规则 | 说明 |
|------|------|
| 产蜜周期 | 每 2 小时结算一次 |
| 结算条件 | 遍历所有已成熟的花卉，未成熟的不计入 |
| 无花处理 | 没有已成熟花卉时不产蜜 |
| 存储上限 | 蜂箱存储满时停止产蜜（不溢出） |
| 蜂箱上限 | 每玩家最多 3 个，存储上限 20/40/60 |
| 蜂箱价格 | 1000 / 2000 / 3000 金币 |

蜂蜜不分种类，是单一物品（item_id: honey），所有花卉产出的蜂蜜相同。

### 9.4 花卉与蜂蜜配方

16 个配方（8 饮品 + 8 蛋糕），全部在交易所配方商店用金币购买。详见 `demo3.0-economy-balance-sheet.md` 第 15 节。

### 9.5 与其他系统的联动

| 系统 | 联动方式 |
|------|---------|
| 花姨 NPC | 花姨最爱蜂蜜和花束；好感 Lv3 解锁温室（种菠萝等热带作物）；好感 Lv4 花园产出 +5% |
| 秘密花园 | 隐藏区域，花姨好感 Lv5 解锁，可种稀有花卉（月光花、星辰花等） |
| 菌菇配方 | 多个菌菇配方需要蜂蜜（口蘑奶昔、松露热可可等） |
| 小动物 | 花卉可作为投喂礼物；知更鸟喜欢浆果类花卉 |
| 装饰区 | 花坛可摆放已种植的花卉作为装饰 |

## 10. 数据结构

### 10.1 人物 NPC 配置表

```sql
CREATE TABLE npc_config (
  id BIGINT PRIMARY KEY,
  npc_code VARCHAR(32) NOT NULL COMMENT 'NPC 唯一标识: ahai/xiaoling/huayi/tangtang/xiaohui',
  display_name VARCHAR(32) NOT NULL COMMENT '展示名',
  stall_name VARCHAR(64) NOT NULL COMMENT '摊位名称',
  position_x INT NOT NULL COMMENT '地图 X 坐标',
  position_y INT NOT NULL COMMENT '地图 Y 坐标',
  avatar VARCHAR(128) COMMENT '头像资源路径',
  dialog_default TEXT COMMENT '默认对话',
  unlock_level INT DEFAULT 1 COMMENT '玩家等级解锁',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE npc_friendship_level_config (
  id BIGINT PRIMARY KEY,
  npc_code VARCHAR(32) NOT NULL,
  friendship_level INT NOT NULL COMMENT '好感等级 1-5',
  required_value INT NOT NULL COMMENT '达到该等级所需好感值',
  level_name VARCHAR(32) NOT NULL COMMENT '等级名称',
  reward_type VARCHAR(32) COMMENT '奖励类型: recipe/seed/decoration/area/feature',
  reward_value VARCHAR(128) COMMENT '奖励内容标识',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE npc_gift_preference (
  id BIGINT PRIMARY KEY,
  npc_code VARCHAR(32) NOT NULL,
  item_id VARCHAR(64) NOT NULL COMMENT '物品标识',
  preference_type VARCHAR(16) NOT NULL COMMENT 'favorite/like/neutral/dislike',
  friendship_gain INT NOT NULL COMMENT '好感值变化',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 10.2 玩家与人物 NPC 数据表

```sql
CREATE TABLE player_npc_friendship (
  id BIGINT PRIMARY KEY,
  player_id BIGINT NOT NULL,
  npc_code VARCHAR(32) NOT NULL,
  friendship_value INT DEFAULT 0 COMMENT '当前好感值',
  friendship_level INT DEFAULT 1 COMMENT '当前好感等级',
  today_gifted TINYINT DEFAULT 0 COMMENT '今日是否已赠送: 0/1',
  last_gift_date DATE COMMENT '最后赠送日期',
  total_gifts INT DEFAULT 0 COMMENT '累计赠送次数',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_player_npc (player_id, npc_code)
);

CREATE TABLE player_npc_gift_log (
  id BIGINT PRIMARY KEY,
  player_id BIGINT NOT NULL,
  npc_code VARCHAR(32) NOT NULL,
  item_id VARCHAR(64) NOT NULL,
  item_count INT NOT NULL,
  friendship_before INT NOT NULL,
  friendship_after INT NOT NULL,
  preference_type VARCHAR(16) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 10.3 小动物 NPC 配置表

```sql
CREATE TABLE animal_npc_config (
  id BIGINT PRIMARY KEY,
  animal_code VARCHAR(32) NOT NULL COMMENT '动物标识: squirrel/hedgehog/robin/fox',
  display_name VARCHAR(32) NOT NULL COMMENT '展示名',
  habitat_name VARCHAR(64) NOT NULL COMMENT '栖息地名称',
  position_x INT NOT NULL COMMENT '栖息地 X 坐标',
  position_y INT NOT NULL COMMENT '栖息地 Y 坐标',
  avatar VARCHAR(128) COMMENT '头像资源路径',
  visit_schedule VARCHAR(64) COMMENT '出现时间: daily/mon_wed_fri',
  unlock_level INT DEFAULT 1 COMMENT '玩家等级解锁',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE animal_gift_config (
  id BIGINT PRIMARY KEY,
  animal_code VARCHAR(32) NOT NULL,
  item_id VARCHAR(64) NOT NULL COMMENT '礼物物品标识',
  season VARCHAR(16) COMMENT '季节限定: spring/summer/autumn/winter/all',
  base_count INT NOT NULL COMMENT '基础数量',
  friendship_level_required INT DEFAULT 1 COMMENT '需要的好感等级',
  is_rare TINYINT DEFAULT 0 COMMENT '是否稀有礼物(好感满级产出)',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE animal_feed_preference (
  id BIGINT PRIMARY KEY,
  animal_code VARCHAR(32) NOT NULL,
  item_id VARCHAR(64) NOT NULL,
  preference_type VARCHAR(16) NOT NULL COMMENT 'favorite/like/neutral/dislike',
  friendship_gain INT NOT NULL COMMENT '好感值变化',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 10.4 玩家与小动物数据表

```sql
CREATE TABLE player_animal_friendship (
  id BIGINT PRIMARY KEY,
  player_id BIGINT NOT NULL,
  animal_code VARCHAR(32) NOT NULL,
  friendship_value INT DEFAULT 0 COMMENT '当前好感值',
  friendship_level INT DEFAULT 1 COMMENT '当前好感等级 1-3',
  today_fed TINYINT DEFAULT 0 COMMENT '今日是否已投喂: 0/1',
  last_fed_date DATE COMMENT '最后投喂日期',
  today_gift_claimed TINYINT DEFAULT 0 COMMENT '今日礼物是否已领取: 0/1',
  last_gift_date DATE COMMENT '最后领礼物日期',
  total_gifts_claimed INT DEFAULT 0 COMMENT '累计领取礼物次数',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_player_animal (player_id, animal_code)
);

CREATE TABLE player_animal_gift_log (
  id BIGINT PRIMARY KEY,
  player_id BIGINT NOT NULL,
  animal_code VARCHAR(32) NOT NULL,
  item_id VARCHAR(64) NOT NULL COMMENT '收到的物品',
  item_count INT NOT NULL,
  is_rare TINYINT DEFAULT 0 COMMENT '是否稀有礼物',
  season VARCHAR(16) COMMENT '产出季节',
  claim_date DATE NOT NULL COMMENT '领取日期',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 10.5 配方商店表

```sql
CREATE TABLE recipe_shop_config (
  id BIGINT PRIMARY KEY,
  recipe_id VARCHAR(64) NOT NULL COMMENT '配方标识',
  recipe_name VARCHAR(64) NOT NULL COMMENT '配方名称',
  shop_type VARCHAR(16) NOT NULL COMMENT '配方类型: drink/cake',
  price INT NOT NULL COMMENT '购买价格(金币)',
  category VARCHAR(32) NOT NULL COMMENT '分类: mushroom/nut/berry/herb/basic',
  sort_order INT DEFAULT 0 COMMENT '排序',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_recipe (recipe_id)
);

CREATE TABLE player_recipe_purchase (
  id BIGINT PRIMARY KEY,
  player_id BIGINT NOT NULL,
  recipe_id VARCHAR(64) NOT NULL COMMENT '购买的配方标识',
  price_paid INT NOT NULL COMMENT '实际支付价格',
  purchased_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_player_recipe (player_id, recipe_id)
);
```

### 10.6 花卉与蜂箱数据表

```sql
CREATE TABLE flower_config (
  id BIGINT PRIMARY KEY,
  flower_id VARCHAR(32) NOT NULL COMMENT '花卉标识: rose/chrysanthemum/jasmine/osmanthus/lavender/roselle/chamomile/sakura',
  display_name VARCHAR(32) NOT NULL,
  purchase_currency VARCHAR(16) NOT NULL COMMENT 'gold/diamond',
  seed_price INT NOT NULL COMMENT '种子价格',
  grow_seconds INT NOT NULL COMMENT '生长时间(秒)',
  base_yield INT NOT NULL COMMENT '基础产量',
  harvest_exp INT NOT NULL COMMENT '收获经验',
  honey_coefficient DECIMAL(3,1) NOT NULL COMMENT '产蜜系数: 金币花=1.0, 钻石花=2.0',
  sell_price INT NOT NULL COMMENT '出售价',
  max_level INT DEFAULT 10 COMMENT '最大等级(可扩展)',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_flower (flower_id)
);

CREATE TABLE flower_level_config (
  id BIGINT PRIMARY KEY,
  flower_id VARCHAR(32) NOT NULL,
  flower_level INT NOT NULL COMMENT '花卉等级 1-10',
  upgrade_cost INT NOT NULL COMMENT '升级费用',
  yield_multiplier DECIMAL(3,1) NOT NULL COMMENT '产量倍率',
  honey_multiplier DECIMAL(3,2) NOT NULL COMMENT '产蜜倍率(公式计算后存入)',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_flower_level (flower_id, flower_level)
);

CREATE TABLE player_flower (
  id BIGINT PRIMARY KEY,
  player_id BIGINT NOT NULL,
  flower_id VARCHAR(32) NOT NULL,
  flower_level INT DEFAULT 1 COMMENT '花卉等级',
  planted_at DATETIME COMMENT '种植时间',
  land_slot INT NOT NULL COMMENT '土地槽位',
  is_mature TINYINT DEFAULT 0 COMMENT '是否已成熟',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE player_beehive (
  id BIGINT PRIMARY KEY,
  player_id BIGINT NOT NULL,
  slot_index INT NOT NULL COMMENT '蜂箱槽位 1-3',
  purchased_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_player_slot (player_id, slot_index)
);

CREATE TABLE player_honey_storage (
  id BIGINT PRIMARY KEY,
  player_id BIGINT NOT NULL,
  honey_count INT DEFAULT 0 COMMENT '当前蜂蜜存储量',
  max_storage INT DEFAULT 20 COMMENT '存储上限(取决于蜂箱数量)',
  last_produced_at DATETIME COMMENT '上次产蜜结算时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_player (player_id)
);
```

产蜜倍率公式（Java 实现参考）：

```java
public static double honeyMultiplier(int flowerLevel) {
    return Math.min(1 + 0.4 * Math.pow(flowerLevel - 1, 0.85), 5.0);
}
```

### 10.7 隐藏区域解锁表

```sql
CREATE TABLE player_hidden_area (
  id BIGINT PRIMARY KEY,
  player_id BIGINT NOT NULL,
  area_code VARCHAR(32) NOT NULL COMMENT '区域标识: hot_spring/secret_fishing/secret_garden/spirit_tree',
  unlocked TINYINT DEFAULT 0 COMMENT '是否已解锁',
  unlocked_at DATETIME COMMENT '解锁时间',
  daily_used TINYINT DEFAULT 0 COMMENT '今日是否已使用',
  last_used_date DATE COMMENT '最后使用日期',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_player_area (player_id, area_code)
);
```

## 11. API 设计

### 11.1 人物 NPC

```
GET  /npc/list                      获取全部 NPC 列表及好感度状态
GET  /npc/{npcCode}/detail          获取 NPC 详情（对话、偏好、当前好感度回报）
POST /npc/{npcCode}/gift            赠送物品给 NPC
GET  /npc/{npcCode}/daily-task      获取 NPC 今日任务
POST /npc/{npcCode}/daily-task/complete  完成今日任务
GET  /npc/xiaoling/shop             获取小铃商店商品列表
POST /npc/xiaoling/shop/buy         购买商品
POST /npc/tangtang/evaluate         提交甜品品鉴
```

### 11.2 小动物 NPC

```
GET  /animal/list                   获取全部小动物列表及好感度状态
GET  /animal/{animalCode}/detail    获取小动物详情（栖息地、今日礼物状态、偏好）
POST /animal/{animalCode}/claim     领取今日礼物
  响应: {
    itemId: "hazelnut",
    count: 2,
    isRare: false,
    friendshipGain: 2,          // 领礼物固定 +2 好感
    friendshipBefore: 10,
    friendshipAfter: 12,
    friendshipLevel: 1,
    levelUp: false
  }
POST /animal/{animalCode}/feed      投喂食物给小动物
  请求体: { itemId: "blueberry", count: 1 }
  响应: {
    preference: "favorite",      // favorite/like/normal/dislike
    friendshipGain: 5,            // 最爱+5/喜欢+3/普通+1/不喜欢+0
    friendshipBefore: 12,
    friendshipAfter: 17,
    levelUp: false,
    friendshipLevel: 1
  }
```

### 11.3 配方商店

```
GET  /recipe-shop/list              获取配方商店全部配方列表（含是否已购买）
  响应: [{
    recipeId: "mushroom_tea",
    recipeName: "蘑菇茶",
    shopType: "drink",
    price: 200,
    category: "mushroom",
    purchased: false
  }]
POST /recipe-shop/buy               购买配方
  请求体: { recipeId: "mushroom_tea" }
  响应: {
    success: true,
    goldRemaining: 1800,
    recipeId: "mushroom_tea"
  }
```

### 11.4 隐藏区域

```
GET  /area/hidden/list                  获取隐藏区域解锁状态
POST /area/hidden/{areaCode}/unlock     解锁隐藏区域（消耗金币）
POST /area/hidden/{areaCode}/use        使用隐藏区域功能（每日一次）
```

### 11.5 装饰

```
GET  /decoration/slots              获取装饰区槽位列表
POST /decoration/place              摆放装饰物
POST /decoration/remove             移除装饰物
```

### 11.6 花卉与蜂蜜

```
GET  /flower/seeds                  获取商店可购买的花卉种子列表
POST /flower/buy-seed               购买花卉种子
  请求体: { flowerId: "rose", count: 1 }
GET  /flower/list                   获取玩家已种植花卉列表
POST /flower/plant                  种植花卉
  请求体: { flowerId: "rose", landSlot: 0 }
POST /flower/harvest                收获成熟花卉
  请求体: { landSlot: 0 }
POST /flower/upgrade                升级花卉等级
  请求体: { flowerId: "rose" }

GET  /beehive/list                  获取蜂箱列表和蜂蜜存储状态
POST /beehive/buy                   购买蜂箱
POST /beehive/collect               收取蜂蜜
  响应: {
    honeyCollected: 12,
    honeyRemaining: 0,
    maxStorage: 40,
    nextProductionAt: "2026-08-01T04:00:00"
  }
GET  /beehive/production-preview     预览下次产蜜量
  响应: {
    estimatedHoney: 12,
    flowers: [
      { flowerId: "rose", count: 3, level: 5, contribution: 6.93 },
      { flowerId: "chrysanthemum", count: 2, level: 1, contribution: 2.00 },
      { flowerId: "sakura", count: 1, level: 3, contribution: 3.44 }
    ]
  }
```

## 12. 实现优先级

### Phase 1 — 人物 NPC 骨架（最小可玩）

- 5 个 NPC 摊位在地图上出现
- 好感度系统（赠送 + 好感值 + 等级升级）
- NPC 对话系统（基础台词 + 好感度分级台词）
- 每日赠送限制和重置

### Phase 2 — 小动物 NPC

- 4 只小动物栖息地在地图上出现
- 每日礼物自动生成 + 领取机制
- 投喂 + 好感度系统（3 级）
- 季节加成逻辑
- 新增食材进入背包和配方系统

### Phase 3 — 回报系统

- 好感度等级解锁配方/种子
- 小铃商店基础版
- 糖糖品鉴系统
- 小灰每日任务系统
- 花姨温室功能（种菠萝）

### Phase 4 — 菌菇与联动配方

- 菌菇类 8 个配方上线（4 饮品 + 4 甜品）
- 联动配方上线（跨动物食材）
- 稀有食材（松露/金松子/金浆果）解锁条件验证

### Phase 5 — 花卉与蜂蜜系统

- 8 种花卉种子上线（7 金币 + 1 钻石，商店购买无等级限制）
- 花卉种植、收获、升级（Lv1-10，公式驱动倍率）
- 蜂箱购买（最多 3 个，存储上限 20/40/60）
- 蜂蜜产出引擎（2 小时周期，遍历成熟花卉计算产量）
- 16 个花卉配方上线（8 饮品 + 8 蛋糕，配方商店金币购买）

### Phase 6 — 隐藏区域

- 温泉解锁和加速效果
- 秘密钓点和钓鱼小游戏
- 秘密花园和稀有花卉
- 精灵树许愿系统

### Phase 7 — 装饰区

- 装饰区槽位和摆放
- 装饰物来源（商店 + 好感回报）
- 串门可见

## 13. 与现有系统的衔接

| 现有系统 | 衔接方式 |
|---------|---------|
| 作物种植 | 作物是 NPC 赠送的主要物品来源 |
| 饮品制作 | 饮品可作为礼物（咖啡→阿海，果汁→小铃）；菌菇饮品新增 4 个配方 |
| 蛋糕制作 | 蛋糕是糖糖品鉴和赠送的核心物品；菌菇甜品新增 4 个配方 |
| 蜂巢蜂蜜 | 蜂箱根据花卉产出蜂蜜；蜂蜜是花姨的最爱；菌菇和花卉配方需要蜂蜜 |
| 鸡蛋牛奶 | 面包/蛋糕原料；小狐狸最爱鸡蛋牛奶 |
| 背包系统 | 小动物礼物直接进背包；新增坚果/菌菇/浆果/香草品类 |
| 交易所 | 糖糖好感 Lv4 后影响交易所售价 |
| 等级系统 | NPC 解锁和隐藏区域解锁依赖玩家等级 |
| 砍树系统 | 隐藏区域通过砍树解锁，复用现有砍树机制 |
| 季节系统 | 小动物礼物随季节变化（待季节系统实现后对接） |

## 14. 不包含

- 探索岛和双岛结构（P7）
- NPC 语音和配音
- NPC 之间的交叉互动剧情
- NPC 季节性换装
- 装饰物的自由旋转和缩放
- 多语言 NPC 对话
- 小动物之间的互动（各自独立）
- 小动物可被领养/带回家（始终在栖息地）
