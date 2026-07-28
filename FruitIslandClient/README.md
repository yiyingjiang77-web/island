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
│   ├── core/
│   │   └── GameManager.ts    # 游戏入口管理器
│   ├── network/
│   │   ├── HttpClient.ts     # 统一 HTTP 请求
│   │   └── Api.ts            # 接口地址集中管理
│   ├── player/
│   │   └── PlayerController.ts # 玩家点击移动控制
│   ├── ui/
│   │   └── LoginUI.ts        # 登录页面
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

### V1.0 - 已完成
- [x] 项目结构搭建
- [x] GameManager 单例管理器
- [x] HttpClient 网络模块
- [x] Api 接口定义
- [x] LoginUI 登录页面
- [x] PlayerController 点击移动

### V1.1 - 待开发
- [ ] 农场种植系统
- [ ] 背包系统
- [ ] 建筑系统
- [ ] 顾客订单
- [ ] Spine 角色动画

## 对接后端

本地开发：修改 `HttpClient.ts` 中的 `baseUrl`

```typescript
private baseUrl: string = 'http://localhost:8080';
```

## 微信小游戏配置

在 Cocos Creator 构建发布时选择 **微信小游戏** 平台。
