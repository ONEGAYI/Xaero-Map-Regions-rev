# 多子项目重构设计

> 日期：2026-07-10
> 状态：已确认，待编写实施计划

## 1. 背景与动机

### 1.1 当前状况

项目通过 Git 分支维护两个平台：

- `master` 分支：NeoForge 1.21.1（Java 21，ModDevGradle），59 个 Java 源文件
- `forge/1.20.1` 分支：Forge 1.20.1（Java 17，ForgeGradle），54 个 Java 源文件

两端 53 个共有文件中，**40 个完全相同**（零 diff），差异集中在网络层 API、模组入口、少量功能分叉。

### 1.2 核心痛点

1. **代码漂移**：分支间独立演进，相同文件逐渐分叉。例如 `RegionNbtCodec` master 端已增加 NBT 字段长度校验，forge 端缺失
2. **维护成本翻倍**：每个改动需要手动同步到两个分支
3. **扩展困难**：将来引入 Fabric 等加载器需要再开一个分支，漂移问题加剧

### 1.3 目标

将两个分支合并到一个单分支多子项目结构中，抽离共用逻辑到公共模块，使每个加载器+版本组合成为一个 Gradle 子项目。结构以加载器为一级目录、MC 版本为二级子项目，为将来扩展 Fabric 等加载器铺路。

## 2. 目录结构

```text
Xaero-Map-Regions-rev/
├── settings.gradle              # include 'common', 'neoforge:mc-1.21.1', 'forge:mc-1.20.1'
├── build.gradle                 # 根项目，仅全局配置 + bump 任务（不 apply MC 插件）
├── gradle.properties            # 全局共享属性（mod_version、mod_id、mod_name 等）
├── gradle/
│   ├── region-repositories.gradle     # 统一仓库声明（mavenCentral + Modrinth + CurseMaven）
│   └── region-platform.gradle         # 平台共享逻辑（jar manifest 模板）
├── common/
│   ├── build.gradle             # apply java-library；Java 17
│   └── src/
│       ├── main/java/           # 纯 Java 公共代码（零 MC 平台依赖）
│       ├── main/resources/
│       │   └── assets/          # 共享资源（lang × 2、textures × 2）
│       └── test/java/           # 纯 Java 测试
├── neoforge/
│   └── mc-1.21.1/
│       ├── build.gradle         # apply ModDevGradle + region-platform；Java 21
│       ├── gradle.properties    # neo_version、MC 1.21.1 专属属性
│       └── src/main/
│           ├── java/            # NeoForge 适配层 + MC API 依赖的代码
│           └── resources/       # neoforge.mods.toml、pack.mcmeta(pack_format=34)
├── forge/
│   └── mc-1.20.1/
│       ├── build.gradle         # apply ForgeGradle + region-platform；Java 17
│       ├── gradle.properties    # forge_version、MC 1.20.1 专属属性
│       └── src/main/
│           ├── java/            # Forge 适配层 + MC API 依赖的代码
│           └── resources/       # mods.toml、pack.mcmeta(pack_format=15)
├── gradle/                      # Wrapper（共用，Gradle 8.8）
├── docs/                        # 文档（共用）
└── CHANGELOG.md / README.md / AGENTS.md / CLAUDE.md  # 根级共用
```

**设计要点**：

- 嵌套子项目通过 Gradle `include 'neoforge:mc-1.21.1'` 自动映射到 `neoforge/mc-1.21.1/` 目录
- 将来加 Fabric 只需 `fabric/mc-1.21.1/` + include
- buildSrc convention 插件集中管理所有平台共享的 Gradle 逻辑

## 3. common 模块边界

### 3.1 进入 common 的文件

**`region/` 包 — 13 个文件**

