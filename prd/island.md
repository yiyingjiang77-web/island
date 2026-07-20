# 海岛日记 — 产品规格书 (PRD)

## 版本

MVP v1.0 — 微信小程序

---

## Problem Statement

现代人需要一个安全的情绪出口。现有社交产品要么完全公开（朋友圈/微博）——让人不敢说真话；要么完全私密（备忘录/日记 App）——写了无人回应，孤独感加倍。用户想表达心事、想被看见和理解、想建立有温度的连接，但不想暴露身份、不想被评判、不想被算法绑架。

---

## Solution

**海岛日记**是一个以"治愈海岛"为场景的情绪社交游戏。

核心循环：**写日记 → 编辑 → 选择三种载体之一将心情送出 → 被别人发现 → 匿名回复 → 互聊 → 双向同意后成为好友**。

三载体设计：
- 🪨 **海玻璃**：沙滩捡拾获得，每日免费，送达 1 人，24h 后字迹消失，在沙滩上被人捡起
- 🦀 **小螃蟹**：沙滩随机出没，免费，每天 1-2 次，它主动爬过来送/取心情，送达 1 人，24h 后字迹消失，在沙滩上被人捡起
- 🌸 **信使花**：元宝购买种子（3元宝/颗） → 花园种植 → 开花后书写，花色匹配心情标签，送达 3 人，48h 后花瓣凋谢，在海中需要打捞。MVP 阶段赠送种子用户元宝用于体验

支撑系统：海岛上有菜园/花园/果园（种植收获售卖）、树林（探索与动物互动）、沙滩（捡贝壳/海玻璃、螃蟹心情快递）、宠物（陪伴与社交破冰）、世界广场（公开心情）、服装商店（装扮人偶）、珍珠兑换商店。所有活动通过情绪标签串联——你今天的心情像"岛上的天气"一样影响各处体验。
---
## User Stories
### 用户与身份
1. As a 新玩家, I want 创建一个正常人偶形象并起一个岛名, so that 我在岛上有一个代表自己的身份
2. As a 玩家, I want 在服装商店里用星币或元宝购买好看的服装/发型/配饰来装扮人偶, so that 我在岛上和邻居面前有自己的风格
3. As a 玩家, I want 在猫/狗/兔子中三选一作为宠物伙伴（选定不可更换）, so that 岛上有一个陪伴我的小生命
### 日记与心情
4. As a 玩家, I want 每天写日记，记录文字内容并选择一个心情标签（😊😢😡😰🥰 等）, so that 我的情绪有处可放
5. As a 玩家, I want 写日记时可选话题标签（#工作 #感情 #家庭 #成长 #随便聊聊 等）, so that 我的日记可以被更好地理解和匹配
6. As a 玩家, I want 回顾自己过去的日记和对应的心情, so that 看到自己的情绪轨迹

### 海玻璃（日常分享媒介，免费）

7. As a 玩家, I want 写完日记后选择用海玻璃分享→进入编辑态删减修改内容→将海玻璃放入大海, so that 我分享的内容是经过我确认的
8. As a 玩家, I want 每天在沙滩上捡到若干块海玻璃作为分享媒介（每日免费限量）, so that 我有稳定的日常心情出口
9. As a 玩家, I want 用海玻璃分享时选择匿名或实名, so that 我控制自己的隐私程度
10. As a 玩家, I want 用海玻璃分享时可选"同步到世界广场", so that 同一份心情可以触达更多人
11. As a 玩家, I want 海玻璃在海里漂流 24 小时后字迹自然消失, so that 分享是"当下的心情"，符合环保自然降解的理念

### 信使花（深度分享媒介，元宝兑换）

12. As a 玩家, I want 用元宝购买信使花种子（3元宝/颗）并种在花圃中→等开花→摘花→书写心情→放入大海, so that 我有更特别的高品质分享方式
13. As a 玩家, I want 将信使花种子种在花圃中→等开花→摘花→把心情写在花瓣上→放入水中飘走, so that 深度分享有仪式感
14. As a 玩家, I want 信使花的颜色自动匹配我当日的心情标签（😊=向日葵黄、😢=蓝玫瑰蓝、😰=薰衣草紫）, so that 接收者看到花色就能感受到我的情绪
15. As a 玩家, I want 信使花的花期保持 48 小时（比海玻璃的 24h 更长）, so that 珍珠媒介比免费媒介更持久
16. As a 玩家, I want 收到信使花的人回复后我获得额外友币奖励, so that 深度分享带来更多社交回报

