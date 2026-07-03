# Xaero Map Regions Rev 项目规则

## 项目目标

本项目面向 Minecraft 1.20.1 Forge，从零实现一个与 Xaero Map Regions 功能相近的区域标注模组。目标是先完成稳定 MVP，再逐步补齐图形化编辑、自定义图标、已探索区域裁剪、进入区域标题等高级能力。

## 技术约定

- 目标版本：Minecraft 1.20.1 + Forge 47.3.33。
- 向上兼容：Forge 版本号集中放在构建属性中；Forge 与 Xaero 接入细节集中在适配层，业务逻辑不直接散落依赖版本细节。
- 主要语言：Java。
- Xaero's World Map 与 IMBlocker 作为开发运行期客户端依赖使用，不打包进本项目产物；`runClient` 通过 Maven 仓库自动下载这些外部模组。
- 服务端作为区域数据权威来源，客户端只缓存和渲染同步数据。
- 区域编辑权限默认限制为 OP 且处于创造模式的玩家。
- 实现功能或修复问题前优先补充契约测试，能用纯 Java 单元测试覆盖的逻辑不要依赖游戏运行环境。
- 本地烟测使用 `./gradlew clean test build` 和 `./gradlew runClient`。ForgeGradle `runClient` 开发烟测通过 `clientRuntimeMods` 配置自动加载 Xaero World Map 与 IMBlocker，并启用 mixin refmap 重映射，避免将发布版 raw jar 直接放入 `run/mods/`。`run/` 与 `libs/runtime/` 均不进入 Git。
- 使用 Computer Use 进入游戏后如遇到按键无反应，优先手动切换输入法到英文；IMBlocker 是首选输入法冲突解决方案，非输入框场景应由 IMBlocker 忽略中文输入法。

## 常用命令

PowerShell 环境优先使用 `.\gradlew.bat`；Git Bash、Linux 或 macOS 环境可将前缀替换为 `./gradlew`。

- `.\gradlew.bat test`：运行纯 Java 契约测试。
- `.\gradlew.bat clean test build`：完整清理、测试与构建烟测。
- `.\gradlew.bat runClient`：启动 Forge 开发客户端；会通过 `clientRuntimeMods` 自动下载并加载 Xaero World Map 与 IMBlocker。
- `.\gradlew.bat --refresh-dependencies test`：依赖缓存异常或远端依赖更新后刷新依赖并跑测试。
- `git diff --check`：提交前检查空白与补丁格式问题。

## 可选技术路线

- Xaero World Map 1.20.1/1.41.2 jar 内存在 `WorldMap.mapElementRenderHandler.add(...)`、`MapElementRenderer`、`MapElementRenderProvider`、`ElementReader` 等 public 类，可作为后续接入 Xaero 地图元素渲染系统的候选路径。
- 该路径目前仅作为待进一步评估和实测的方向：它更适合标签、图标、点状元素，半透明多边形填充仍可能需要独立 overlay 绘制。
- 暂不改动当前实现；继续保持独立 overlay + `MapProjectionAdapter` 集中适配 Xaero 视口状态。

## 文档与提交

- 设计、计划、CHANGELOG、提交信息和 PR 描述默认使用中文。
- 通用项目规则维护在本文件。
- `CLAUDE.md` 只保留 Claude 专属补充规则，并通过 `@AGENTS.md` 引入本文件。
- 修改文件树或模块职责后，同步维护下方文件树说明。

## 文件树

```text
.
├── AGENTS.md
│   项目通用规则、技术约定与文件树说明。
├── CLAUDE.md
│   Claude 专属入口文件，通过 @AGENTS.md 引入通用规则。
├── README.md
│   项目介绍、安装使用、版本依赖、构建命令与当前 MVP 状态。
├── build.gradle
│   ForgeGradle 构建脚本、运行配置、客户端运行期模组依赖与测试依赖声明。
├── docs/
│   └── superpowers/
│       ├── plans/
│       │   ├── 2026-07-02-xaero-map-regions-mvp-implementation.md
│       │   │   Forge 47.3.33 区域标注 MVP 实现计划。
│       │   └── 2026-07-03-map-labels-and-editor-implementation.md
│       │       地图标注、可改快捷键和地图内编辑实现计划。
│       └── specs/
│           ├── 2026-07-02-xaero-map-regions-mvp-design.md
│           │   Forge 1.20.1 版 Xaero 区域标注 MVP 设计文档。
│           └── 2026-07-03-map-labels-and-editor-design.md
│               地图标注、可改快捷键和地图内编辑设计文档。
├── src/main/java/com/suian/xaeroregionsrev/
│   ├── client/
│   │   客户端区域缓存、可改快捷键、地图内编辑状态、凹多边形填充与 Xaero World Map 覆盖层交互适配。
│   ├── command/
│   │   `/region` 服务端命令入口与权限校验。
│   ├── data/
│   │   区域 SavedData 持久化。
│   ├── network/
│   │   区域同步网络包、地图内编辑 C2S 请求处理、刷新冷却与广播入口。
│   ├── platform/
│   │   Forge 权限适配。
│   ├── region/
│   │   区域、点位、颜色、几何、请求限制/验证与 NBT 编解码模型。
│   └── service/
│       区域读写服务层与跨维度快照聚合。
├── src/main/resources/
│   Forge 模组元数据、资源包描述与中英文客户端文本资源。
└── src/test/java/com/suian/xaeroregionsrev/
    纯 Java 契约测试，覆盖区域模型、编解码、网络包、Xaero 屏幕识别与地图编辑状态机。
```