| 文件 | 来源 | 说明 |
|------|------|------|
| `ArgbColor.java` | 两端相同 | |
| `ColorPaletteLimits.java` | 两端相同 | |
| `PermissionProfile.java` | 两端相同 | |
| `PointMarker.java` | 两端相同 | |
| `PolygonMath.java` | 两端相同 | |
| `Region.java` | 两端相同 | |
| `RegionColorParser.java` | 两端相同 | |
| `RegionId.java` | 两端相同 | |
| `RegionPoint.java` | 两端相同 | |
| `RegionRequestValidator.java` | 两端相同 | |
| `RegionStyleUpdater.java` | 两端相同 | |
| `RegionLimits.java` | master 版本 | 下沉 forge 缺少的 4 个常量（MAX_ID_LENGTH、MAX_DIMENSION_LENGTH、MAX_CATEGORY_LENGTH、MAX_ICON_NAME_LENGTH） |
| `RegionNbtCodec.java` | master 版本 | 下沉 boundedString 校验逻辑；改用 NbtCompound/NbtList 接口 |

**`region/nbt/` 包 — 3 个抽象接口（新建）**

| 文件 | 说明 |
|------|------|
| `NbtCompound.java` | 接口：putString/getString/putInt/getInt/putLong/getLong/put/getList/contains/remove（10 个方法） |
| `NbtList.java` | 接口：add/getCompound/size |
| `NbtFactory.java` | 接口：createCompound/createList，供 writeRegion 创建 NBT 实例 |

**`network/buffer/` 包 — 1 个抽象接口（新建）**

| 文件 | 说明 |
|------|------|
| `PacketBuffer.java` | 接口：writeUtf/readUtf/writeInt/readInt/writeBoolean/readBoolean/writeLong/readLong/writeIntArray/readIntArray 等 payload 用到的方法（约 12 个） |

**`network/data/` 包 — 8 个 payload data record（从现有 payload 拆出）**

将 8 个 payload 的 encode/decode 纯逻辑提取为不依赖任何 MC 类型的 data record：

| 文件 | 拆自 |
|------|------|
| `RegionSyncData.java` | RegionSyncPacket |
| `CreateRegionRequestData.java` | CreateRegionRequestPacket |
| `DeleteRegionRequestData.java` | DeleteRegionRequestPacket |
| `UpdateRegionStyleRequestData.java` | UpdateRegionStyleRequestPacket |
| `ColorHistorySyncData.java` | ColorHistorySyncPacket |
| `ColorHistoryUpdateRequestData.java` | ColorHistoryUpdateRequestPacket |
| `RegionRefreshData.java` | RegionRefreshRequestPacket |
| `RegionEditResultData.java` | RegionEditResultPacket |

每个 data record 暴露 `static encode(PacketBuffer, Data)` 和 `static decode(PacketBuffer)` 方法。平台子项目的 payload wrapper 类负责实现 `CustomPacketPayload`（NeoForge）或注册到 `SimpleChannel`（Forge），内部委托给 data record。

**`service/` 包 — 2 个文件**

| 文件 | 说明 |
|------|------|
| `ServerContext.java` | 接口：overworld()、getLevel(dim)、getRegionStore() |
| `RegionStore.java` | 接口：loadAll()、saveAll(regions) — 桥接平台端 RegionSavedData |
| `RegionService.java` | 两端代码相同，改用 ServerContext/RegionStore 接口替代 MinecraftServer/ServerLevel |

**`client/` 包 — 4 个文件**

| 文件 | 说明 |
|------|------|
| `EditorOpener.java` | 接口：openEditor(...) — 抽象打开 Screen 的动作，避免 common 依赖 Screen 类 |
| `ClientRegionEditResultHandler.java` | 两端代码相同，改用 EditorOpener |
| `ClientRegionCache.java` | LogUtils→LoggerFactory，消除 MC 日志依赖 |
| `ClientColorHistoryCache.java` | LogUtils→LoggerFactory，消除 MC 日志依赖 |

**`client/editor/` 包 — 5 个纯逻辑文件**

ColorPickerModel、RegionEditSession、RegionManagerListModel、RegionSelection、RegionSubmissionState（两端相同）

**`client/xaero/` 包 — 4 个文件**

RegionLabelDisplay、RegionRenderStyle、XaeroMapInputRouter（零依赖）；PolygonTriangulator（依赖 org.joml.Vector2f，加 JOML 依赖）

**资源文件**

`common/src/main/resources/assets/xaeroregionsrev/` 下放 4 个共享资源：lang/en_us.json、lang/zh_cn.json、textures/gui/color_palette_icon.png、textures/gui/region_editor_icons.png