### 发现与回复心情

17. As a 玩家, I want 系统根据心情标签优先匹配心情内容（海玻璃/螃蟹/信使花都适用）, so that 我发现的内容和我有情绪共鸣
18. As a 玩家, I want 每天在岸边收到 3 个随机心情（三种载体混合），来沙滩上可选择捡起或打捞或拒绝, so that 系统帮我发现可能感兴趣的人
19. As a 玩家, I want 在"海边"主动寻找海上的心情（随机匹配）, so that 我可以主动探索
20. As a 玩家, I want 捡到心情后阅读内容并选择回复（匿名）, so that 我和对方产生连接但保持安全距离
21. As a 玩家, I want 在匿名互聊中双方都不知道对方身份, so that 可以真诚交流而无社交压力
22. As a 玩家, I want 在匿名互聊中随时主动发起好友申请, so that 遇到投缘的人可以推进关系
23. As a 玩家, I want 互聊满 3 轮后系统自动弹窗询问双方"是否想认识这个人", so that 不会错过缘分
24. As a 玩家, I want 回复记录保留在对应心情的详情中, so that 我可以回顾之前的连接

### 世界广场

25. As a 玩家, I want 在世界广场浏览其他玩家公开的心情状态, so that 感受岛上其他岛民的情绪
26. As a 玩家, I want 对广场上的心情进行评论和回复, so that 参与公开的社交互动
27. As a 玩家, I want 在世界广场发言时可选择匿名或实名, so that 我控制自己的可见程度
28. As a 玩家, I want 拉黑骚扰或不适的用户, so that 我的社交空间是安全的

### 沙滩

29. As a 玩家, I want 在沙滩捡到不同品级的贝壳（小白贝/粉色扇贝/金色扇贝/紫色海螺）, so that 有收集和期待感
30. As a 玩家, I want 打开贝壳获得珍珠（品级越高珍珠越多，珍珠为稀缺资源）, so that 捡到稀有贝壳是真正的惊喜
31. As a 玩家, I want 在沙滩遇到随机出没的螃蟹，它爬过来带来周围岛民的心情并问"要打开吗？", so that 通过螃蟹这个信使发现有趣的陌生人
32. As a 玩家, I want 螃蟹带来的心情可以接受或拒绝, so that 我有选择权
33. As a 玩家, I want 写完心情后有三种发送方式可选：🦀让螃蟹带走（1人）/ 🪨用海玻璃放入大海（1人）/ 🌸用信使花飘走（3人）, so that 根据心情的深浅选择不同的媒介

### 珍珠兑换商店

32. As a 玩家, I want 在珍珠兑换商店里使用珍珠兑换各种物品, so that 珍珠有多种消费出口
33. As a 玩家, I want 在兑换商店中珍珠→星币按 1:10 固定汇率兑换, so that 我有选择如何花珍珠的自由
34. As a 玩家, I want 兑换商店货架包含：额外海玻璃 / 优先漂流券 / 加速肥料 / 稀有种子盲盒 / 珍珠限定装扮 / 珍珠→星币兑换, so that 商店内容丰富

### 菜园/花园/果园

35. As a 玩家, I want 初始拥有 2 块菜地 + 1 个花圃 + 1 个果树坑, so that 可以开始种植
36. As a 玩家, I want 随着等级提升免费解锁更多地块, so that 成长带来更多种植空间
37. As a 玩家, I want 种植蔬菜（短周期，小时级）、花卉（中周期，天级）、果树（长周期，种下后每日结果）, so that 有不同的种植策略
38. As a 玩家, I want 收菜时偶尔触发"稀有品种"弹窗，花费 50 友币可收获稀有品种, so that 社交货币有明确的消费出口
39. As a 玩家, I want 蔬菜被好友"摘取"时我不损失作物且获得友币, so that 被摘菜是开心的社交体验而非损失
40. As a 玩家, I want 随着作物升级被更多好友摘取（Lv1:1个、Lv3:2个、Lv5:3个）, so that 升级有社交回报
41. As a 玩家, I want 每日早上 7 点有一艘船来收购蔬菜/水果/鲜花/稀有品种（各有定价）支付星币, so that 种植有稳定的经济回报
42. As a 玩家, I want 新玩家获得 2000 星币和一些初始种子, so that 可以立刻开始种植和售卖

