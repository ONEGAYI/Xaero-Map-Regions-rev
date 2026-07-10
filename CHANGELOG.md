# Changelog

所有重要变更都会记录在此文件中。

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 的结构，并使用语义化版本。

## [1.0.0] - 2026-07-10

这是项目的第一个大版本。核心变更是将双分支（master NeoForge 1.21.1 + forge/1.20.1）合并为单分支 Gradle 多子项目结构，同时统一了两条加载器线的功能。NeoForge 1.21.1 与 Forge 1.20.1 现在在同一仓库中并存，共享平台无关的业务逻辑。

### 新功能

- Forge 1.20.1 端移植自动校准开关功能：默认关闭，与 NeoForge 端行为对齐；支持 `/region autoCalibrate true|false` 游戏内热切换，配置持久化到 `config/xaero_map_region_rev/client.json`。
- Forge 1.20.1 端补齐 NBT 字段长度校验和颜色历史上限保护（`ColorPaletteLimits.MAX_COLORS`），与 NeoForge 端安全边界对齐。

### 其他改进

- 将项目重构为 Gradle 多子项目：根 `build.gradle` 只注册跨平台聚合任务（`buildAll`、`bump`），`settings.gradle` include `common`、`neoforge:mc-1.21.1`、`forge:mc-1.20.1`。
- 新增 `common/` 纯 Java 17 公共模块（java-library），承载平台无关的区域模型、网络数据类、NBT 编解码、`RegionService` 业务服务层与一组平台抽象接口（`PacketBuffer`、`NbtCompound`/`NbtList`/`NbtFactory`、`ServerContext`/`RegionStore`、`EditResultHandler`）。
- 将原 `src/` 下不依赖 Minecraft API 的源码与纯 Java 契约测试迁入 `common/`；剩余平台相关源码迁入各加载器子项目，并为抽象接口提供 NeoForge / Forge 各自的适配实现。
- 平台子项目通过 sourceSet 把 `common` 的 main 源码纳入 mod classpath，并以 `implementation project(':common')` 提供编译依赖，MC API 边界集中隔离在适配层。
- 迁移构建系统：版本号、构建/运行配置集中到各平台子项目的 `build.gradle` 与 `gradle.properties`，模组元数据（mod_id、mod_version 等）保留在根 `gradle.properties`。
- 新增 `buildAll` 跨平台构建任务；旧单项目下的 `src/` 目录已全部迁移并删除。
- 更新 `.gitignore` 添加各平台 `run*/` 目录规则；同步更新 AGENTS.md、README.md 反映多子项目结构与命令。

## [0.1.5] - 2026-07-09

这个版本为地图自动校准机制引入可配置开关，解决区域图形在相机移动时因校准精度误差导致的轻微左右抖动。自动校准默认关闭，需要时可通过客户端命令开启。

### 新功能

- 新增地图自动校准开关，默认关闭，避免校准精度误差导致的区域图形左右抖动；可通过 `/region autoCalibrate true|false` 热切换，或 `/region autoCalibrate` 查询当前状态。配置持久化到客户端 `config/xaero_map_region_rev/client.json`，重启后保持。

## [0.1.4] - 2026-07-09

这是一个面向兼容性的修复版本。0.1.3 的 `neoforge.mods.toml` 把 NeoForge 运行时依赖下限与构建工具链版本耦合，要求 `21.1.235` 或以上，导致使用较低小版本（如 21.1.226）的整合包在 FML 阶段崩溃。本版本将下限放宽到代码实际依赖的最低 API 版本，与构建版本解耦。

### Bug 修复

- 放宽 NeoForge 运行时依赖下限：`neoforge.mods.toml` 此前把运行时下限与构建工具链版本耦合（要求 `21.1.235` 或以上），导致使用较低小版本（如 21.1.226）的整合包在 FML 阶段崩溃。改为独立的 `neo_version_range` 属性，下限放宽到 `21.1.0`（代码实际用到的 API 自该版本起稳定），与构建版本解耦。