**common 文件总计：约 40 个 main 文件 + 约 20 个纯 Java 测试文件**

### 3.2 不进入 common 的文件（留在各平台子项目）

**MC 版本 API 不兼容（两端各自维护）：**

- `PolygonFillRenderer.java` — BufferBuilder API 1.20↔1.21 不兼容
- `RegionSavedData.java` — SavedData API 变更（Factory + HolderLookup.Provider）
- `ColorPickerScreen.java`、`RegionManagerScreen.java`、`RegionStyleEditScreen.java` — GUI 渲染层次（1.21 renderBackground 签名变更）

**MC GUI/平台依赖（两端代码相同但不抽象，因 GUI 耦合太深）：**

- `RegionContextMenu.java` — 大量 GuiGraphics 调用
- `XaeroScreenDetector.java` — 依赖 Screen 类层级

**加载器适配层（各自独立）：**

- `XaeroRegionsRev.java` — @Mod 入口
- `XaeroRegionsClient.java` — 事件总线注册
- `RegionNetwork.java` — 网络注册（NeoForge PayloadRegistrar / Forge SimpleChannel）
- `RegionEditRequestHandler.java` — payload handler 签名差异
- `RegionKeyMappings.java` — 按键注册 import 差异
- `XaeroMapOverlayRenderer.java` — import + forge 多一个 register() 静态块
- 各 payload wrapper 类（CustomPacketPayload + StreamCodec / SimpleChannel 适配 *Data）
- `PermissionAdapter`（两端实现完全相同，均调用 player.hasPermissions(2) + gameMode.isCreative()，但依赖 MC Player 类无法进 common，留在平台；common 中已有纯数据类 PermissionProfile 表达权限结果）

**master 独有功能（先留 NeoForge 子项目）：**

- `ClientLocalConfig.java` + 自动校准开关逻辑（MapProjectionAdapter 中的 calibration 相关方法）
- `ClientboundPayloadBridge.java` / `ClientboundPayloadDispatch.java`
- `RegionClientCommands.java`（客户端命令）

## 4. 抽象接口设计

### 4.1 设计原则

- 接口只暴露业务逻辑实际使用的方法子集，不做全量镜像
- 平台实现类命名规则：`{MC类名}{接口名}`（如 `FriendlyByteBufPacketBuffer`、`CompoundTagNbtCompound`）
- 接口放在 common 的对应包下，平台实现放在平台子项目的对应包下

### 4.2 网络层：PacketBuffer

```java
// common: network/buffer/PacketBuffer.java
// 仅包含 8 个 payload 实际使用的方法（经源码逐行核对）
public interface PacketBuffer {
    void writeUtf(String value, int maxLength);
    String readUtf(int maxLength);
    void writeInt(int value);
    int readInt();
    void writeLong(long value);
    long readLong();
    void writeBoolean(boolean value);
    boolean readBoolean();
    void writeVarInt(int value);
    int readVarInt();
}
```

> 方法集经全部 8 个 payload 源码逐行核对，writeUtf/readUtf 均带 maxLength 参数，不含 writeIntArray/readIntArray（payload 用 writeVarInt + 循环 writeInt 代替）。

平台实现 `FriendlyByteBufPacketBuffer` 包装 `FriendlyByteBuf`，逐方法委托。两端 FriendlyByteBuf 的这 10 个方法签名在 1.20.1 和 1.21.1 完全一致，适配器代码可共用接口定义。

payload data record 的 encode/decode 签名改为操作 `PacketBuffer` 而非 `FriendlyByteBuf`。平台 wrapper 在注册 payload 时负责 `FriendlyByteBuf` ↔ `PacketBuffer` 的适配转换。

### 4.3 NBT 层：NbtCompound / NbtList / NbtFactory