### 树林

43. As a 玩家, I want 主动找小野猪对话，它根据我当日心情先讲冷笑话安慰、再给出可验证的指引（"东边有蘑菇"→真的刷出蘑菇）, so that 对话有信任感和功能性
44. As a 玩家, I want 骑上小野猪在岛上兜风（MVP 纯视觉体验）, so that 有治愈的视觉体验
45. As a 玩家, I want 找小松鼠对话得知哪棵树上有松子→走过去摇树→松子掉落→收集卖珍珠, so that 树林探索有收获
46. As a 玩家, I want 小鸟主动飞来通知我社交动态（心情被回复/好友上线/邻居来访）, so that 社交信息以自然的方式传达
47. As a 玩家, I want 每天在树林里有一次寻宝机会获得随机物品, so that 有每日期待

### 宠物

48. As a 玩家, I want 宠物（猫/狗/兔）纯陪伴——在家和花园里自由走动、可以抚摸和喂食, so that 岛上有生命陪伴感
49. As a 玩家, I want 宠物偶尔出走一段时间→回来后带回好友种的菜（可能附带社交连接）, so that 宠物是社交破冰器
50. As a 玩家, I want 宠物出走时也给邻居家门口放一个小礼物, so that 宠物在无声中帮我交朋友
51. As a 玩家, I want 用星币装饰宠物的小窝, so that 我的宠物有独特的家

### 交友

52. As a 玩家, I want 三条路径交朋友：（1）海玻璃/信使花→匿名互聊→双向同意、（2）点击"添加好友"随机推荐 20 人→选人发申请、（3）在世界广场上看到发言→点头像发申请, so that 有多种社交拓展方式
53. As a 玩家, I want 友币用于扩展好友上限、兑换稀有品种激活、购买专属社交道具, so that 友币有不可替代的价值

### 经济系统

54. As a 玩家, I want 星币（一般等价物）购买基础款服装/装饰/宠物窝, so that 免费玩家有完整的装扮体验
55. As a 玩家, I want 元宝（付费货币，3元=30元宝）购买稀有/限定装饰和皮肤, so that 付费玩家有独特的视觉差异化
56. As a 玩家, I want 珍珠（稀缺资源，1珍珠=10星币）在兑换商店消费, so that 珍珠有多种策略性用途
57. As a 玩家, I want 星币不可换元宝、元宝不可换星币, so that 稀有物品保持稀缺性

### 岛屿与社交空间

58. As a 玩家, I want 我的海岛是一幅精美插画（旅行青蛙风格），有沙滩/树林/小窝/菜园等热点可点击进入, so that 岛屿有沉浸感和治愈感
59. As a 玩家, I want 去邻居的岛串门——看到对方的小窝装饰、花园、宠物窝、人偶装扮, so that 社交有空间载体
60. As a 玩家, I want 给邻居的菜地浇水/帮忙获得友币, so that 串门是互助而非竞争

