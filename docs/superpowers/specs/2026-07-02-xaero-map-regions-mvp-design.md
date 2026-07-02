# Xaero Map Regions Rev MVP 设计

## 背景

目标项目 Xaero Map Regions 在 Modrinth 和 CurseForge 上发布为 MIT License，但公开项目元数据没有源码地址。检索结果显示 Modrinth API 的 `source_url`、`issues_url`、`wiki_url` 均为空，GitHub 仓库与代码搜索也没有找到同名公开源码。因此本项目按干净实现处理，不复制或反编译目标 jar。

本设计面向 Minecraft 1.20.1 + Forge 47.3.33，实现一个与原模组核心体验相近的 MVP：服务端保存区域数据、同步给客户端，并在 Xaero's World Map 界面绘制区域边界和半透明填充。实现时需要把 Forge 版本号集中在构建属性中，并把 Forge/Xaero 接入代码隔离到适配层，便于同一 Minecraft 版本内向上兼容。

## 范围

### MVP 包含

- Forge 1.20.1-47.3.33 Java 模组脚手架。
- 区域数据模型：区域 id、名称、维度、点列表、ARGB 颜色、分类、图标名、创建时间和更新时间。
- 服务端 `SavedData` 持久化区域。
- 服务端权限检查：只有 OP 且创造模式玩家可以创建、编辑、删除区域或上传图标。
- 网络同步：玩家登录和区域变更时同步区域快照到客户端。
- 客户端区域缓存。
- Xaero World Map 屏幕上的多边形边界与填充渲染。
- 基础命令：
  - `/region hide <player> <region>`
  - `/region visible <player> <region>`
  - `/region createpoly <name> <argb> <points>`：MVP 调试/管理命令，用于在图形化绘制流程完成前创建多边形区域。
  - `/region createpoint <player> <mode> <iconname> <label> <x> <y> <z>`：作为目标功能兼容项，首轮可先实现服务端数据与同步，不要求完整地图图标渲染。
  - `/region delpoint <player> <x> <y> <z>`：同上，首轮以数据闭环为主。
- 单元测试覆盖序列化、权限判断、点在多边形内判断、区域隐藏规则等纯逻辑。

### MVP 暂不包含

- 完整图形化区域管理器。
- 点标记在 Xaero 地图上的完整图标渲染。
- 自定义 PNG 图标上传和分发。
- 严格按 Xaero 已探索地图数据裁剪区域显示。
- 新揭示区域淡入动画。
- Traveler's Titles 反射集成。
- 服务端配置界面和多语言文本完整打磨。

这些能力作为后续阶段实现，避免一开始被 Xaero 私有 API 和屏幕坐标转换拖住。

## 推荐方案

采用 Forge 1.20.1-47.3.33 + Java，弱耦合接入 Xaero's World Map。

项目不将 Xaero's World Map 或 XaeroLib 打包进 jar，只在开发环境和运行环境声明外部依赖。服务端数据、权限、同步协议和核心几何逻辑由本项目独立维护。客户端渲染层尽量通过 Forge 客户端事件、Mixin 或反射接入 Xaero 地图屏幕，接入点集中封装，减少未来 Xaero 版本变化时的修改范围。

相比 Kotlin 复刻或先做独立地图 UI，此方案更适合 Forge 1.20.1 生态，也能尽早验证“在 Xaero 地图上看见区域”的核心体验。

## 架构

### 服务端

服务端是权威数据源。区域数据保存在 `SavedData` 中，按世界存储，并通过一个服务类提供查询、创建、更新、删除、隐藏和显示操作。所有写操作先经过权限判断，再修改数据并广播同步包。

核心模块：

- `RegionStore`：封装 `SavedData` 读写。
- `RegionService`：提供业务操作，负责权限、校验和同步触发。
- `RegionCommands`：注册 `/region` 命令。
- `RegionNetwork`：注册 SimpleChannel 和消息类型。

### 客户端

客户端只保存服务端同步来的只读快照。渲染时读取当前维度和玩家隐藏规则，筛选出应该显示的区域，再将区域点转换到 Xaero 地图屏幕坐标。

核心模块：

- `ClientRegionCache`：保存区域、隐藏状态和点标记快照。
- `XaeroMapOverlayRenderer`：检测 Xaero World Map 屏幕并绘制区域。
- `MapProjectionAdapter`：集中处理世界坐标到地图屏幕坐标的转换。
- `RegionTitleOverlay`：后续阶段用于进入区域标题，MVP 可先保留接口或不实现。

### 共享逻辑

共享逻辑不依赖 Minecraft 客户端类，尽量可单元测试。

- `Region`、`RegionPoint`、`RegionId`、`PointMarker` 等数据类。
- `RegionCodec`：NBT 和网络编码。
- `PolygonMath`：点在多边形内、包围盒、简单校验。
- `PermissionRules`：权限判定的纯逻辑入口，Minecraft 对象适配在服务端层完成。

## 数据流

1. 服务端加载世界时初始化 `RegionStore`。
2. 玩家登录后，服务端向该玩家发送区域快照。
3. OP 创造玩家通过命令或后续 GUI 发起修改。
4. 服务端校验权限和数据合法性。
5. 修改写入 `SavedData` 并标记 dirty。
6. 服务端向所有相关客户端广播增量或全量同步。
7. 客户端更新 `ClientRegionCache`。
8. 客户端打开 Xaero World Map 时，渲染器读取缓存并绘制多边形。

## 错误处理

- 无权限写操作返回命令错误，不修改数据。
- 非法区域点数、非法颜色、未知维度、重复名称等输入在服务端拒绝。
- 网络包解析失败时丢弃该包并记录日志，避免客户端崩溃。
- Xaero World Map 未安装或接入点变化时，模组应尽量降级：服务端功能和命令仍可用，客户端仅不显示地图 overlay。
- 渲染转换失败时跳过当前帧，不影响游戏继续运行。

## 测试策略

- 单元测试优先覆盖不依赖 Minecraft 的共享逻辑：
  - 多边形点内外判断。
  - 区域数据序列化和反序列化。
  - 隐藏区域规则。
  - 权限策略的边界条件。
- Forge 开发环境烟测：
  - `runClient` 能启动。
  - `runServer` 能启动。
  - 客户端和服务端连接后能收到区域同步。
  - 打开 Xaero 地图后不会崩溃。
- 后续接入图形化编辑前，先为区域编辑流程补充命令级测试或手动烟测脚本说明。

## 后续阶段

1. 图形化绘制流程：在 Xaero 地图中按键进入绘制状态，鼠标点击添加多边形点。
2. 区域管理界面：列表、重命名、改色、分类、删除。
3. 自定义图标：服务端保存图标元数据并同步给客户端。
4. 已探索区域裁剪：研究 Xaero World Map 数据结构，只在玩家探索过的位置显示区域。
5. 进入区域标题：先做内部 overlay，再通过反射兼容 Traveler's Titles。
6. 新揭示区域淡入动画：基于客户端缓存记录首次可见时间。

## 验收标准

- 仓库能构建 Forge 1.20.1 模组 jar。
- 服务端能持久保存至少一个多边形区域。
- 客户端加入世界后能收到区域数据。
- Xaero World Map 打开时能显示服务端同步的区域边界和半透明填充。
- 非 OP 或非创造玩家无法创建、编辑、删除区域。
- 核心纯逻辑测试通过。