```java
// common: region/nbt/NbtCompound.java
// 仅包含 RegionNbtCodec 及其测试实际使用的方法
public interface NbtCompound {
    void putString(String key, String value);
    String getString(String key);
    void putInt(String key, int value);
    int getInt(String key);
    void putLong(String key, long value);
    long getLong(String key);
    void put(String key, NbtList list);
    NbtList getList(String key, int type);
    boolean contains(String key, int type);
    void remove(String key);  // RegionNbtCodecTest 使用

    // NBT type id 常量（与 Minecraft TagTags 对应）
    int TAG_INT = 3;
    int TAG_LONG = 4;
    int TAG_STRING = 8;
    int TAG_LIST = 9;
    int TAG_COMPOUND = 10;
}

// common: region/nbt/NbtList.java
public interface NbtList {
    void add(NbtCompound compound);
    NbtCompound getCompound(int index);
    int size();
}

// common: region/nbt/NbtFactory.java
// 用于 writeRegion 创建 NBT 实例，避免 common 直接 new MC 类
public interface NbtFactory {
    NbtCompound createCompound();
    NbtList createList();
}
```

> 方法集经 RegionNbtCodec 源码及其测试逐行核对。`remove` 方法仅在测试中使用（删除字段验证必填校验），但纳入接口以支持 common 测试。

平台实现 `CompoundTagNbtCompound` 包装 `CompoundTag`，`ListTagNbtList` 包装 `ListTag`，`CompoundTagNbtFactory` 实现 `NbtFactory`。

适配器需提供 `raw()` 方法返回底层 MC 对象（`CompoundTag` 或 `ListTag`），供平台 `RegionSavedData` 在 `ListTag.add()` 等场景拆包。

**视图语义约定：** `getList` 返回的是底层 `ListTag` 的**视图包装**（与原生 `CompoundTag.getList` 行为一致），修改会反映到底层，不做拷贝。

`RegionNbtCodec` 的 `writeRegion` 签名改为 `writeRegion(NbtFactory factory, Region region)` 返回 `NbtCompound`；`readRegion(NbtCompound tag)` 不需要工厂参数。平台子项目在 SavedData 中调用时负责 `CompoundTag` ↔ `NbtCompound` 的适配转换。

`boundedString()` 方法纯 Java，不涉及 MC 类型，直接保留在 `RegionNbtCodec` 中。

> **colorHistory 的 NBT 序列化**（`IntTag.valueOf(color.value())` 等直接操作）不进 common，留在各平台 `RegionSavedData` 中。`RegionStore` 接口的 `colorHistory()`/`rememberColor()` 返回 `List<ArgbColor>` 纯 Java 类型，抽象层不暴露 NBT 细节。

### 4.4 服务端：ServerContext + RegionStore

RegionSavedData 因 SavedData API（1.20 vs 1.21 的 Factory + HolderLookup.Provider）不兼容，留在各平台子项目。为了让 common 的 RegionService 能读写持久化数据，需要两层抽象：

```java
// common: service/ServerContext.java
public interface ServerContext {
    RegionStore overworld();
    RegionStore getLevel(String dimensionKey);
    Iterable<RegionStore> allLevels();
}
```

```java
// common: service/RegionStore.java
// 方法集与现有 RegionSavedData 的公共 API 对齐
public interface RegionStore {
    Collection<Region> allRegions();
    Optional<Region> find(RegionId id);
    void put(Region region);
    boolean remove(RegionId id);
    List<ArgbColor> colorHistory();
    void rememberColor(ArgbColor color, int limit);
}
```

`RegionService` 从直接依赖 `MinecraftServer`/`ServerLevel` 改为依赖 `ServerContext`/`RegionStore`，方法签名从 `list(ServerLevel)` 改为 `list(RegionStore)`。

**调用方改造：** 现有调用方（`RegionCommands`、`RegionEditRequestHandler`、`XaeroRegionsRev`）手里只有 `ServerLevel`/`MinecraftServer`。平台子项目需提供桥接工具方法（如 `RegionStores.of(ServerLevel)` 返回 `RegionSavedDataStore` 包装），供调用方在不改动业务逻辑的情况下完成转换。具体调用点改造代码在实施计划中给出。

各平台实现：

- `NeoForgeServerContext` / `ForgeServerContext`：包装 `MinecraftServer`，`overworld()` / `allLevels()` 返回 `RegionSavedDataStore` 包装
- `RegionSavedDataStore`：各平台实现 `RegionStore`，委托给本平台的 `RegionSavedData`（NeoForge 用 1.21 API，Forge 用 1.20 API）