## [0.1.3] - 2026-07-04

这是一个面向 NeoForge 1.21.1 主线的迁移与修复版本。主线现在以 NeoForge 1.21.1 作为主要维护目标，并保留 Forge 1.20.1 历史分支；同时修复了迁移后的服务端加载边界、异常存档边界和表单文字模糊问题。

### 新功能

- 支持 Minecraft 1.21.1 + NeoForge 21.1.235 主线，构建产物名包含 `neoforge-1.21.1`，便于和 Forge 1.20.1 历史维护线区分。（无 PR，直接提交 ad4e7c9）

### Bug 修复

- 修复 NeoForge dedicated server 加载风险：通过客户端入口与 S2C payload 字符串桥接，避免服务端误加载 client-only 类。（无 PR，直接提交 cb4bec0）
- 加强 SavedData / NBT 读取边界，裁剪颜色历史并拒绝超出同步上限的存档字符串，降低异常或旧存档导致同步异常的风险。（无 PR，直接提交 cb4bec0）
- 修复自定义表单、颜色选择器与区域管理器的说明文字被 1.21.1 背景模糊阶段覆盖而显示发虚的问题。（无 PR，直接提交 6266497）

### 其他改进

- 将构建系统从 ForgeGradle 迁移到 ModDevGradle，并迁移 mod 入口、事件注册、NeoForge payload 网络、客户端发送、SavedData 签名与测试 classpath。（无 PR，直接提交 ad4e7c9）
- 将区域同步与地图内编辑请求包迁移为 NeoForge payload，并新增共享 `PacketCodecs` 与 clientbound payload 分发桥接。（无 PR，直接提交 ad4e7c9、cb4bec0）
- 将 Xaero World Map 与 IMBlocker 作为开发客户端运行期依赖同步到独立 `run-client/mods`，同时让 `runServer` 使用独立 `run-server`，避免服务端端测误加载客户端模组。（无 PR，直接提交 cb4bec0、6266497）
- 补充 NeoForge 1.21.1 主线迁移设计与实施计划，明确 `master` 作为 NeoForge 主维护线、`forge/1.20.1` 作为 Forge 历史维护线。（无 PR，直接提交 d3ef3c1、15fb01f、464b8dc）
- 忽略 `.vscode` 本地编辑器配置目录，避免开发环境文件进入版本控制。（无 PR，直接提交 f1047e4）

## [0.1.2] - 2026-07-04

这是一个 patch 版本，重点改善地图内编辑提交反馈和高分辨率下的区域标签显示。编辑请求现在由服务端回执驱动界面关闭与失败提示，避免客户端权限误判和重复提交。

### Bug 修复

- 区域创建与样式更新改为等待服务端结果回执：成功时显示气泡并关闭表单，失败时显示气泡并保留表单。（直接提交 6949252）
- 保存按钮提交后进入 10 秒 `已提交` / `Submitted` 禁用状态，并在服务端无回执时超时恢复，防止快速重复提交或无限等待。（直接提交 6949252）
- 区域标签显示判据改为按多边形内可用宽度和字体像素宽度布局，在 2K/高 DPI 场景下不再依赖屏幕面积阈值；过长标签会自动截断，过窄时隐藏内嵌标签。（直接提交 6949252）

### 其他改进

- 新增编辑结果 S2C 网络包和请求 `requestId` 匹配，避免迟到回执影响后续表单。（直接提交 6949252）
- 补充编辑提交等待、编辑结果包、请求包 requestId 和标签布局契约测试。（直接提交 6949252）

## [0.1.1] - 2026-07-04

这是一个 patch 版本，重点改进调色盘数据持久化、高分辨率地图/调色盘显示体验，并补齐版本与清理相关的 Gradle 工具。

### 新功能