---
## Implementation Decisions
### 平台架构
- 微信小程序（前端） + 云开发或独立后端（Node.js/Go）
- MVP 阶段先做单服务器架构，后续根据 DAU 拆分微服务
- 图片/静态资源走 CDN，日记内容走数据库
### 模块划分
| 模块 | 职责 | MVP 阶段 |
|---|---|---|
| `user` | 账号（微信登录）、身份、好友关系 | 完整 |
| `diary` | 日记 CRUD、心情标签、话题标签 | 完整 |
| `seaglass` | 海玻璃捡拾/书写/放入大海、日常免费媒介，送达 1 人 | 完整 |
| `crab` | 螃蟹心情快递——随机出没送来附近岛民心情、带走主人的心情送达 1 人 | 完整 |
| `messenger-flower` | 信使花种子（元宝购买）→种植→开花→书写→放入大海、付费媒介，送达 3 人 | 完整 |
| `drift` | 三载体心情匹配分发、捡起/打捞、24h/48h 生命周期管理、匿名回复 | 完整 |
| `chat` | 匿名对话线程、好友申请（主动+3轮弹窗） | 完整 |
| `square` | 世界广场发布、评论、回复、拉黑 | 完整 |
| `shore` | 每日 3 个心情推送、贝壳刷新、螃蟹刷新、海玻璃刷新 | 完整 |
| `forest` | 寻宝、动物互动（猪/松鼠/鸟）、蘑菇/松子采集 | 完整 |
| `garden` | 种植（菜/花/树）、收获、稀有触发、友币系统、信使花种植 | 完整 |
| `pet` | 宠物选择、陪伴状态、出走/回归逻辑、送礼 | 完整 |
| `island` | 岛屿插画主场景、热点导航（MVP 纯前端组件） | 完整 |
| `economy` | 星币/元宝/珍珠/友币账户、交易流水、船收菜定时任务 | 完整 |
| `payment` | 充值/支付接口 | 接口预留，不接真实支付 |
| `avatar` | 人偶换装系统（服装/发型/配饰） | 完整 |
| `shop` | 服装商店（星币区 + 元宝区）+ 珍珠兑换商店 + 友币商城 | 完整 |
### API 契约（核心端点）
所有接口通过 `/api/*` 网关层暴露给小程序前端。核心契约：
```
用户：
  POST   /api/user/auth             微信登录授权
  GET    /api/user/profile          获取个人信息
  PUT    /api/user/avatar           更新人偶装扮

日记：
  POST   /api/diary                 创建日记
  GET    /api/diary/:id             获取日记详情
  PUT    /api/diary/:id             编辑日记
  DELETE /api/diary/:id             删除日记
  GET    /api/diary/list            日记列表（按日期/心情筛选）

海玻璃（日常免费媒介）：
  GET    /api/seaglass/collect      捡拾海玻璃（每日上限）
  POST   /api/seaglass/send         书写心情并用海玻璃送出（送达 1 人）
  GET    /api/seaglass/list         我的海玻璃记录

螃蟹（免费心情信使）：
  GET    /api/crab/appear          检查螃蟹是否出没+它带来的心情
  POST   /api/crab/accept          接受螃蟹带来的心情
  POST   /api/crab/reject          拒绝螃蟹带来的心情
  POST   /api/crab/send            书写心情让螃蟹带走（送达 1 人，每日 1-2 次）

信使花（元宝媒介）：
  GET    /api/flower/seed-shop       信使花种子价格（3元宝/颗）
  POST   /api/flower/buy-seed        用元宝购买信使花种子
  POST   /api/flower/plant           种植信使花
  GET    /api/flower/growth          查看信使花生长状态
  POST   /api/flower/send            花开后书写心情并放入大海（送达 3 人）

海上心情发现与回复：
  GET    /api/drift/pick            从海上发现心情（心情匹配，信使花加权）
  GET    /api/drift/shore           每日岸边心情（海玻璃+螃蟹+信使花混合）
  POST   /api/drift/reply           回复发现的心情
  GET    /api/drift/detail/:id      心情详情+回复线程

匿名聊天：
  GET    /api/chat/thread/:id       获取匿名对话线程
  POST   /api/chat/send             发送匿名消息
  POST   /api/chat/friend-request   发起好友申请
  POST   /api/chat/friend-accept    同意好友申请

世界广场：
  GET    /api/square/feed           获取广场动态流
  POST   /api/square/post           发布心情到广场
  POST   /api/square/comment        评论
  POST   /api/square/block          拉黑

沙滩：
  GET    /api/shore/state           沙滩当前状态（贝壳/螃蟹/海玻璃）
  POST   /api/shore/collect-shell   捡贝壳/开珍珠

珍珠兑换商店：
  GET    /api/shop/pearl/items      兑换商店货架
  POST   /api/shop/pearl/buy        珍珠兑换商品
  POST   /api/shop/pearl/exchange   珍珠→星币兑换

树林：
  POST   /api/forest/treasure       每日寻宝
  POST   /api/forest/mushroom       小野猪指引后采集蘑菇
  POST   /api/forest/pinecone       摇树捡松子

花园：
  GET    /api/garden/state          花园当前状态
  POST   /api/garden/plant          种植
  POST   /api/garden/harvest        收获（含稀有触发判定）
  POST   /api/garden/pick           好友摘取（无损+友币）
  POST   /api/garden/boat-sell      船收菜出售

宠物：
  GET    /api/pet/state             宠物当前状态
  POST   /api/pet/choose            选择宠物（一次性）
  POST   /api/pet/feed              喂食
  GET    /api/pet/adventure         出走归来结果

好友：
  GET    /api/friend/list           好友列表
  POST   /api/friend/random         随机推荐 20 人
  POST   /api/friend/request        发送好友申请
  POST   /api/friend/accept         同意申请

经济：
  GET    /api/wallet                账户余额（星币/元宝/珍珠/友币）

服装商店：
  GET    /api/shop/clothing/items   服装商品列表（星币区+元宝区）
  POST   /api/shop/clothing/buy     购买服装
```