> **colorHistory 的 NBT 序列化**（`IntTag.valueOf` 等直接操作）留在各平台 `RegionSavedData` 中，不进 common。Forge 端需手动补 master 的 `Math.min(colorList.size(), ColorPaletteLimits.MAX_COLORS)` 上限保护。

### 4.5 客户端：EditResultHandler

```java
// common: client/EditResultHandler.java
// 接收编辑结果并转发给当前编辑器屏幕的回调接口
public interface EditResultHandler {
    void handleEditResult(long requestId, boolean success, boolean closeScreen, String message);
}
```

> 接口必须包含 `closeScreen` 参数——`RegionEditResultPacket` 有 4 个字段（requestId、success、closeScreen、message），`RegionSubmissionState.receive` 依赖 `success && closeScreen` 判断是否关闭屏幕。省略 closeScreen 会破坏关屏逻辑。

`ClientRegionEditResultHandler` 原来调用 `Minecraft.getInstance().screen instanceof RegionStyleEditScreen` 并转发 packet，改为通过 `EditResultHandler` 回调。平台子项目注入实现（检查当前 Screen 类型并转发）。`ClientboundPayloadBridge`（NeoForge）/ `RegionNetwork` 的 consumer（Forge）负责从 packet 拆字段调用 `ClientRegionEditResultHandler.handle(requestId, success, closeScreen, message)`。

### 4.6 不抽象的层

GUI 渲染层（`GuiGraphics`、`Component`、`Screen`）不做抽象。原因：

- `GuiGraphics` 有数十个渲染方法且 1.20→1.21 签名有变（如 renderBackground 参数变了）
- `RegionContextMenu` 和 `XaeroScreenDetector` 直接调用 GUI API，抽象代价接近重写渲染层
- 这两个文件留在各平台子项目，两端代码当前完全相同，通过约定保持手动同步

## 5. 构建系统配置

### 5.1 settings.gradle

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { name = 'NeoForged';      url = 'https://maven.neoforged.net/releases' }
        maven { name = 'MinecraftForge'; url = 'https://maven.minecraftforge.net/' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.7.0'
}

rootProject.name = 'Xaero-Map-Regions-rev'

include 'common'
include 'neoforge:mc-1.21.1'
include 'forge:mc-1.20.1'
```

两个 MC 插件 maven 仓库同时声明，各自子项目的 `plugins {}` 块只请求对应插件。

### 5.2 gradle.properties 分层

**根 gradle.properties（全局共享）：**

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false

# 模组元数据（所有平台共享）
mod_id=xaeroregionsrev
mod_group_id=com.suian.xaeroregionsrev
mod_name=Xaero Map Regions Rev
mod_license=MIT
mod_version=0.1.5
mod_authors=SUIAN
archives_base_name=Xaero-Map-Regions-Rev

# common 依赖
joml_version=1.10.5
```

**neoforge/mc-1.21.1/gradle.properties：**

```properties
minecraft_version=1.21.1
artifact_loader=neoforge
neo_version=21.1.235
neo_version_range=[21.1.0,)
loader_version_range=[1,)
minecraft_version_range=[1.21.1]
xaero_world_map_runtime_version=1.39.12
xaero_world_map_runtime_file_id=6778114
imblocker_runtime_version=5.5.1
imblocker_runtime_file_id=7986164
xaero_world_map_version_range=[1.39.0,)
mod_description=Server-synced polygon regions rendered on Xaero's World Map for NeoForge 1.21.1.
```

**forge/mc-1.20.1/gradle.properties：**

```properties
minecraft_version=1.20.1
artifact_loader=forge
forge_version=47.3.33
loader_version_range=[47,)
forge_version_range=[47.3.33,)
minecraft_version_range=[1.20.1,1.21)
xaero_world_map_runtime_version=1.41.2
xaero_world_map_version_range=[1.30.4,)
imblocker_runtime_file_id=4626679
mod_description=Server-synced polygon regions rendered on Xaero's World Map for Forge 1.20.1.
```

### 5.3 共享 Gradle 脚本

不使用 buildSrc（避免预编译脚本插件与 `apply from:` 的机制冲突），共享脚本放在 `gradle/` 目录：

