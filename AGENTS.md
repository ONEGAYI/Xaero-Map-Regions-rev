# Xaero Map Regions Rev 项目规则

## 项目目标

本项目主力维护线面向 Minecraft 1.21.1 NeoForge，实现一个与 Xaero Map Regions 功能相近的区域标注模组。Forge 1.20.1 历史维护线保留在 `forge/1.20.1` 分支；主线后续功能与修复优先面向 NeoForge 1.21.1。

## 技术约定

- 目标版本：Minecraft 1.21.1 + NeoForge 21.1.235。
- 向上兼容：Minecraft、NeoForge 与 Xaero 版本号集中放在构建属性中；加载器与 Xaero 接入细节集中在适配层，业务逻辑不直接散落依赖版本细节。
- 版本范围解耦：`gradle.properties` 的 `neo_version` 是 ModDevGradle 构建工具链版本，`neo_version_range` 才是 `neoforge.mods.toml` 声明的运行时最低兼容下限。两者分开维护，避免把构建版本误写为运行时要求；代码实际依赖的 API 最低可兼容到 `neo_version_range` 下限（当前 `21.1.0`），下调前须确认未使用该版本之后才引入的 API。
- 主要语言：Java。
- Java 版本：Java 21。
- Xaero's World Map 与 IMBlocker 作为开发运行期客户端依赖使用，不打包进本项目产物；`runClient` 通过 Maven 仓库自动下载外部模组。
- 服务端作为区域数据权威来源，客户端只缓存和渲染同步数据；调色盘历史颜色同样由服务端存档保存并同步给玩家。
- 调色盘收藏颜色是玩家个人偏好，保存在客户端实例 `config/xaero_map_region_rev/favourite.json`，不进入服务端共享数据。
- 区域编辑权限默认限制为 OP 且处于创造模式的玩家。
- 实现功能或修复问题前优先补充契约测试，能用纯 Java 单元测试覆盖的逻辑不要依赖游戏运行环境。
- 本地烟测使用 `./gradlew clean test build`、`./gradlew runClient` 和必要时的 `./gradlew runServer`。ModDevGradle `runClient` 开发烟测通过 `syncClientRuntimeMods` 自动把 Xaero World Map 与 IMBlocker 同步到独立的 `run-client/mods`，发布 jar 不打包外部模组。`run/`、`run-client/`、`run-server/` 与 `libs/runtime/` 均不进入 Git。
- 使用 Computer Use 进入游戏后如遇到按键无反应，优先确认 `run-client/mods` 已同步 IMBlocker；异常情况下再手动切换输入法到英文。

## 常用命令

PowerShell 环境优先使用 `.\gradlew.bat`；Git Bash、Linux 或 macOS 环境可将前缀替换为 `./gradlew`。

- `.\gradlew.bat test`：运行纯 Java 契约测试。
- `.\gradlew.bat clean`：清理 `build/`、`bin/`、根目录 `logs/`、`.tmp/` 和各运行目录日志；不清理 `run*/saves`、`run*/config`、Xaero 地图缓存或 `.gradle/`。
- `.\gradlew.bat clean test build`：完整清理、测试与构建烟测。
- `.\gradlew.bat runClient`：启动 NeoForge 开发客户端；会通过 `syncClientRuntimeMods` 自动下载并加载 Xaero World Map 与 IMBlocker。
- `.\gradlew.bat runServer`：启动 NeoForge 开发服务端，用于服务端加载与数据持久化烟测。
- `.\gradlew.bat bump "-PVERSION=0.1.1"`：将 `gradle.properties` 中的 `mod_version` 设置为指定版本。
- `.\gradlew.bat bump -Ppatch` / `-Pminor` / `-Pmajor`：按语义化版本递增 `mod_version`。
- `.\gradlew.bat --refresh-dependencies test`：依赖缓存异常或远端依赖更新后刷新依赖并跑测试。
- `git diff --check`：提交前检查空白与补丁格式问题。

## 可选技术路线