### 数据库核心表

```
users:
  id, openid, nickname, island_name, avatar_config,
  pet_type(enum:cat/dog/rabbit), garden_level, created_at

diaries:
  id, user_id, content, mood_tag, topic_tag, is_private, created_at

seaglasses:
  id, user_id, diary_id, content, mood_tag, topic_tag,
  is_anonymous, synced_to_square, status(floating/sunk/replied),
  expires_at(created_at + 24h), created_at

crab_messages:
  id, user_id(sender), diary_id, content, mood_tag, topic_tag,
  is_anonymous, synced_to_square, status(floating/sunk/replied),
  expires_at(created_at + 24h), daily_send_count, created_at

crab_visits:
  id, visitor_user_id, carrier_crab_id, from_user_id, content_preview,
  status(pending/accepted/rejected), created_at

messenger_flowers:
  id, user_id, diary_id, content, mood_tag, flower_color(matches mood),
  is_anonymous, synced_to_square, status(growing/bloomed/floating/sunk/replied),
  planted_at, bloomed_at, expires_at(bloomed_at + 48h), created_at

drift_replies:
  id, drift_id, drift_type(enum:seaglass/crab/flower), from_user_id,
  to_user_id, content, is_anonymous, created_at

chat_threads:
  id, drift_id, drift_type, user_a_id, user_b_id, round_count,
  friend_requested_by, friend_accepted_at, created_at

square_posts:
  id, user_id, content, mood_tag, is_anonymous, created_at

square_comments:
  id, post_id, user_id, content, created_at

garden_plots:
  id, user_id, plot_type(enum:veg/flower/tree), crop_id,
  plant_level, planted_at, harvest_ready_at

crops:
  id, name, type(veg/flower/tree), base_price, grow_duration,
  rare_multiplier

pets:
  id, user_id, pet_type, name, mood, is_away, away_until,
  return_gift, created_at

wallets:
  id, user_id, star_coin, yuanbao, pearl, friend_coin

transactions:
  id, user_id, currency_type, amount, reason, created_at

friends:
  id, user_a_id, user_b_id, status(pending/accepted/blocked),
  created_at

pearl_shop_items:
  id, name, description, price_pearl, category, stock_type

user_pearl_shop_purchases:
  id, user_id, item_id, purchased_at
```

### 珍珠兑换商店（完整货架）

| 分类 | 商品 | 珍珠价格 | 说明 |
|---|---|---|---|
| 🪨 社交 | 额外海玻璃次数 | 1 珍珠/次 | 超出每日免费上限后的额外分享次数 |
| 🎯 社交 | 优先漂流券 | 3 珍珠 | 下一次发出的海玻璃/螃蟹心情优先推送给 5 个心情匹配的人 |
| 🌱 种植 | 加速肥料 | 2 珍珠 | 缩短作物（不含信使花）生长时间 50% |
| 🌱 种植 | 稀有种子盲盒 | 5 珍珠 | 随机开出一种稀有蔬菜/花卉/果树种子 |
| 🧥 外观 | 珍珠限定装扮 | 10-50 珍珠 | 只能用珍珠购买的服装/配饰（星币和元宝都买不到） |
| 💱 兑换 | 珍珠 → 星币 | 1:10 汇率 | 1 珍珠兑换 10 星币 |