```text
gradle/
├── region-repositories.gradle   # 仓库声明（mavenCentral + Modrinth + CurseMaven）
└── region-platform.gradle       # jar manifest 模板
```

`bump` 任务注册在根 `build.gradle`（避免子项目并行执行冲突），指向根 `gradle.properties`。

**region-repositories.gradle** — 所有子项目 apply：

```groovy
repositories {
    mavenCentral()
    maven {
        name = 'Modrinth'
        url = 'https://api.modrinth.com/maven'
        content { includeGroup 'maven.modrinth' }
    }
    maven {
        name = 'CurseMaven'
        url = 'https://cursemaven.com'
        content { includeGroup 'curse.maven' }
    }
}
```

**region-platform.gradle** — 平台子项目 apply：

- jar manifest 属性模板（Specification-Title 等）
- 不含 bump 任务（bump 统一在根 build.gradle 注册）

### 5.4 common/build.gradle

```groovy
plugins {
    id 'java-library'
}

apply from: "$rootDir/gradle/region-repositories.gradle"

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

dependencies {
    api "org.joml:joml:${joml_version}"
    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test { useJUnitPlatform() }
```

Java 17（两端最低值），保证 forge 17 和 neoforge 21 都能引用。common 中不得使用 Java 18+ 语法。

### 5.5 neoforge/mc-1.21.1/build.gradle

```groovy
plugins {
    id 'net.neoforged.moddev' version '2.0.141'
}

apply from: "$rootDir/gradle/region-platform.gradle"

version = "${mod_version}+${artifact_loader}-${minecraft_version}"
java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = project.neo_version
    runs {
        // 沿用 master 的 run-client/run-server/run-data/gameTestServer 配置
    }
    mods {
        "${mod_id}" {
            sourceSet project(':common').sourceSets.main
            sourceSet sourceSets.main
        }
    }
    unitTest { enable(); testedMod = mods."${mod_id}" }
}

dependencies {
    implementation project(':common')
    // clientRuntimeMods（CurseMaven 文件 ID 方式，沿用 master）
    // testImplementation（JUnit）
}

processResources {
    from(project(':common').sourceSets.main.resources)
}

// syncClientRuntimeMods、verifyArchiveName 任务沿用 master
```

### 5.6 forge/mc-1.20.1/build.gradle

```groovy
plugins {
    id 'net.minecraftforge.gradle' version '[6.0.16,6.2)'
}

apply from: "$rootDir/gradle/region-platform.gradle"

version = "${mod_version}+${artifact_loader}-${minecraft_version}"
java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings channel: 'official', version: minecraft_version
    runs {
        configureEach {
            mods {
                "${mod_id}" {
                    source project(':common').sourceSets.main
                    source sourceSets.main
                }
            }
            // 沿用 forge 的 property 配置
        }
    }
}

dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"
    implementation project(':common')
    // clientRuntimeMods + fg.deobf（沿用 forge 方式）
}

processResources {
    from(project(':common').sourceSets.main.resources)
}

// reobfJar、runClient classpath += 沿用 forge
```

### 5.7 sourceSet 纳入机制

核心设计：平台子项目通过 **两层机制** 消费 common：

1. `mods {}` 块用 `sourceSet project(':common').sourceSets.main` 告诉加载器 common 的源码属于该 mod（dev 运行时识别 mod 类和资源）
2. `dependencies { implementation project(':common') }` 把 common 的编译产物和传递依赖（如 JOML）加入 classpath

两者**叠加使用**，缺一不可。仅 sourceSet 纳入不会把 common 的依赖（JOML）加到平台 classpath；仅 project 依赖不会让 dev 运行时 mod 加载器识别 common 的类。

common 源码由 common 项目的 Java 17 toolchain 独占编译，平台子项目只消费其编译产物（Java 17 字节码在 Java 21 runtime 下向后兼容）。

### 5.8 资源文件共享

assets 资源放 `common/src/main/resources/assets/`，各平台通过 `processResources { from(project(':common').sourceSets.main.resources) }` 合并到最终 jar。

约定：`assets/` 下的资源只放 common；平台子项目的 resources 目录只放 `META-INF/*.toml` 和 `pack.mcmeta`，职责不交叉，避免文件冲突。

