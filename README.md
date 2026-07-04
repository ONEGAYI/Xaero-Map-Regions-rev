# Xaero Map Regions Rev

Xaero Map Regions Rev 是一个面向 Minecraft 1.21.1 NeoForge 的区域标注模组，用于在 Xaero's World Map 页面上显示由服务端同步的多边形区域、半透明填充和标签。

当前项目处于 MVP 阶段，重点是稳定的数据同步、地图内编辑、区域标签和基础样式管理。

主力维护线为 NeoForge 1.21.1；Forge 1.20.1 历史维护线保留在 `forge/1.20.1` 分支。

## 功能

- 在 Xaero 世界地图上渲染服务端同步的多边形区域。
- 支持区域半透明填充、标签文本、标签颜色和填充颜色。
- 支持地图页面内编辑模式按钮。
- 支持可重绑快捷键：
  - 切换区域编辑模式。
  - 打开区域管理器。
  - 提交区域草稿。
  - 清空草稿 / 退出编辑模式。
- 编辑模式下可在地图中添加区域草稿点。
- 选中区域后可通过右键菜单删除区域，或一次性编辑标签文本、填充颜色和标签颜色。
- 颜色编辑支持调色盘、RGBA 输入/滑条、历史颜色和收藏颜色。
- 区域数据由服务端保存，客户端只缓存和渲染同步数据。
- 区域管理权限默认要求玩家是 OP 且处于创造模式。

## 版本与依赖

| 项目 | 版本 |
|------|------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.235 |
| Java | 21 |
| 模组版本 | 见 `gradle.properties` 的 `mod_version` |
| Xaero's World Map | 可选客户端依赖，见 `gradle.properties` 的 `xaero_world_map_version_range` |
| IMBlocker | 仅开发客户端运行期加载，见 `gradle.properties` 的 `imblocker_runtime_version` |

开发环境的 `runClient` 使用 `gradle.properties` 中的 `xaero_world_map_runtime_file_id` 与 `imblocker_runtime_file_id` 解析 Xaero's World Map 和 IMBlocker，配套的 `*_runtime_version` 用于人工核对版本，并会通过 Gradle 自动同步到 `run-client/mods`；发布 jar 不会打包这些外部模组。

开发客户端使用独立的 `run-client` 目录；开发服务端使用独立的 `run-server` 目录，避免客户端运行期模组影响服务端烟测。

## 安装

将构建产物放入客户端或服务端的 `mods` 目录：

```text
build/libs/Xaero-Map-Regions-Rev-<mod_version>+neoforge-1.21.1.jar
```

客户端如需在 Xaero 世界地图上显示区域，需要同时安装 Xaero's World Map。服务端负责保存和同步区域数据。

## 使用

### 地图内编辑

1. 打开 Xaero 世界地图。
2. 点击地图页面右上角的 `Edit` 按钮，或使用“切换区域编辑模式”快捷键进入编辑模式。
3. 在地图上添加区域草稿点。
4. 使用“提交区域草稿”快捷键打开创建表单。
5. 保存后区域会同步到服务端并广播给在线客户端。

### 右键菜单

在编辑模式下选中区域后，使用右键打开区域菜单，可执行：

- 删除区域。
- 编辑区域样式，包括标签文本、填充颜色和标签颜色。

### 命令

服务端命令入口为：

```text
/region
```

当前可用的主要区域命令：

```text
/region createpoly <name> <argb> <x,z;x,z;x,z...>
```

`createpoint`、`hide`、`visible`、`delpoint` 目前只保留 MVP 数据流占位或提示，尚未实现完整点标记和可见性规则。

## 数据保存

区域数据使用 Minecraft `SavedData` 保存，按世界存档和维度数据存储。开发环境和正式游戏环境都会使用对应存档目录下的世界数据；不同 save 之间不会共享区域数据。

客户端缓存会在收到服务端同步包时更新，并在断线时清空。

## 构建与开发

PowerShell：

```powershell
.\gradlew.bat test
.\gradlew.bat clean test build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

Git Bash、Linux 或 macOS：

```bash
./gradlew test
./gradlew clean test build
./gradlew runClient
./gradlew runServer
./gradlew bump -Ppatch
./gradlew bump "-PVERSION=0.1.1"
```

构建产物位于：

```text
build/libs/Xaero-Map-Regions-Rev-<mod_version>+neoforge-1.21.1.jar
```

## 项目状态

已完成的 MVP 能力：

- 服务端区域数据持久化。
- 客户端区域缓存和同步。
- Xaero 世界地图覆盖层渲染。
- 半透明凹多边形填充。
- 地图内编辑入口和右键菜单。
- 基础安全边界：同步包大小限制、刷新冷却、CJK 文本传输长度校验。

仍待后续完善：

- 点标记完整数据模型和渲染。
- 按玩家/团队的区域可见性规则。
- 更完整的图标系统。
- 与 Xaero 公开地图元素 API 的进一步评估和实测。
- 正式发布用的 LICENSE。
