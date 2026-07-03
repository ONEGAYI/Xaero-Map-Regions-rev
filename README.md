# Xaero Map Regions Rev

Xaero Map Regions Rev 是一个面向 Minecraft 1.20.1 Forge 的区域标注模组，用于在 Xaero's World Map 页面上显示由服务端同步的多边形区域、半透明填充和标签。

当前项目处于 MVP 阶段，重点是稳定的数据同步、地图内编辑、区域标签和基础样式管理。

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
- 选中区域后可通过右键菜单删除区域、修改填充颜色、修改标签文本或标签颜色。
- 区域数据由服务端保存，客户端只缓存和渲染同步数据。
- 区域管理权限默认要求玩家是 OP 且处于创造模式。

## 版本与依赖

| 项目 | 版本 |
|------|------|
| Minecraft | 1.20.1 |
| Forge | 47.3.33 或更高 47.x |
| Java | 17 |
| 模组版本 | 0.1.0 |
| Xaero's World Map | 可选客户端依赖，1.30.4 或更高 |

开发环境的 `runClient` 当前使用 Xaero's World Map `1.41.2`，并会通过 Gradle 自动下载运行期模组；发布 jar 不会打包 Xaero's World Map 或 IMBlocker。

## 安装

将构建产物放入客户端或服务端的 `mods` 目录：

```text
build/libs/Xaero-Map-Regions-Rev-0.1.0+forge-1.20.1.jar
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
- 修改填充颜色。
- 修改标签文本。
- 修改标签颜色。

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
```

Git Bash、Linux 或 macOS：

```bash
./gradlew test
./gradlew clean test build
./gradlew runClient
```

构建产物位于：

```text
build/libs/Xaero-Map-Regions-Rev-0.1.0+forge-1.20.1.jar
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
- 正式发布用的 LICENSE、CHANGELOG 和 Git tag。
