# 果香小岛 - 客户端 (FruitIslandClient)

Cocos Creator 3.8 微信小游戏客户端

## 技术栈

- Cocos Creator 3.8
- TypeScript
- 微信小游戏

## 目录结构

```
assets/
├── scenes/          # 场景文件
│   ├── Launch.scene # 启动/登录场景
│   └── Main.scene   # 主游戏场景
├── scripts/
│   ├── game/
│   │   └── GameManager.ts    # 唯一游戏入口
│   ├── map/                  # 地图、网格、建筑和碰撞
│   ├── camera/               # 玩家跟随和拖动视野
│   ├── network/
│   │   ├── HttpClient.ts     # 统一 HTTP 请求
│   │   └── Api.ts            # 接口地址集中管理
│   ├── player/
│   │   └── PlayerController.ts # 玩家点击移动控制
│   ├── ui/
│   │   └── UIManager.ts      # HUD
│   └── types/
│       └── index.ts          # 类型定义
├── prefabs/         # 预制体
├── textures/        # 贴图资源
├── ui/              # UI 资源
└── resources/       # 动态加载资源
```

## 游戏流程

```
启动 GameManager
    ↓
检查本地 Token
    ↓
┌── 有效？──→ 请求 /game/init → 进入 MainScene
│
└── 无效？──→ 微信登录 → 获取 code
                  ↓
              POST /auth/wechat/login
                  ↓
              保存 Token
                  ↓
              GET /game/init
                  ↓
              进入 MainScene
```

## 场景结构

### Launch.scene
```
Canvas
├── LoginButton
├── StatusLabel
└── LoadingIndicator
```

### Main.scene
```
Main
├── Camera
├── Map
│   └── (岛屿地图)
├── Player
│   └── (岛主角色，挂载 PlayerController)
└── UI
    ├── HUD（金币、钻石、等级）
    └── BottomBar（菜单按钮）
```

## 开发阶段

### Demo2.4 - 当前阶段
- [x] 项目结构搭建
- [x] GameManager 单例管理器
- [x] HttpClient 网络模块
- [x] Api 接口定义
- [x] PlayerController 点击移动
- [x] 48×48 地图配置
- [x] 摄像机跟随与拖动代码
- [x] 建筑碰撞
- [ ] 在 Cocos Creator 中创建并保存 Main.scene
- [ ] 土地交互接入 Cocos 客户端

### 后续开发
- [ ] 农场种植系统
- [ ] 背包系统
- [ ] 建筑系统
- [ ] 顾客订单
- [ ] Spine 角色动画

## 对接后端

本地开发地址统一维护在 `assets/configs/GameConfig.ts`：

```typescript
AUTH_SERVER_URL = 'http://localhost:8081'
GAME_SERVER_URL = 'http://localhost:8082'
```

## Cocos Creator 首次运行

1. 使用 Cocos Creator 3.8 打开 `FruitIslandClient`。
2. 新建并保存 `assets/scenes/Main.scene`。
3. 场景中创建 `Canvas`，在 Canvas 或其子节点挂载
   `assets/scripts/game/GameManager.ts`。
4. 点击预览。其他世界节点会由 GameManager 自动创建。

## 微信小游戏配置

在 Cocos Creator 构建发布时选择 **微信小游戏** 平台。