- Xaero World Map 开发运行期 jar 内存在 `WorldMap.mapElementRenderHandler.add(...)`、`MapElementRenderer`、`MapElementRenderProvider`、`ElementReader` 等 public 类，可作为后续接入 Xaero 地图元素渲染系统的候选路径；具体运行期版本与 CurseForge 文件 ID 见 `gradle.properties` 的 `xaero_world_map_runtime_version` 和 `xaero_world_map_runtime_file_id`，其中 Gradle 解析依赖以 `*_runtime_file_id` 为准，`*_runtime_version` 用于人工核对。
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
├── CHANGELOG.md
│   版本变更记录与发布说明。
├── .gitignore
│   Git 忽略规则，排除构建产物、运行目录、日志与 IDE 文件。
├── README.md
│   项目介绍、安装使用、版本依赖、构建命令与当前 MVP 状态。
├── build.gradle
│   ModDevGradle 构建脚本、NeoForge 运行配置、客户端运行期模组同步与测试依赖声明。
├── docs/
│   ├── review-reports/
│   │   外部/人工审查报告归档。
│   └── superpowers/
│       ├── plans/
│       │   ├── 2026-07-02-xaero-map-regions-mvp-implementation.md
│       │   │   Forge 47.3.33 区域标注 MVP 实现计划。
│       │   ├── 2026-07-03-map-labels-and-editor-implementation.md
│       │   │   地图标注、可改快捷键和地图内编辑实现计划。
│       │   └── 2026-07-04-neoforge-1211-port-implementation.md
│       │       NeoForge 1.21.1 主线迁移实施计划。
│       └── specs/
│           ├── 2026-07-02-xaero-map-regions-mvp-design.md
│           │   Forge 1.20.1 版 Xaero 区域标注 MVP 设计文档。
│           ├── 2026-07-03-map-labels-and-editor-design.md
│           │   地图标注、可改快捷键和地图内编辑设计文档。
│           └── 2026-07-04-neoforge-1211-port-design.md
│               NeoForge 1.21.1 主线迁移设计文档。
├── gradle/
│   Gradle Wrapper 运行文件。
├── gradle.properties
│   模组版本、NeoForge/Minecraft/Xaero 版本与发布包名属性。
├── gradlew
│   POSIX 环境 Gradle Wrapper 启动脚本。
├── gradlew.bat
│   Windows 环境 Gradle Wrapper 启动脚本。
├── settings.gradle
│   Gradle 项目名称配置。
├── src/main/java/com/suian/xaeroregionsrev/
│   ├── client/
│   │   客户端区域/颜色历史缓存、clientbound payload 桥接、玩家收藏色配置、客户端本地配置（自动校准开关持久化）、可改快捷键、分页区域管理器、调色盘、地图内编辑状态、编辑提交等待与服务端回执处理、凹多边形填充与 Xaero World Map 覆盖层交互适配。
│   ├── command/
│   │   `/region` 服务端命令入口与权限校验；`client/command` 子包提供自动校准开关等客户端命令。
│   ├── data/
│   │   区域与共享颜色历史 SavedData 持久化。
│   ├── network/
│   │   NeoForge payload 注册与分发、区域同步网络包、地图内编辑 C2S 请求与 S2C 结果回执处理、刷新冷却与广播入口。
│   ├── platform/
│   │   Minecraft 权限适配。
│   ├── region/
│   │   区域、点位、颜色、几何、请求限制/验证与 NBT 编解码模型。
│   └── service/
│       区域读写服务层与跨维度快照聚合。
├── src/main/resources/
│   ├── META-INF/neoforge.mods.toml
│   │   NeoForge 模组元数据与依赖声明。
│   ├── assets/xaeroregionsrev/
│   │   中英文客户端文本资源与地图编辑器 UI 贴图资源。
│   └── pack.mcmeta
│       资源包描述与 pack format 配置。
└── src/test/java/com/suian/xaeroregionsrev/
    纯 Java 契约测试，覆盖区域模型、编解码、网络包、Xaero 屏幕识别与地图编辑状态机。
```
