# Xaero Map Regions Rev 项目规则

## 项目目标

本项目是一个与 Xaero Map Regions 功能相近的区域标注模组，采用多子项目结构同时维护两条加载器线：

- `common/`：纯 Java 公共模块，承载平台无关的区域模型、网络数据、NBT 编解码和服务/客户端抽象接口。
- `neoforge/mc-1.21.1/`：Minecraft 1.21.1 + NeoForge 主力维护线。
- `forge/mc-1.20.1/`：Minecraft 1.20.1 + Forge 历史维护线。

主线后续功能与修复优先面向 NeoForge 1.21.1；Forge 1.20.1 作为历史维护线保留在同仓库的独立子项目中。

## 技术约定

- 多子项目结构：根 `build.gradle` 不 apply 任何 MC 插件，只注册跨平台聚合任务（`buildAll`、`bump`）；各加载器子项目在各自的 `build.gradle` 中 apply 对应 MC 插件。
- 公共模块：`common/` 是纯 Java 17 的 `java-library` 项目，不依赖任何 Minecraft API；它定义平台无关的区域模型、网络数据类和一组抽象接口，并通过 `project(':common')` 被 `neoforge:mc-1.21.1` 与 `forge:mc-1.20.1` 作为 `implementation` 依赖引入。
- MC API 隔离：平台子项目通过 sourceSet 把 common 的 main 源码纳入 mod classpath，并为以下抽象接口提供各自的适配实现：
  - `PacketBuffer`（网络缓冲区，对应 NeoForge/Forge 的 `FriendlyByteBuf`）。
  - `NbtCompound` / `NbtList` / `NbtFactory`（NBT 标签抽象，对应 `CompoundTag` / `ListTag`）。
  - `ServerContext` / `RegionStore`（服务端上下文与区域持久化存储，对应 `MinecraftServer`/`ServerLevel` 与 `RegionSavedData`）。
  - `EditResultHandler`（编辑结果回调，对应客户端屏幕转发）。
- 主要语言：Java。`common` 与 `forge:mc-1.20.1` 使用 Java 17，`neoforge:mc-1.21.1` 使用 Java 21。
- 版本范围解耦：各平台的 `gradle.properties` 中，`neo_version` / `forge_version` 是构建工具链版本，`neo_version_range` / `forge_version_range` 才是 mods.toml 声明的运行时最低兼容下限。两者分开维护，避免把构建版本误写为运行时要求；下调运行时下限前须确认未使用该版本之后才引入的 API。
- 向上兼容：Minecraft、加载器与 Xaero 版本号集中放在各平台子项目的 `gradle.properties` 中；加载器与 Xaero 接入细节集中在适配层，业务逻辑不直接散落依赖版本细节。
- Xaero's World Map 与 IMBlocker 作为开发运行期客户端依赖使用，不打包进本项目产物；`runClient` 通过 Maven 仓库自动下载外部模组。
- 服务端作为区域数据权威来源，客户端只缓存和渲染同步数据；调色盘历史颜色同样由服务端存档保存并同步给玩家。
- 调色盘收藏颜色是玩家个人偏好，保存在客户端实例 `config/xaero_map_region_rev/favourite.json`，不进入服务端共享数据。
- 区域编辑权限默认限制为 OP 且处于创造模式的玩家。
- 实现功能或修复问题前优先补充契约测试，能用纯 Java 单元测试覆盖的逻辑放在 `common` 模块，不依赖游戏运行环境。
- 本地烟测使用 `./gradlew :common:test :neoforge:mc-1.21.1:test :forge:mc-1.20.1:test`、`./gradlew buildAll` 和必要时的各平台 `runClient` / `runServer`。各平台 `runClient` 通过 `syncClientRuntimeMods` 自动把 Xaero World Map 与 IMBlocker 同步到独立的 `run-client/mods`，发布 jar 不打包外部模组。各平台的 `run*/` 目录均不进入 Git（见 `.gitignore`）。
- 使用 Computer Use 进入游戏后如遇到按键无反应，优先确认对应平台的 `run-client/mods` 已同步 IMBlocker；异常情况下再手动切换输入法到英文。