### 作物定价（船收菜）

**蔬菜（短周期，小时级收获）：**

| 作物 | 生长周期 | 收购价(星币) | 稀有品种收购价 |
|---|---|---|---|
| 白菜 | 2h | 5 | 50 |
| 胡萝卜 | 3h | 8 | 80 |
| 番茄 | 4h | 12 | 120 |
| 草莓 | 6h | 18 | 180 |
| 南瓜 | 8h | 25 | 250 |

**花卉（中周期，天级收获）：**

| 花卉 | 生长周期 | 收购价(星币) | 稀有品种收购价 |
|---|---|---|---|
| 雏菊 | 1d | 15 | 150 |
| 郁金香 | 2d | 30 | 300 |
| 向日葵 | 3d | 50 | 500 |
| 薰衣草 | 4d | 70 | 700 |
| 蓝玫瑰 | 5d | 100 | 1000 |

**果树（长周期，种下 7d 后每日结果）：**

| 果树 | 结果周期 | 每日产量 | 每颗收购价(星币) | 稀有品种收购价 |
|---|---|---|---|---|
| 苹果树 | 7d 成熟后每 1d | 2 颗 | 12 | 120 |
| 橘子树 | 7d 成熟后每 1d | 2 颗 | 15 | 150 |
| 樱桃树 | 7d 成熟后每 1d | 2 颗 | 18 | 180 |
| 芒果树 | 10d 成熟后每 1d | 1 颗 | 30 | 300 |
| 椰子树 | 10d 成熟后每 1d | 1 颗 | 40 | 400 |

### 贝壳品级与珍珠产出

| 贝壳 | 出现概率 | 珍珠产出 | 外观提示 |
|---|---|---|---|
| 小白贝 | 70% | 0-1 颗（50%概率出1颗） | 普通白色 |
| 粉色扇贝 | 20% | 1-2 颗 | 粉色带纹路 |
| 金色扇贝 | 8% | 2-5 颗 | 金色闪光 |
| 紫色海螺 | 2% | 5-10 颗 | 紫色稀有光效 |

### 升级与地块解锁

| 玩家等级 | 需要经验（累积收获次数） | 解锁地块 |
|---|---|---|
| Lv1 | — | 2 菜地 + 1 花圃 + 1 果树坑 |
| Lv5 | 50 次 | +1 菜地 |
| Lv10 | 150 次 | +1 花圃 |
| Lv15 | 300 次 | +1 菜地 + 1 果树坑 |
| Lv20 | 500 次 | +1 菜地 + 1 花圃 |

（经验值 = 累积收获次数，不是独立经验条）

### 作物升级与被摘次数

| 作物等级 | 升级条件（连续种植同种次数） | 可被摘取人数 |
|---|---|---|
| Lv1 | — | 1 人 |
| Lv2 | 连续种植 5 次 | 1 人 |
| Lv3 | 连续种植 15 次 | 2 人 |
| Lv4 | 连续种植 30 次 | 2 人 |
| Lv5 | 连续种植 50 次 | 3 人 |

被摘一次 → 种植者获得 10 友币（与被摘作物等级无关）
### 海上心情匹配算法（MVP）
1. 获取用户当前日记的心情标签
2. 在活跃的心情池（海玻璃 + 螃蟹 + 信使花）中筛选相同心情标签的内容，按创建时间倒序 → 取前 50 个候选
3. 如果心情匹配不足 10 个，降级为话题标签匹配
4. 如果仍不足，随机补足
5. 从候选池中随机选取一个返回
6. 用户在同一天内不会收到同一个人的多份心情
7. 信使花在匹配池中有 1.5x 加权（珍珠媒介优先被发现）
8. 螃蟹每日随机出没 1-2 次，每次携带 1 个附近岛民的心情，与匹配系统独立

---

## Testing Decisions

### 测试接缝

最高测试接缝是 **API 网关层**（`/api/*`），所有测试从 API 契约层面切入，不依赖 UI。

### 每个模块的测试策略