- 调色盘历史颜色改为服务端存档共享，并在登录、刷新和颜色保存后同步给玩家。（直接提交 e706db8）
- 调色盘收藏颜色改为玩家个人配置，保存到客户端 `config/xaero_map_region_rev/favourite.json`。（直接提交 e706db8）
- 新增 `bump` Gradle 任务，支持 `VERSION` 指定版本以及 `patch` / `minor` / `major` 递增。（直接提交 f75b3d9）

### Bug 修复

- 改善 2K/高分辨率地图下区域标签过于保守的问题，可读投影面积达到阈值时会显示标签。（直接提交 b8400f6）
- 改善 2K/高 GUI 缩放下调色盘历史色、收藏色和底部按钮重叠/推挤的问题。（直接提交 b8400f6）

### 其他改进

- 将 Xaero World Map 运行期版本与依赖范围集中到 `gradle.properties` 管理。（直接提交 f75b3d9）
- 扩展 `clean` 任务，额外清理 `bin/`、根目录 `logs/`、`.tmp/` 和 `run/logs/`，同时保留端测世界、配置和缓存。（直接提交 7eb3a32）

## [0.1.0] - 2026-07-03

这是 Xaero Map Regions Rev 的首个 MVP 发布版本，面向 Minecraft 1.20.1 + Forge 47.3.33。版本重点是服务端同步区域数据、Xaero 世界地图覆盖层渲染、地图内编辑和基础样式管理。

### 新功能

- 支持在 Xaero's World Map 上渲染服务端同步的多边形区域，包含半透明填充、边界、顶点和标签。
- 支持凹多边形填充、区域标签截断、悬停提示，以及缩放过小时隐藏或缩窄边界/顶点。
- 支持地图内编辑模式、图标化工具栏、可重绑快捷键、草稿顶点撤销/重做、清空草稿和提交草稿。
- 支持区域创建与编辑表单，可设置标签、填充颜色和标签颜色。
- 新增调色盘界面，支持 HSV 色相盘、RGBA 输入/滑条、历史颜色、收藏颜色和收藏释放。
- 支持右键菜单删除区域，编辑入口可一次性修改标签、填充颜色和标签颜色。
- 支持区域管理器分页、选中删除、刷新，以及跳转 Xaero 地图视界到区域中心点。
- 支持服务端区域 SavedData 持久化、客户端缓存同步、刷新冷却和基础权限校验。
- 开发运行期自动加载 Xaero's World Map 与 IMBlocker，不打包进发布产物。

### Bug 修复

- 修复区域覆盖层投影和地图移动倍率不一致的问题，并增加自动校准机制。
- 修复地图缩小时区域标签和边界过度占屏的问题。
- 修复调色盘保存返回表单后颜色被重新初始化覆盖的问题。
- 修复区域管理器在高 GUI 缩放或窄屏下按钮和列表溢出的问题。

### 其他改进

- 构建产物命名为 `Xaero-Map-Regions-Rev-0.1.0+forge-1.20.1.jar`。
- 补充 README、项目规则、契约测试和多轮审查修复。
- 新增个人 Codex skill：`ai-pixel-art-postprocess`，用于将 AI 生成的伪像素图后处理为真实小尺寸像素资产。

<!-- 变更链接 -->

[1.0.0]: https://github.com/ONEGAYI/Xaero-Map-Regions-rev/compare/v0.1.5...v1.0.0
[0.1.5]: https://github.com/ONEGAYI/Xaero-Map-Regions-rev/compare/v0.1.4...v0.1.5
[0.1.4]: https://github.com/ONEGAYI/Xaero-Map-Regions-rev/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/ONEGAYI/Xaero-Map-Regions-rev/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/ONEGAYI/Xaero-Map-Regions-rev/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/ONEGAYI/Xaero-Map-Regions-rev/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/ONEGAYI/Xaero-Map-Regions-rev/commits/v0.1.0