META-INF toml 文件和 pack.mcmeta 因加载器/MC 版本不同（文件名、依赖声明、pack_format），必须各平台独立维护。

### 5.9 共享 Gradle 任务

根 build.gradle 提供 `buildAll` 任务：

```groovy
tasks.register('buildAll') {
    dependsOn ':neoforge:mc-1.21.1:build', ':forge:mc-1.20.1:build'
}
```

`bump` 任务通过 region-platform.gradle 注册到各平台子项目，修改根 gradle.properties 的 mod_version。

## 6. 迁移步骤

### 6.1 阶段总览

```text
阶段 0  分支准备
  └→ 阶段 1  Gradle 骨架搭建 + common 子项目可编译
       └→ 阶段 2  NeoForge 子项目迁移（master 功能完整）
            └→ 阶段 3  Forge 子项目迁移（forge/1.20.1 功能完整）
                 └→ 阶段 4  清理与文档
```

每个阶段是可独立验证的里程碑，前一阶段不通过不进下一阶段。

### 6.2 阶段 0：分支准备

1. 从 master 创建重构分支 `refactor/multi-project-structure`
2. 确认工作树干净
3. 标记当前状态为回滚点

### 6.3 阶段 1：Gradle 骨架 + common 可编译

**目标**：多项目结构成形，common 子项目的纯 Java 代码和测试能独立 `./gradlew :common:test` 通过。

**步骤**：

1. 创建 settings.gradle（include common + 两平台子项目）
2. 创建 buildSrc/ 及 convention 脚本
3. 拆分 gradle.properties → 根 + 各平台
4. 创建 common/build.gradle（java-library，Java 17，JOML 依赖）
5. 搬入第一批 common 文件（零依赖 + 改 import，不需要抽象接口）：
   - region/ 11 个零依赖文件
   - region/RegionLimits.java（master 版本）
   - client/editor/ 5 个纯逻辑文件
   - client/xaero/ 3 个零依赖文件 + PolygonTriangulator（JOML）
   - client/ClientRegionCache.java、ClientColorHistoryCache.java（LogUtils→LoggerFactory）
   - common/src/main/resources/assets/ 4 个资源文件
6. 搬入 common 的纯 Java 测试文件（约 20 个）
7. **验证点**：`./gradlew :common:test` 全绿

**风险**：根 gradle.properties 分层后属性可见性。Gradle 子项目默认能读根 gradle.properties，但子项目自己的 gradle.properties 会覆盖同名键——需确认键名不冲突。

### 6.4 阶段 2：NeoForge 子项目迁移

**目标**：`./gradlew :neoforge:mc-1.21.1:runClient` 能启动游戏，所有 NeoForge 功能正常。

**步骤**：

1. 创建 neoforge/mc-1.21.1/build.gradle（ModDevGradle + region-platform + common sourceSet）
2. 创建 neoforge/mc-1.21.1/gradle.properties
3. 搬入平台特有资源（neoforge.mods.toml、pack.mcmeta）
4. 创建抽象接口（网络 + NBT + 平台），并实现 NeoForge 端适配器：
   - PacketBuffer + FriendlyByteBufPacketBuffer
   - NbtCompound/NbtList + CompoundTagNbtCompound/ListTagNbtList
   - ServerContext + NeoForgeServerContext
   - EditorOpener + NeoForge 端实现
5. 搬入 payload data 层（8 个 *Data.java）到 common，从现有 payload 拆出 encode/decode
6. 搬入 RegionNbtCodec 到 common（改用 NbtCompound/NbtList）
7. 搬入 RegionService 到 common（改用 ServerContext）
8. 搬入 ClientRegionEditResultHandler 到 common（改用 EditorOpener）
9. NeoForge 适配层（留平台子项目）：
   - XaeroRegionsRev、XaeroRegionsClient、RegionNetwork、RegionEditRequestHandler
   - 各 payload wrapper（CustomPacketPayload + StreamCodec 适配 *Data）
   - RegionKeyMappings、PermissionAdapter、ColorPickerScreen、RegionManagerScreen 等 GUI
   - master 独有功能：ClientLocalConfig、ClientboundPayloadBridge/Dispatch、RegionClientCommands、MapProjectionAdapter 校准逻辑