| 模块 | 测试重点 |
|---|---|
| `diary` | CRUD 正确性、心情标签校验、编辑后分享的内容隔离 |
| `seaglass` | 每日捡拾上限、书写→放入大海→24h 过期、送达 1 人 |
| `crab` | 随机出没逻辑、送心情→接受/拒绝、带走心情（每日 1-2 次上限）、24h 过期、送达 1 人 |
| `messenger-flower` | 种子来源验证（需用元宝购买）、种植→开花状态机、花色匹配心情、48h 过期、送达 3 人 |
| `drift` | 三载体匹配算法（心情匹配→话题降级→随机）、内容池加权（信使花 1.5x）、匿名/实名切换、同步广场 |
| `chat` | 匿名对话线程隔离、3 轮计数器、好友申请弹窗触发条件 |
| `square` | 发布/评论/拉黑 CRUD、匿名/实名切换 |
| `shore` | 每日心情刷新+贝壳刷新+海玻璃刷新+螃蟹出没（跨天重置）、贝壳品级概率分布验证 |
| `forest` | 寻宝每日限制、小野猪指引→资源实际刷新验证、松子掉落 |
| `garden` | 种植→生长→收获完整生命周期、稀有触发概率、友币记录、船收菜定时任务、信使花种植 |
| `pet` | 三选一不可更改、出走/回归状态机、礼物生成 |
| `economy` | 四币种账户隔离、交易流水完整性、珍珠兑换汇率 1:10 正确性、船收菜批量结算 |
| `shop` | 服装商店购买、珍珠兑换商店货架/兑换 |
| `friend` | 三条交友路径的申请/同意流转、拉黑后互动阻断 |

### 好的测试标准

- 测试外部行为（API 输入→输出），不测试内部实现
- 每个 API 端点至少覆盖：正常流程 + 边界条件 + 错误状态
- 定时任务（船收菜、心情过期、每日刷新）用可控时钟 mock
- 概率型逻辑（贝壳品级、稀有触发）用固定种子验证分布落在预期范围内

---

## Out of Scope (MVP)

- 真实支付/充值（接口预留，不做集成）
- 小游戏（后续版本）
- 植物/宠物 3D 建模和复杂动画（MVP 用 2D 插画 + 微动效）
- 语音/图片日记（MVP 纯文字）
- 种植的合种合作模式
- 季节系统、天气系统
- 推送通知（MVP 依赖小程序模板消息）
- 骑猪的功能性加成（MVP 纯视觉）
- 数据分析和推荐算法优化（MVP 用规则匹配）

---

## Further Notes

### 视觉风格参考

- 主场景：旅行青蛙式的精美插画海岛
- 配色：暖沙色 + 海蓝 + 森林绿 + 奶油白
- 动效：海浪轻拍、树叶摇动、宠物闲逛（轻微 GIF/帧动画）
- 字体：圆体/手写体，强化治愈感

### 情绪设计原则

整座岛围绕"你今天的心情"运转。所有子系统的文案和视觉反馈应根据用户当日心情做微调：

- 😊 开心 → 岛上阳光明媚，动物对话活泼
- 😢 难过 → 岛上下起小雨（仅在视觉层），小野猪先讲笑话再给指引，宠物靠近你的频率增加
- 😡 生气 → 树林里多刷一个蘑菇，给你"发泄散步"的出口
- 😰 焦虑 → 小鸟带来更多社交消息提醒你"不孤单"

（MVP 阶段仅做文案差异化，不做概率倾斜）

### 日常节拍设计

```
07:00  船来收菜（每日经济结算）+ 贝壳/海玻璃/螃蟹刷新
全天   种植/收获/寻宝/森林动物互动（自由活动）
全天   写日记 → 选择载体：🦀螃蟹 / 🪨海玻璃 / 🌸信使花（社交核心）
全天   沙滩捡拾/打捞心情 → 回复 → 逛广场（社交互动）
次日   岸边每日心情刷新 + 寻宝次数重置 + 螃蟹出没次数重置
```

### 冷启动建议

- 初期种子用户 100-500 人，通过微信群/朋友圈邀请
- **MVP 元宝赠送**：新注册种子用户赠送 60 元宝（价值 6 元），可买 20 颗信使花种子，确保能完整体验三载体系统
- 前两周所有人的海玻璃/螃蟹/信使花在匹配池中加权出现，确保每个用户都能发现内容
- 世界广场初期由运营/种子用户填充内容，避免"空广场"尴尬
