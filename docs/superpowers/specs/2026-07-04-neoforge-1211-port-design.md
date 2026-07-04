# NeoForge 1.21.1 主线迁移设计

## 背景

当前项目是 Minecraft 1.20.1 + Forge 47.3.33 单模块模组，已完成区域数据持久化、网络同步、Xaero World Map 覆盖层渲染、地图内编辑、调色盘与颜色历史等 MVP 能力。后续主力维护版本改为 Minecraft 1.21.1 + NeoForge，Forge 1.20.1 仅作为历史支线保留。

本设计以 2026-07-04 查询到的官方版本为基准：NeoForge 1.21.1 最新稳定线为 `21.1.235`，ModDevGradle 最新版本为 `2.0.141`。NeoForge 1.21.1 的开发环境应使用 Java 21。

迁移前已在当前提交创建本地支线分支：

- `forge/1.20.1`：保留当前 Forge 1.20.1 状态，供后续热修或历史发布使用。
- `master`：继续作为主开发线，迁移到 NeoForge 1.21.1。

## 范围

### 本阶段包含

- 将主线构建目标切到 Minecraft `1.21.1` + NeoForge `21.1.235`。
- 将构建插件从 ForgeGradle 切到 ModDevGradle `2.0.141`。
- 将 Java toolchain 从 17 升级为 21。
- 将模组元数据从 `META-INF/mods.toml` 切换为 `META-INF/neoforge.mods.toml`。
- 将 Forge API 引用迁移到 NeoForge 包名与事件模型。
- 将网络层从 Forge `SimpleChannel` 迁移到 NeoForge payload API。
- 保持现有区域数据模型、NBT 持久化格式、客户端缓存和编辑体验的用户行为不变。
- 调整构建产物命名，使 loader 和 Minecraft 版本在 jar 文件名中明确区分。
- 更新 README、AGENTS 文件树与常用命令，说明 NeoForge 主线和 Forge 支线关系。
- 继续通过单元测试覆盖纯 Java 逻辑和网络包编解码约束。

### 本阶段暂不包含

- 在同一 Gradle 工程内同时构建 Forge 1.20.1 和 NeoForge 1.21.1。
- Fabric、Quilt 或其他 loader 支持。
- 重新设计区域数据格式或做存档迁移器。
- 升级到 Minecraft 1.21.2 或更高版本。
- 重做 Xaero World Map 公开地图元素 API 接入。
- 自动发布或推送远端分支。

## 推荐方案

采用主线直接迁移方案：`master` 成为 NeoForge 1.21.1 主维护线，`forge/1.20.1` 保留迁移前状态。

这个方案避免在同一分支混用 ForgeGradle 与 ModDevGradle，也避免为了双 loader 维护大量条件源码。当前项目的 Forge 依赖主要集中在入口、事件注册、网络、客户端快捷键、配置路径和权限适配层，业务模型和大部分纯 Java 测试可以复用，因此直接迁移比多模块双构建更稳。

## 构建与产物

`gradle.properties` 调整为 NeoForge 主线属性：

- `minecraft_version=1.21.1`
- `artifact_loader=neoforge`
- `neo_version=21.1.235`
- `loader_version_range=[1,)`
- `minecraft_version_range=[1.21.1]`
- `mod_description` 更新为 NeoForge 1.21.1 描述。

`build.gradle` 使用 ModDevGradle：

- 插件：`net.neoforged.moddev` `2.0.141`。
- `java.toolchain.languageVersion = JavaLanguageVersion.of(21)`。
- `neoForge { version = project.neo_version }`。
- run configs 保留 `client`、`server`、`gameTestServer`、`data`。
- `localRuntime` 或等价运行期配置继续承载 Xaero World Map 与 IMBlocker，发布 jar 不打包这些运行期模组。

构建产物必须区分 loader 和 Minecraft 版本：

```text
build/libs/Xaero-Map-Regions-Rev-<mod_version>+neoforge-1.21.1.jar
```

Forge 支线的历史产物保持：

```text
build/libs/Xaero-Map-Regions-Rev-<mod_version>+forge-1.20.1.jar
```

## 模组元数据

资源文件从：

```text
src/main/resources/META-INF/mods.toml
```

迁移为：

```text
src/main/resources/META-INF/neoforge.mods.toml
```

依赖声明规则：

- `neoforge` 为必需依赖，范围从当前 `neo_version` 开始。
- `minecraft` 为必需依赖，范围为 `[1.21.1]`。
- `xaeroworldmap` 保持客户端可选依赖，加载顺序 `AFTER`。

`pack.mcmeta` 继续参与资源属性展开，按 1.21.1 所需 pack format 更新。

## 代码迁移边界

### 入口和事件

主入口 `XaeroRegionsRev` 迁移到 NeoForge 包：

- `net.neoforged.fml.common.Mod`
- `net.neoforged.bus.api.IEventBus`
- `net.neoforged.neoforge.common.NeoForge`
- `net.neoforged.neoforge.event.RegisterCommandsEvent`
- `net.neoforged.neoforge.event.entity.player.PlayerEvent`

入口构造函数接收 mod event bus，并在构造期完成：

1. 注册 payload handlers。
2. 注册客户端初始化入口。
3. 注册 NeoForge game event bus 监听。
4. 保留玩家登录后的全量区域与颜色历史同步。

### 客户端

客户端入口 `XaeroRegionsClient` 继续负责一次性注册：

- key mappings。
- Xaero 地图渲染事件。
- Xaero 地图输入事件。
- 屏幕切换清理。
- 客户端退出服务器时清空本地缓存。

Forge 包名迁移到 NeoForge 包名：