10. **验证点**：
    - `./gradlew :neoforge:mc-1.21.1:test` 全绿
    - `./gradlew :neoforge:mc-1.21.1:build` 产物 jar 文件名正确
    - `./gradlew :neoforge:mc-1.21.1:runClient` 能启动、进入世界、创建/编辑区域

**风险**：

- payload 拆分 data+wrapper 时 encode/decode 方法签名变化（FriendlyByteBuf → PacketBuffer），所有调用点都要更新
- mods {} sourceSet 纳入 common 后 IDE 可能需要重新导入项目

### 6.5 阶段 3：Forge 子项目迁移

**目标**：`./gradlew :forge:mc-1.20.1:runClient` 能启动游戏，Forge 功能正常。

**步骤**：

1. 创建 forge/mc-1.20.1/build.gradle（ForgeGradle + region-platform + common sourceSet）
2. 创建 forge/mc-1.20.1/gradle.properties
3. 搬入平台特有资源（mods.toml、pack.mcmeta）
4. 创建 Forge 端抽象接口实现：
   - FriendlyByteBufPacketBuffer（forge 版，FriendlyByteBuf 1.20.1 签名）
   - CompoundTagNbtCompound（forge 版）
   - ForgeServerContext
   - Forge 端 EditorOpener
5. Forge 适配层（留平台子项目）：同阶段 2 的平台文件，但用 Forge API
6. **验证点**：
    - `./gradlew :forge:mc-1.20.1:test` 全绿
    - `./gradlew :forge:mc-1.20.1:build` 产物 jar 正确
    - `./gradlew :forge:mc-1.20.1:runClient` 能启动、功能正常

**风险**：

- Forge 的 fg.deobf() + reobfJar 在多项目中的行为
- Forge 1.20.1 的 FriendlyByteBuf/CompoundTag 方法签名可能与 1.21.1 有细微差异（需在实现适配器时验证）

### 6.6 阶段 4：清理与文档

1. 删除旧的 src/main/java、src/test/java、src/main/resources（已全部迁移）
2. 更新根 build.gradle（清理、添加 buildAll 任务）
3. 更新 .gitignore（各平台运行目录）
4. 更新 AGENTS.md 文件树和构建命令
5. 更新 CHANGELOG.md
6. 删除 forge/1.20.1 分支（内容已合并到主仓库多项目结构中）
7. **最终验证**：`./gradlew buildAll` 两端都构建成功

### 6.7 回滚策略

- 每个阶段结束时创建一个 Git commit，含可运行的状态
- 如果某阶段验证失败，git reset 到上一阶段 commit
- 重构分支 refactor/multi-project-structure 不影响 master，master 始终可用
- 整个重构完成后，在重构分支上人工验收通过才合并回 master

### 6.8 工作量估算

| 阶段 | 估时 | 说明 |
|------|------|------|
| 阶段 0 | 0.5h | 分支创建 |
| 阶段 1 | 4-6h | Gradle 骨架 + common 可编译（含测试） |
| 阶段 2 | 8-12h | NeoForge 迁移（含抽象接口、payload 拆分、烟测） |
| 阶段 3 | 6-8h | Forge 迁移（大部分接口已定义，主要是适配实现） |
| 阶段 4 | 2-3h | 清理与文档 |
| 合计 | 约 4-5 天 | |

## 7. 调研依据

本设计基于以下调研数据：

- 两分支共 53 个共有文件中 40 个零 diff（完整 diff 行数统计）
- 13 个有差异文件的逐行 diff 分类（A 类加载器适配 16 个、B 类 MC 版本 1 个、C 类功能分叉 5 个）
- 26 个相同 main 文件的 import 依赖分析（19 个零平台依赖、2 个仅日志、1 个 JOML、4 个 MC API 绑定）
- 测试代码对比（34 个共有测试，23 个完全一致，11 个有差异）
- ModDevGradle 与 ForgeGradle 插件兼容性验证（可在不同子项目共存，settings.gradle 同时声明两个 maven）
- 两端 Gradle Wrapper 版本一致（8.8）
- 两端 build.gradle、gradle.properties、settings.gradle、资源文件的完整对比