## 常用命令

PowerShell 环境优先使用 `.\gradlew.bat`；Git Bash、Linux 或 macOS 环境可将前缀替换为 `./gradlew`。以下示例统一使用 `./gradlew`。命令需要从仓库根目录执行，Gradle 会按 `settings.gradle` 的 include 配置路由到对应子项目。

- `./gradlew :common:test`：运行公共模块纯 Java 契约测试。
- `./gradlew :neoforge:mc-1.21.1:test`：运行 NeoForge 子项目测试。
- `./gradlew :neoforge:mc-1.21.1:runClient`：启动 NeoForge 开发客户端；会通过 `syncClientRuntimeMods` 自动下载并加载 Xaero World Map 与 IMBlocker。
- `./gradlew :forge:mc-1.20.1:test`：运行 Forge 子项目测试。
- `./gradlew :forge:mc-1.20.1:runClient`：启动 Forge 开发客户端。
- `./gradlew buildAll`：构建所有平台子项目（`neoforge:mc-1.21.1` + `forge:mc-1.20.1`）。
- `./gradlew bump -Ppatch` / `-Pminor` / `-Pmajor`：按语义化版本递增根 `gradle.properties` 的 `mod_version`。
- `./gradlew bump "-PVERSION=0.1.1"`：将 `mod_version` 设置为指定版本。
- `./gradlew --refresh-dependencies :neoforge:mc-1.21.1:test`：依赖缓存异常或远端依赖更新后刷新依赖并跑测试。
- `git diff --check`：提交前检查空白与补丁格式问题。

## 可选技术路线