- `RegisterKeyMappingsEvent`
- `ScreenEvent`
- `ClientPlayerNetworkEvent`
- `FMLPaths`
- `Lazy`
- key conflict context 与 key modifier。

如果 1.21.1 NeoForge 的 key conflict API 与 Forge 1.20.1 存在签名差异，优先保留现有语义：快捷键只在 Xaero 世界地图页面或编辑模式中生效。

### 权限适配

`ForgePermissionAdapter` 重命名为 loader 无关的 `PermissionAdapter` 或 `MinecraftPermissionAdapter`。逻辑保持不变：

- OP 等级至少 2。
- 玩家处于创造模式。

重命名后更新命令和网络请求处理器引用，避免 NeoForge 主线继续出现 Forge 命名。

## 网络迁移

NeoForge 1.21.1 不继续使用 Forge `SimpleChannel`。网络层迁移为 payload API：

- 每个网络包 record 实现 `CustomPacketPayload`。
- 每个包声明 `CustomPacketPayload.Type<TYPE>`。
- 每个包声明 `StreamCodec<FriendlyByteBuf, PacketType>` 或等价 codec。
- 现有 `encode(packet, FriendlyByteBuf)` 和 `decode(FriendlyByteBuf)` 逻辑尽量保留，作为 codec 的内部实现。
- `RegionNetwork.register()` 改为监听 `RegisterPayloadHandlersEvent`。
- 服务端处理器从 `NetworkEvent.Context` 迁移到 NeoForge payload context，仍在主线程执行写操作。
- 服务端发送改为 `PacketDistributor.sendToPlayer` 与 `PacketDistributor.sendToAllPlayers`。
- 客户端发送改为 `ClientPacketDistributor.sendToServer`。

当前协议包保持原集合：

- `RegionSyncPacket`
- `ColorHistorySyncPacket`
- `RegionEditResultPacket`
- `CreateRegionRequestPacket`
- `DeleteRegionRequestPacket`
- `UpdateRegionStyleRequestPacket`
- `RegionRefreshRequestPacket`
- `ColorHistoryUpdateRequestPacket`

网络安全边界保持：

- 同步区域数量上限不变。
- 单区域点数与总点数上限不变。
- 字符串长度上限不变。
- 服务端写请求继续执行权限和业务校验。

## Xaero World Map 运行期依赖

`runClient` 继续自动加载 Xaero World Map 和 IMBlocker，不把它们打包进产物。

NeoForge 1.21.1 需要确认可用的 Xaero World Map 运行期坐标：

- 优先使用 Modrinth Maven 上匹配 `neoforge-1.21.1` 的 Xaero World Map artifact。
- 如果 Modrinth Maven 命名与 Forge 线不同，迁移时以实际解析成功的 artifact 为准。
- 如果 IMBlocker 无 NeoForge 1.21.1 运行期版本，开发环境可以暂时仅加载 Xaero World Map，输入法冲突作为手动烟测注意事项记录。

Xaero 接入仍保持在 `client/xaero` 包内，迁移不扩大对 Xaero 内部类的耦合范围。

## 数据兼容

区域 SavedData 的 NBT 字段不因 loader 迁移改变。已有世界存档中的区域数据在 1.21.1 NeoForge 主线下应尽量保持可读。

若 Minecraft 1.21.1 的 `SavedData` API 签名变化导致读取入口需要改动，只改数据层适配代码，不改变 `RegionNbtCodec` 的字段结构。

## 文档更新

迁移实现完成后更新：

- `README.md`：项目定位、版本依赖、安装路径、构建命令、产物文件名。
- `AGENTS.md`：技术约定、常用命令、文件树描述、Forge 支线说明。
- `CHANGELOG.md`：迁移完成后作为新版本条目记录，发布时再补具体版本号。

`CLAUDE.md` 继续只通过 `@AGENTS.md` 引入通用规则，不承载通用迁移说明。

## 测试策略

迁移按 TDD 和小步验证执行：

1. 先为构建产物名补 Gradle 验证任务或测试，确认 jar 名包含 `+neoforge-1.21.1`。
2. 迁移 payload 包时保留并更新现有 packet 往返测试。
3. 迁移权限适配命名后运行权限测试。
4. 迁移数据层后运行 SavedData 与 NBT codec 测试。
5. 迁移客户端事件后运行不依赖游戏启动的客户端状态机测试。
6. 完整编译后运行：

```powershell
.\gradlew.bat clean test build
```

7. 构建通过后再做开发客户端烟测：

```powershell
.\gradlew.bat runClient
```

烟测重点：

- 客户端能启动 NeoForge 1.21.1。
- Xaero World Map 能被开发运行配置加载。
- 进入世界后不崩溃。
- 打开 Xaero 世界地图后区域 overlay 不崩溃。
- 地图内编辑、创建、同步、删除区域的核心流程仍可走通。
- 生成 jar 文件名包含 `neoforge-1.21.1`。

## 验收标准

- 本地存在 `forge/1.20.1` 分支，指向迁移前 Forge 状态。
- `master` 构建目标为 Minecraft 1.21.1 + NeoForge。
- `.\gradlew.bat clean test build` 通过。
- 构建产物名为 `Xaero-Map-Regions-Rev-<mod_version>+neoforge-1.21.1.jar`。
- 源码主线不再依赖 `net.minecraftforge.*` 包。
- `META-INF/neoforge.mods.toml` 正确声明 `neoforge`、`minecraft` 和可选 `xaeroworldmap` 依赖。
- 网络包使用 NeoForge payload API 注册和发送。
- 区域 NBT 持久化格式保持兼容。
- README 和 AGENTS 中的版本、命令、文件树与实际项目一致。