- Xaero World Map 开发运行期 jar 内存在 `WorldMap.mapElementRenderHandler.add(...)`、`MapElementRenderer`、`MapElementRenderProvider`、`ElementReader` 等 public 类，可作为后续接入 Xaero 地图元素渲染系统的候选路径；具体运行期版本与 CurseForge 文件 ID 见各平台子项目 `gradle.properties` 的 `xaero_world_map_runtime_version` 和 `xaero_world_map_runtime_file_id`，其中 Gradle 解析依赖以 `*_runtime_file_id` 为准，`*_runtime_version` 用于人工核对。
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
│   Git 忽略规则，排除构建产物、各平台运行目录、日志与 IDE 文件。
├── README.md
│   项目介绍、安装使用、版本依赖、构建命令与当前 MVP 状态。
├── build.gradle
│   根构建脚本，不 apply 任何 MC 插件；注册跨平台聚合任务 buildAll 与版本号 bump 任务。
├── settings.gradle
│   Gradle 多项目配置：include common、neoforge:mc-1.21.1、forge:mc-1.20.1。
├── gradle.properties
│   模组元数据（mod_id、mod_name、mod_version 等）与 common 依赖版本（joml）。
├── gradle/
│   ├── wrapper/
│   │   Gradle Wrapper 运行文件。
│   ├── region-platform.gradle
│   │   平台子项目共享逻辑：jar manifest 模板。
│   └── region-repositories.gradle
│       所有子项目共享的仓库声明（mavenCentral、Modrinth、CurseMaven）。
├── gradlew / gradlew.bat
│   Gradle Wrapper 启动脚本（POSIX / Windows）。
├── common/
│   纯 Java 17 公共模块（java-library），承载平台无关的业务逻辑与抽象接口。
│   ├── build.gradle
│   │   java-library 插件、JOML/slf4j 依赖与 JUnit 5 测试配置。
│   └── src/
│       ├── main/java/com/suian/xaeroregionsrev/
│       │   ├── client/
│       │   │   客户端区域/颜色历史缓存、编辑结果回调接口（EditResultHandler）。
│       │   │   ├── editor/ 地图编辑器模型（调色盘、编辑会话、区域选择、区域管理列表）。
│       │   │   └── xaero/ 凹多边形三角化与渲染样式模型。
│       │   ├── network/
│       │   │   ├── buffer/ PacketBuffer 网络缓冲区抽象接口。
│       │   │   └── data/ 平台无关的网络数据类（同步、创建/更新/删除请求、编辑结果、刷新）。
│       │   ├── region/
│       │   │   区域、点位、颜色、几何、请求验证与共享抽象；nbt/ 子包为 NBT 接口（NbtCompound/NbtList/NbtFactory）与 RegionNbtCodec。
│       │   └── service/
│       │       RegionService 业务服务层与 ServerContext / RegionStore 抽象接口。
│       ├── main/resources/assets/xaeroregionsrev/
│       │   中英文 lang 文本与地图编辑器 UI 贴图资源。
│       └── test/java/com/suian/xaeroregionsrev/
│           纯 Java 契约测试，覆盖区域模型、编解码、调色盘、编辑状态机与多边形几何。
├── neoforge/
│   └── mc-1.21.1/
│       Minecraft 1.21.1 + NeoForge 21.1.235 主力维护线，Java 21。
│       ├── build.gradle
│       │   ModDevGradle 构建、run 配置（client/server/data）、客户端运行期模组同步与 processResources 占位符替换。
│       ├── gradle.properties
│       │   NeoForge/Minecraft/Xaero 版本与运行时依赖下限。
│       └── src/
│           ├── main/java/com/suian/xaeroregionsrev/
│           │   ├── client/ 客户端本地配置、payload 桥接、快捷键、Xaero 覆盖层与编辑器屏幕。
│           │   ├── command/ `/region` 服务端命令与权限校验。
│           │   ├── data/ RegionSavedData 持久化。
│           │   ├── network/ payload 注册分发、编辑请求处理与各 payload 包类。
│           │   ├── platform/ NeoForge ServerContext、权限适配、RegionStore 实现。
│           │   └── region/nbt/ CompoundTag/ListTag NBT 抽象适配实现。
│           ├── main/resources/
│           │   ├── META-INF/neoforge.mods.toml NeoForge 模组元数据与依赖声明。
│           │   └── pack.mcmeta 资源包描述与 pack format。
│           └── test/java/com/suian/xaeroregionsrev/
│               NeoForge 子项目测试（payload 编解码、SavedData、屏幕识别、输入路由等）。
├── forge/
│   └── mc-1.20.1/
│       Minecraft 1.20.1 + Forge 47.3.33 历史维护线，Java 17。
│       ├── build.gradle
│       │   ForgeGradle 构建、run 配置、reobfJar 与客户端运行期模组依赖。
│       ├── gradle.properties
│       │   Forge/Minecraft/Xaero 版本与运行时依赖下限。
│       └── src/
│           ├── main/java/com/suian/xaeroregionsrev/
│           │   client/ command/ data/ network/ platform/ region/nbt/
│           │   Forge 平台实现，结构与 NeoForge 子项目对应。
│           ├── main/resources/
│           │   ├── META-INF/mods.toml Forge 模组元数据与依赖声明。
│           │   └── pack.mcmeta 资源包描述与 pack format。
│           └── test/java/com/suian/xaeroregionsrev/
│               Forge 子项目测试。
└── docs/
    ├── review-reports/
    │   外部/人工审查报告归档。
    └── superpowers/
        ├── plans/ 各阶段实施计划（MVP、标注编辑器、NeoForge 迁移、多子项目重构）。
        └── specs/ 各阶段设计文档。
```

注：平台子项目通过 sourceSet 把 `common` 的 main 源码纳入 mod classpath，并以 `implementation project(':common')` 提供编译依赖；各 `run*/`、`build/`、`.gradle/` 等目录不进入 Git。
