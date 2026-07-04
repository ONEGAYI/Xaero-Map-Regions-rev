# NeoForge 1.21.1 Port Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将主线从 Minecraft 1.20.1 Forge 迁移到 Minecraft 1.21.1 NeoForge，并保证构建产物名明确包含 `neoforge-1.21.1`。

**Architecture:** `master` 直接成为 NeoForge 1.21.1 主维护线，`forge/1.20.1` 保留迁移前 Forge 状态。业务模型、NBT 格式、客户端编辑状态和 Xaero overlay 逻辑尽量复用；loader 迁移集中在构建、元数据、入口事件、网络 payload、客户端事件和权限适配层。

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1.235, ModDevGradle 2.0.141, JUnit 5, Xaero World Map runtime dependency.

---

## Migration Closure Rule

Tasks 2-7 migrate interdependent build, loader, network, client, and Minecraft API surfaces. During execution, do not treat `compileJava` or packet tests as meaningful until the Task 7 compile-repair step closes the migration. If an earlier task reaches a commit step while main sources cannot compile because later migration tasks are still pending, skip that commit and create one combined API-migration commit after Task 7 passes `compileJava` and the targeted tests.

Do not hide pre-existing failures: Task 1 must still run the Forge baseline tests before the build migration begins.

## File Structure

- Modify: `build.gradle`
  - 切换到 ModDevGradle。
  - 配置 NeoForge runs、资源展开、运行期模组依赖、产物命名和 `verifyArchiveName`。
- Modify: `gradle.properties`
  - 设置 `minecraft_version=1.21.1`、`artifact_loader=neoforge`、`neo_version=21.1.235`。
- Modify: `settings.gradle`
  - 增加 NeoForged Maven plugin repository。
- Delete: `src/main/resources/META-INF/mods.toml`
  - Forge 元数据不再进入 NeoForge 主线产物。
- Create: `src/main/resources/META-INF/neoforge.mods.toml`
  - NeoForge 元数据和依赖声明。
- Modify: `src/main/resources/pack.mcmeta`
  - 更新 1.21.1 资源包格式。
- Modify: `src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java`
  - 迁移到 NeoForge mod entrypoint 和 event bus。
- Modify: `src/main/java/com/suian/xaeroregionsrev/command/RegionCommands.java`
  - 使用 NeoForge `RegisterCommandsEvent`，改用 loader-neutral 权限适配器。
- Move: `src/main/java/com/suian/xaeroregionsrev/platform/ForgePermissionAdapter.java` -> `src/main/java/com/suian/xaeroregionsrev/platform/MinecraftPermissionAdapter.java`
  - 去掉 Forge 命名，保留权限规则。
- Modify: `src/main/java/com/suian/xaeroregionsrev/network/RegionNetwork.java`
  - 改为 NeoForge payload 注册、发送和 client-to-server helper。
- Create: `src/main/java/com/suian/xaeroregionsrev/network/ClientboundPayloadDispatch.java`
  - 在 common 网络注册中安全转发 clientbound payload，避免 dedicated server 直接加载客户端类。
- Create: `src/main/java/com/suian/xaeroregionsrev/client/ClientboundPayloadBridge.java`
  - 客户端实际处理 S2C payload 的桥接入口，由 common dispatch 在客户端物理端通过类名字符串反射调用。
- Modify: `src/main/java/com/suian/xaeroregionsrev/network/RegionEditRequestHandler.java`
  - 从 Forge `NetworkEvent.Context` 改到 NeoForge `IPayloadContext`。
- Create: `src/main/java/com/suian/xaeroregionsrev/network/payload/PacketCodecs.java`
  - 包装现有 `encode/decode` 为 NeoForge `StreamCodec`。
- Modify: `src/main/java/com/suian/xaeroregionsrev/network/payload/*.java`
  - 每个 record 实现 `CustomPacketPayload`，添加 `TYPE`、`STREAM_CODEC` 和 `type()`。
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/XaeroRegionsClient.java`
  - 使用传入的 mod event bus 与 NeoForge game event bus。
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/RegionKeyMappings.java`
  - 使用 NeoForge key mapping 包。
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/editor/ClientFavoriteColorStore.java`
  - 使用 NeoForge `FMLPaths`。
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/editor/ColorPickerScreen.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/editor/RegionManagerScreen.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/editor/RegionStyleEditScreen.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java`
  - 将 `RegionNetwork.CHANNEL.sendToServer(...)` 改为 `RegionNetwork.sendToServer(...)`。
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapInputHandler.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayRenderer.java`
  - 使用 NeoForge `ScreenEvent`，删除内部 Forge event bus 注册入口。
- Modify: `src/main/java/com/suian/xaeroregionsrev/data/RegionSavedData.java`
  - 适配 Minecraft 1.21.1 `SavedData` 签名。
- Modify: `src/test/java/com/suian/xaeroregionsrev/network/payload/*.java`
  - 保留现有 FriendlyByteBuf 往返测试，新增 `type()` 和 `STREAM_CODEC` 往返断言。
- Modify: `README.md`
  - 更新项目定位、依赖版本、构建命令、产物名和 Forge 支线说明。
- Modify: `AGENTS.md`
  - 更新技术约定、常用命令和文件树。
- Modify: `CHANGELOG.md`
  - 在 Unreleased 或顶部加入 NeoForge 主线迁移条目。

---

### Task 1: Baseline And Branch Safety

**Files:**
- Inspect only: repository root

- [ ] **Step 1: Verify branch split**

Run:

```powershell
git show-ref --verify refs/heads/forge/1.20.1
git branch --show-current
git status --short
```

Expected:

```text
refs/heads/forge/1.20.1 exists
master
```

`git status --short` must be empty before implementation starts.

If `git status --short` is not empty, stop and ask the user how to handle the existing changes. Do not run `git reset --hard`, `git checkout --`, `git clean`, delete untracked files, stash user work, or include unrelated changes in a migration commit.

- [ ] **Step 2: Record the Forge baseline**

Run:

```powershell
git rev-parse forge/1.20.1
git rev-parse HEAD
```

Expected: `forge/1.20.1` points to the migration-before-implementation commit, while `HEAD` points to the spec/plan commit on `master`.

- [ ] **Step 3: Run current tests as baseline**

Run:

```powershell
.\gradlew.bat test
```

Expected: current Forge baseline tests pass before code migration. If they fail, stop and diagnose because the port would otherwise hide pre-existing failures.

---

### Task 2: Build Script And NeoForge Metadata

**Files:**
- Modify: `settings.gradle`
- Modify: `gradle.properties`
- Modify: `build.gradle`
- Delete: `src/main/resources/META-INF/mods.toml`
- Create: `src/main/resources/META-INF/neoforge.mods.toml`
- Modify: `src/main/resources/pack.mcmeta`

- [ ] **Step 1: Update `settings.gradle` plugin repositories**

Replace the `pluginManagement.repositories` block with:

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = 'NeoForged'
            url = 'https://maven.neoforged.net/releases'
        }
    }
}
```

Keep the existing `org.gradle.toolchains.foojay-resolver-convention` plugin and `rootProject.name`.

- [ ] **Step 2: Update `gradle.properties`**

Replace the environment properties with:

```properties
minecraft_version=1.21.1
artifact_loader=neoforge
neo_version=21.1.235
loader_version_range=[1,)
minecraft_version_range=[1.21.1]
xaero_world_map_runtime_version=1.39.12
xaero_world_map_runtime_file_id=6778114
xaero_world_map_version_range=[1.39.0,)
```

Replace `mod_description` with:

```properties
mod_description=Server-synced polygon regions rendered on Xaero's World Map for NeoForge 1.21.1.
```

Remove `forge_version` and `forge_version_range`.

- [ ] **Step 3: Replace the Gradle plugin and NeoForge run configuration**

Change the `plugins` block to:

```groovy
plugins {
    id 'java-library'
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.neoforged.moddev' version '2.0.141'
}
```

Set:

```groovy
version = "${mod_version}+${artifact_loader}-${minecraft_version}"
group = mod_group_id

base {
    archivesName = archives_base_name
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)
```

Replace the `minecraft { ... }` block with:

```groovy
neoForge {
    version = project.neo_version

    runs {
        client {
            client()
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }

        server {
            server()
            programArgument '--nogui'
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }

        gameTestServer {
            type = 'gameTestServer'
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }

        data {
            data()
            programArguments.addAll '--mod', project.mod_id, '--all',
                    '--output', file('src/generated/resources/').absolutePath,
                    '--existing', file('src/main/resources/').absolutePath
        }

        configureEach {
            systemProperty 'forge.logging.markers', 'REGISTRIES'
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        "${mod_id}" {
            sourceSet sourceSets.main
        }
    }
}
```

- [ ] **Step 4: Configure resources and runtime dependencies**

Use this structure:

```groovy
sourceSets.main.resources {
    srcDir 'src/generated/resources'
}

repositories {
    mavenCentral()
    maven {
        name = 'Modrinth'
        url = 'https://api.modrinth.com/maven'
        content {
            includeGroup 'maven.modrinth'
        }
    }
    maven {
        name = 'CurseMaven'
        url = 'https://cursemaven.com'
        content {
            includeGroup 'curse.maven'
        }
    }
}

dependencies {
    clientRuntimeMods "curse.maven:xaeros-world-map-317780:${xaero_world_map_runtime_file_id}"

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
}
```

Use a resolvable `clientRuntimeMods` configuration plus `syncClientRuntimeMods` so ModDevGradle's client run receives Xaero World Map from an isolated `run-client/mods` directory without publishing or bundling it in the mod jar. This replaced the earlier additional-classpath attempt because the jar appeared on the legacy classpath but did not enter the client Mod List.

For IMBlocker, first search for a NeoForge 1.21.1 runtime artifact. If a compatible artifact exists, add it to `clientRuntimeMods`. If no compatible artifact exists, do not load IMBlocker in the NeoForge development run and record the manual input-method caveat in README during Task 9.

If the CurseMaven artifact does not resolve in Task 2 Step 9, update `xaero_world_map_runtime_file_id` first. If the dependency source changes to Modrinth Maven, update both the `clientRuntimeMods` coordinate and the property naming while keeping the isolated client runtime mod sync.

- [ ] **Step 5: Update resource expansion**

Use:

```groovy
tasks.named('processResources', ProcessResources).configure {
    var replaceProperties = [
            minecraft_version               : minecraft_version,
            minecraft_version_range         : minecraft_version_range,
            neo_version                     : neo_version,
            loader_version_range            : loader_version_range,
            xaero_world_map_version_range   : xaero_world_map_version_range,
            mod_id                          : mod_id,
            mod_name                        : mod_name,
            mod_license                     : mod_license,
            mod_version                     : mod_version,
            mod_authors                     : mod_authors,
            mod_description                 : mod_description
    ]
    inputs.properties replaceProperties

    filesMatching(['META-INF/neoforge.mods.toml', 'pack.mcmeta']) {
        expand replaceProperties + [project: project]
    }
}
```

- [ ] **Step 6: Add archive-name verification**

Add:

```groovy
tasks.register('verifyArchiveName') {
    group = 'verification'
    description = 'Verify that the built jar name includes loader and Minecraft version.'

    dependsOn tasks.named('jar')

    doLast {
        String expected = "${archives_base_name}-${mod_version}+${artifact_loader}-${minecraft_version}.jar"
        File jarFile = tasks.named('jar', Jar).get().archiveFile.get().asFile
        if (jarFile.name != expected) {
            throw new GradleException("Expected jar '${expected}', got '${jarFile.name}'.")
        }
        logger.lifecycle("Verified archive name: ${jarFile.name}")
    }
}

tasks.named('check') {
    dependsOn tasks.named('verifyArchiveName')
}
```

Keep the existing `bump` task unchanged.

- [ ] **Step 7: Preserve test, clean, and manifest behavior**

Keep or recreate these existing behaviors:

```groovy
tasks.named('test', Test) {
    useJUnitPlatform()
}

tasks.named('clean', Delete).configure {
    delete 'bin', 'logs', '.tmp', 'run/logs'
}
```

The `clean` task must not delete `run/saves`, `run/config`, Xaero map caches, or `.gradle`.

Keep the jar manifest attributes currently present in `build.gradle`, but remove ForgeGradle-only behavior:

```text
jar.finalizedBy 'reobfJar'
tasks.matching { it.name == 'runClient' }.configureEach { dependsOn 'createSrgToMcp' }
fg.deobf(...)
mixin.env.refMapRemappingFile using createSrgToMcp
```

- [ ] **Step 8: Create NeoForge metadata**

Delete `src/main/resources/META-INF/mods.toml`.

Create `src/main/resources/META-INF/neoforge.mods.toml`:

```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="${mod_authors}"
description='''${mod_description}'''

[[dependencies.${mod_id}]]
modId="neoforge"
type="required"
versionRange="[${neo_version},)"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="minecraft"
type="required"
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="xaeroworldmap"
type="optional"
versionRange="${xaero_world_map_version_range}"
ordering="AFTER"
side="CLIENT"
```

- [ ] **Step 9: Update `pack.mcmeta`**

Set `pack_format` to 34 for Minecraft 1.21.1:

```json
{
  "pack": {
    "description": "${mod_name} resources",
    "pack_format": 34
  }
}
```

- [ ] **Step 10: Verify Gradle configuration and runtime coordinates**

Run:

```powershell
.\gradlew.bat tasks --all
.\gradlew.bat dependencies --configuration clientRuntimeMods
```

Expected: Gradle configures successfully, lists `runClient`, `runServer`, `runData`, `verifyArchiveName`, `test`, and `build`, and resolves the client runtime dependencies.

- [ ] **Step 11: Commit build metadata migration**

Run:

```powershell
git add settings.gradle gradle.properties build.gradle src/main/resources/META-INF src/main/resources/pack.mcmeta
git commit -m "chore: 切换到 NeoForge 1.21.1 构建" -m "将主线构建从 ForgeGradle 迁移到 ModDevGradle，并设置 Minecraft 1.21.1、NeoForge 21.1.235 与 Java 21。" -m "新增 neoforge.mods.toml 和产物名校验，确保 jar 名包含 neoforge-1.21.1。"
```

---

### Task 3: Network Payload Codec Scaffold

**Files:**
- Create: `src/main/java/com/suian/xaeroregionsrev/network/payload/PacketCodecs.java`

- [ ] **Step 1: Create `PacketCodecs`**

Create:

```java
package com.suian.xaeroregionsrev.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class PacketCodecs {
    private PacketCodecs() {
    }

    public static <B extends FriendlyByteBuf, T> StreamCodec<B, T> of(
            BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder
    ) {
        return new StreamCodec<>() {
            @Override
            public T decode(B buffer) {
                return decoder.apply(buffer);
            }

            @Override
            public void encode(B buffer, T value) {
                encoder.accept(value, buffer);
            }
        };
    }
}
```

- [ ] **Step 2: Do not add packet tests in this task**

This task only creates the reusable codec helper. Add all packet `STREAM_CODEC` tests in Task 4, after the packet classes expose `STREAM_CODEC`, so every task remains internally runnable.

---

### Task 4: Convert Packet Records To NeoForge Custom Payloads

**Files:**
- Modify: all files in `src/main/java/com/suian/xaeroregionsrev/network/payload/`
- Modify: all files in `src/test/java/com/suian/xaeroregionsrev/network/payload/`

- [ ] **Step 1: Apply the payload pattern to each packet**

For every packet record, add these imports:

```java
import com.suian.xaeroregionsrev.XaeroRegionsRev;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
```

Change the record declaration to implement `CustomPacketPayload`.

For `RegionSyncPacket`, use this exact pattern:

```java
public record RegionSyncPacket(List<Region> regions) implements CustomPacketPayload {
    public static final Type<RegionSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "region_sync")
    );
    public static final StreamCodec<FriendlyByteBuf, RegionSyncPacket> STREAM_CODEC =
            PacketCodecs.of(RegionSyncPacket::encode, RegionSyncPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

Preserve the existing canonical constructor, validation constants, `encode`, and `decode` logic.

Use `StreamCodec<FriendlyByteBuf, T>` for every packet. NeoForge `playToClient` and `playToServer` accept `StreamCodec<? super RegistryFriendlyByteBuf, T>`, and `FriendlyByteBuf` is a valid supertype for these packets because they do not read registry-aware values. This keeps existing tests simple and avoids constructing `RegistryFriendlyByteBuf` in unit tests.

- [ ] **Step 2: Use these payload IDs**

```text
RegionSyncPacket -> region_sync
ColorHistorySyncPacket -> color_history_sync
RegionEditResultPacket -> region_edit_result
CreateRegionRequestPacket -> create_region_request
DeleteRegionRequestPacket -> delete_region_request
UpdateRegionStyleRequestPacket -> update_region_style_request
RegionRefreshRequestPacket -> region_refresh_request
ColorHistoryUpdateRequestPacket -> color_history_update_request
```

- [ ] **Step 3: Add type assertions to packet tests**

For each packet test, add one assertion equivalent to:

```java
assertEquals(RegionSyncPacket.TYPE, new RegionSyncPacket(List.of()).type());
```

Use the matching packet constructor in each test. For packets requiring values, use the smallest valid value already present in the test class.

- [ ] **Step 4: Add stream codec round-trip tests**

For each packet test, add a `STREAM_CODEC` round-trip equivalent to:

```java
var buffer = new FriendlyByteBuf(Unpooled.buffer());
RegionSyncPacket.STREAM_CODEC.encode(buffer, packet);
var decoded = RegionSyncPacket.STREAM_CODEC.decode(buffer);
assertEquals(packet, decoded);
```

Use existing valid packet fixtures from each test class.

Do not delete existing packet boundary tests. The migration must preserve tests that reject invalid region counts, single-region point counts, total point counts, over-limit strings, color history limits, and edit result message length limits.

- [ ] **Step 5: Record packet test command for Task 7**

Run:

```powershell
.\gradlew.bat test --tests "com.suian.xaeroregionsrev.network.payload.*"
```

Do not run this command until Task 7 has removed the remaining Forge imports and `compileJava` passes. Running it earlier will compile all main sources and fail on migration work that has not happened yet.

- [ ] **Step 6: Commit packet payload conversion**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev/network/payload src/test/java/com/suian/xaeroregionsrev/network/payload
git commit -m "refactor: 迁移网络包为 NeoForge payload" -m "为所有区域同步与编辑请求包实现 CustomPacketPayload，并保留现有 FriendlyByteBuf 编解码边界。" -m "新增 StreamCodec 往返测试，确保 NeoForge payload 注册可复用既有协议校验。"
```

---

### Task 5: Rewrite Region Network Registration And Send Helpers

**Files:**
- Modify: `src/main/java/com/suian/xaeroregionsrev/network/RegionNetwork.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/network/ClientboundPayloadDispatch.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/client/ClientboundPayloadBridge.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/network/RegionEditRequestHandler.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/editor/ColorPickerScreen.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/editor/RegionManagerScreen.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/editor/RegionStyleEditScreen.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java`

- [ ] **Step 1: Replace `RegionNetwork` channel code**

Remove `SimpleChannel`, `NetworkRegistry`, `NetworkDirection`, and `PacketDistributor.PLAYER.with`.

Use these imports:

```java
import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.network.payload.ColorHistorySyncPacket;
import com.suian.xaeroregionsrev.network.payload.ColorHistoryUpdateRequestPacket;
import com.suian.xaeroregionsrev.network.payload.CreateRegionRequestPacket;
import com.suian.xaeroregionsrev.network.payload.DeleteRegionRequestPacket;
import com.suian.xaeroregionsrev.network.payload.RegionEditResultPacket;
import com.suian.xaeroregionsrev.network.payload.RegionRefreshRequestPacket;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import com.suian.xaeroregionsrev.network.payload.UpdateRegionStyleRequestPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
```

Implement this registration shape:

```java
public static void register(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

    registrar.playToClient(RegionSyncPacket.TYPE, RegionSyncPacket.STREAM_CODEC,
            ClientboundPayloadDispatch::handleRegionSync);
    registrar.playToClient(ColorHistorySyncPacket.TYPE, ColorHistorySyncPacket.STREAM_CODEC,
            ClientboundPayloadDispatch::handleColorHistorySync);
    registrar.playToClient(RegionEditResultPacket.TYPE, RegionEditResultPacket.STREAM_CODEC,
            ClientboundPayloadDispatch::handleRegionEditResult);

    registrar.playToServer(CreateRegionRequestPacket.TYPE, CreateRegionRequestPacket.STREAM_CODEC,
            RegionEditRequestHandler::handleCreate);
    registrar.playToServer(DeleteRegionRequestPacket.TYPE, DeleteRegionRequestPacket.STREAM_CODEC,
            RegionEditRequestHandler::handleDelete);
    registrar.playToServer(UpdateRegionStyleRequestPacket.TYPE, UpdateRegionStyleRequestPacket.STREAM_CODEC,
            RegionEditRequestHandler::handleUpdateStyle);
    registrar.playToServer(RegionRefreshRequestPacket.TYPE, RegionRefreshRequestPacket.STREAM_CODEC,
            RegionEditRequestHandler::handleRefresh);
    registrar.playToServer(ColorHistoryUpdateRequestPacket.TYPE, ColorHistoryUpdateRequestPacket.STREAM_CODEC,
            RegionEditRequestHandler::handleRememberColor);
}
```

Keep `PROTOCOL_VERSION = "6"` unless a compile-time NeoForge negotiation error requires a bump.

Do not import `com.suian.xaeroregionsrev.client.*` in `RegionNetwork`. Dedicated servers must be able to load `RegionNetwork` without touching classes that import `net.minecraft.client.*`.

- [ ] **Step 2: Create clientbound payload dispatch and client bridge**

Create `src/main/java/com/suian/xaeroregionsrev/client/ClientboundPayloadBridge.java`:

```java
package com.suian.xaeroregionsrev.client;

import com.suian.xaeroregionsrev.network.payload.ColorHistorySyncPacket;
import com.suian.xaeroregionsrev.network.payload.RegionEditResultPacket;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;

public final class ClientboundPayloadBridge {
    private ClientboundPayloadBridge() {
    }

    public static void handleRegionSync(RegionSyncPacket packet) {
        ClientRegionCache.replaceAll(packet.regions());
    }

    public static void handleColorHistorySync(ColorHistorySyncPacket packet) {
        ClientColorHistoryCache.replaceAll(packet.colors());
    }

    public static void handleRegionEditResult(RegionEditResultPacket packet) {
        ClientRegionEditResultHandler.handle(packet);
    }
}
```

Create `src/main/java/com/suian/xaeroregionsrev/network/ClientboundPayloadDispatch.java` as a common-side dispatcher. It should enqueue work, check `FMLEnvironment.dist == Dist.CLIENT`, then call `ClientboundPayloadBridge` by class name string reflection. Do not import client classes in the common dispatcher; dedicated servers load the same common networking classes.

- [ ] **Step 3: Add send helpers**

Use:

```java
public static void sendToPlayer(ServerPlayer player, RegionSyncPacket packet) {
    LOGGER.info("Sending {} region(s) to player {}.", packet.regions().size(), player.getGameProfile().getName());
    PacketDistributor.sendToPlayer(player, packet);
}

public static void sendEditResultToPlayer(ServerPlayer player, RegionEditResultPacket packet) {
    PacketDistributor.sendToPlayer(player, packet);
}

public static void sendToAll(RegionSyncPacket packet) {
    LOGGER.info("Broadcasting {} region(s) to all players.", packet.regions().size());
    PacketDistributor.sendToAllPlayers(packet);
}

public static void sendColorHistoryToPlayer(ServerPlayer player, ColorHistorySyncPacket packet) {
    LOGGER.info("Sending {} color history item(s) to player {}.",
            packet.colors().size(), player.getGameProfile().getName());
    PacketDistributor.sendToPlayer(player, packet);
}

public static void sendColorHistoryToAll(ColorHistorySyncPacket packet) {
    LOGGER.info("Broadcasting {} color history item(s) to all players.", packet.colors().size());
    PacketDistributor.sendToAllPlayers(packet);
}

public static void sendToServer(CustomPacketPayload packet) {
    PacketDistributor.sendToServer(packet);
}
```

- [ ] **Step 4: Rewrite `RegionEditRequestHandler` method signatures**

Replace every `Supplier<NetworkEvent.Context>` signature with:

```java
public static void handleCreate(CreateRegionRequestPacket packet, IPayloadContext context)
```

Use imports:

```java
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
```

Add:

```java
private static ServerPlayer serverPlayer(IPayloadContext context) {
    Player player = context.player();
    return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
}
```

Inside each handler, replace:

```java
NetworkEvent.Context context = contextSupplier.get();
context.enqueueWork(() -> {
    ServerPlayer sender = context.getSender();
    ...
});
context.setPacketHandled(true);
```

with:

```java
context.enqueueWork(() -> {
    ServerPlayer sender = serverPlayer(context);
    ...
});
```

Do not call `setPacketHandled`; NeoForge payload handlers do not use it.

Preserve the existing security and validation behavior:

- `handleCreate`, `handleDelete`, `handleUpdateStyle`, and `handleRememberColor` must continue to require `sender != null && canManage(sender)`.
- `handleCreate` and `handleUpdateStyle` must continue to call `RegionRequestValidator` before writing data.
- `handleRefresh` remains available to any non-null sender, but must keep the existing 2 second per-player cooldown.
- All successful writes must continue to broadcast fresh snapshots or color history as they do today.
- All create/update failures must continue to send `RegionEditResultPacket` with the original `requestId`.

- [ ] **Step 5: Replace all client send call sites**

Change every:

```java
RegionNetwork.CHANNEL.sendToServer(new SomePacket(...));
```

to:

```java
RegionNetwork.sendToServer(new SomePacket(...));
```

Affected files:

```text
src/main/java/com/suian/xaeroregionsrev/client/editor/ColorPickerScreen.java
src/main/java/com/suian/xaeroregionsrev/client/editor/RegionManagerScreen.java
src/main/java/com/suian/xaeroregionsrev/client/editor/RegionStyleEditScreen.java
src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java
```

- [ ] **Step 6: Run network text checks**

Run:

```powershell
rg -n "RegionNetwork\.CHANNEL|SimpleChannel|NetworkRegistry|NetworkEvent|import com\.suian\.xaeroregionsrev\.client" src/main/java/com/suian/xaeroregionsrev/network
```

Expected: no `RegionNetwork.CHANNEL`, `SimpleChannel`, `NetworkRegistry`, Forge `NetworkEvent`, or direct client imports remain in common networking classes. `ClientboundPayloadDispatch` may contain only a client bridge class name string for reflective dispatch on client physical side.

- [ ] **Step 7: Commit network registration migration**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev/network src/main/java/com/suian/xaeroregionsrev/client
git commit -m "refactor: 使用 NeoForge payload 网络注册" -m "将 SimpleChannel 注册和发送迁移到 RegisterPayloadHandlersEvent、PayloadRegistrar 与 PacketDistributor。" -m "保留服务端主线程处理、权限校验和客户端缓存更新语义，并提供统一 sendToServer 入口。"
```

---

### Task 6: Loader Event And Client API Migration

**Files:**
- Modify: `src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/XaeroRegionsClient.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/RegionKeyMappings.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/editor/ClientFavoriteColorStore.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapInputHandler.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayRenderer.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/command/RegionCommands.java`
- Move: `src/main/java/com/suian/xaeroregionsrev/platform/ForgePermissionAdapter.java` -> `src/main/java/com/suian/xaeroregionsrev/platform/MinecraftPermissionAdapter.java`

- [ ] **Step 1: Rewrite mod entrypoint**

Update `XaeroRegionsRev` imports:

```java
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
```

Use constructor:

```java
public XaeroRegionsRev(IEventBus modEventBus) {
    modEventBus.addListener(RegionNetwork::register);
    if (FMLEnvironment.dist == Dist.CLIENT) {
        registerClient(modEventBus);
    }
    NeoForge.EVENT_BUS.register(this);
    LOGGER.info("Xaero Map Regions Rev loaded.");
}

private static void registerClient(IEventBus modEventBus) {
    try {
        Class<?> clientClass = Class.forName("com.suian.xaeroregionsrev.client.XaeroRegionsClient");
        clientClass.getMethod("register", IEventBus.class).invoke(null, modEventBus);
    } catch (ReflectiveOperationException exception) {
        throw new IllegalStateException("Failed to initialize Xaero Map Regions Rev client hooks.", exception);
    }
}
```

Remove `MinecraftForge`. Do not directly import or reference client-only classes from the common `@Mod` constructor; dedicated servers load the same entrypoint.

- [ ] **Step 2: Rewrite client entrypoint**

Change `XaeroRegionsClient.register()` to accept the mod event bus:

```java
public static void register(IEventBus modEventBus) {
    if (registered) {
        return;
    }
    registered = true;
    modEventBus.addListener(RegionKeyMappings::register);
    NeoForge.EVENT_BUS.addListener(XaeroMapOverlayRenderer::onScreenRenderPost);
    NeoForge.EVENT_BUS.addListener(XaeroMapInputHandler::onKeyPressed);
    NeoForge.EVENT_BUS.addListener(XaeroMapInputHandler::onMouseButtonPressed);
    NeoForge.EVENT_BUS.addListener(XaeroRegionsClient::onScreenOpening);
    NeoForge.EVENT_BUS.addListener(XaeroRegionsClient::onClientLoggingOut);
}
```

Use imports:

```java
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
```

Remove `FMLJavaModLoadingContext`.

- [ ] **Step 3: Rename permission adapter**

Move `ForgePermissionAdapter.java` to `MinecraftPermissionAdapter.java` and replace class content:

```java
package com.suian.xaeroregionsrev.platform;

import com.suian.xaeroregionsrev.region.PermissionProfile;
import net.minecraft.server.level.ServerPlayer;

public final class MinecraftPermissionAdapter {
    private MinecraftPermissionAdapter() {
    }

    public static PermissionProfile from(ServerPlayer player) {
        boolean operator = player.hasPermissions(2);
        boolean creative = player.gameMode.isCreative();
        return new PermissionProfile(operator, creative);
    }
}
```

Update `RegionCommands` and `RegionEditRequestHandler` imports and calls from `ForgePermissionAdapter` to `MinecraftPermissionAdapter`.

- [ ] **Step 4: Update client event imports**

Replace imports:

```text
net.minecraftforge.client.event.ScreenEvent -> net.neoforged.neoforge.client.event.ScreenEvent
net.minecraftforge.client.event.RegisterKeyMappingsEvent -> net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
net.minecraftforge.client.event.ClientPlayerNetworkEvent -> net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
net.minecraftforge.common.MinecraftForge -> net.neoforged.neoforge.common.NeoForge
net.minecraftforge.fml.loading.FMLPaths -> net.neoforged.fml.loading.FMLPaths
net.minecraftforge.common.util.Lazy -> net.neoforged.neoforge.common.util.Lazy
```

In `XaeroMapOverlayRenderer`, delete the `register()` method because registration now happens in `XaeroRegionsClient`.

- [ ] **Step 5: Update key mapping imports**

Replace:

```java
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
```

with the NeoForge equivalents:

```java
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
```

If `IKeyConflictContext` package has changed in NeoForge 21.1.235, use compiler error output to pick the NeoForge package, but keep the existing active/conflict behavior exactly the same.

- [ ] **Step 6: Update command event import**

In `RegionCommands`, replace:

```java
import net.minecraftforge.event.RegisterCommandsEvent;
```

with:

```java
import net.neoforged.neoforge.event.RegisterCommandsEvent;
```

- [ ] **Step 7: Run loader text checks**

Run:

```powershell
rg "net\.minecraftforge|MinecraftForge|ForgePermissionAdapter|SimpleChannel|NetworkEvent" src/main/java
```

Expected: no output.

Record the key/input state test command for Task 7:

```powershell
.\gradlew.bat test --tests "com.suian.xaeroregionsrev.client.xaero.XaeroMapInputHandlerTest" --tests "com.suian.xaeroregionsrev.client.xaero.XaeroMapInputRouterTest"
```

Do not run the command until Task 7 has closed compile errors. Expected after Task 7: non-Xaero screens are ignored, focused text inputs are not intercepted, and edit-mode-only actions remain ignored outside edit mode.

- [ ] **Step 8: Commit loader API migration**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev
git commit -m "refactor: 迁移入口和客户端事件到 NeoForge" -m "将 mod 入口、命令注册、客户端事件、快捷键和配置路径迁移到 NeoForge API。" -m "重命名权限适配器，避免 NeoForge 主线继续保留 Forge 命名。"
```

---

### Task 7: Minecraft 1.21.1 SavedData And Compile Repairs

**Files:**
- Modify: `src/main/java/com/suian/xaeroregionsrev/data/RegionSavedData.java`
- Modify: any file reported by `compileJava` due to Minecraft 1.21.1 API signature changes

- [ ] **Step 1: Update `RegionSavedData` signatures**

Add import:

```java
import net.minecraft.core.HolderLookup;
```

Change `get` to:

```java
public static RegionSavedData get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(RegionSavedData::new, RegionSavedData::load),
            DATA_NAME
    );
}
```

Change `load` to:

```java
public static RegionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
    RegionSavedData data = new RegionSavedData();
    ...
}
```

Change `save` to:

```java
@Override
public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ...
}
```

Keep the inner load/save body unchanged.

- [ ] **Step 2: Run compile and fix only explicit 1.21.1 API errors**

Run:

```powershell
.\gradlew.bat compileJava
```

Allowed repairs in this step:

- Replace removed `new ResourceLocation(namespace, path)` calls with `ResourceLocation.fromNamespaceAndPath(namespace, path)`.
- Adjust method signatures where Minecraft 1.21.1 adds `HolderLookup.Provider`.
- Adjust NeoForge package names when the compiler reports the exact replacement.

Do not refactor rendering, editor state, region model, or command behavior in this task.

- [ ] **Step 3: Add Forge 1.20.1 NBT compatibility fixture**

Add a test to `src/test/java/com/suian/xaeroregionsrev/data/RegionSavedDataTest.java` that builds the same NBT shape used by the Forge 1.20.1 branch:

```java
@Test
void loadsForge1201StyleRegionAndColorHistory() {
    CompoundTag root = new CompoundTag();
    ListTag regions = new ListTag();
    CompoundTag region = new CompoundTag();
    region.putString("id", "spawn");
    region.putString("name", "Spawn");
    region.putString("dimension", "minecraft:overworld");
    region.putInt("color", 0x8800FF00);
    region.putString("label", "Spawn Label");
    region.putInt("labelColor", 0xFFFFFFFF);
    region.putString("category", "default");
    region.putString("iconName", "default");
    region.putLong("createdAt", 100L);
    region.putLong("updatedAt", 200L);
    ListTag points = new ListTag();
    points.add(pointTag(0, 0));
    points.add(pointTag(16, 0));
    points.add(pointTag(16, 16));
    region.put("points", points);
    regions.add(region);
    root.put("regions", regions);
    ListTag colors = new ListTag();
    colors.add(IntTag.valueOf(0x8800FF00));
    root.put("colorHistory", colors);

    RegionSavedData data = RegionSavedData.load(root, HolderLookup.Provider.create(Stream.empty()));

    assertEquals(1, data.allRegions().size());
    assertEquals(List.of(new ArgbColor(0x8800FF00)), data.colorHistory());
}
```

If `HolderLookup.Provider.create(Stream.empty())` is not available in 1.21.1 tests, use the compiler-provided empty provider factory. Keep the fixture field names and values unchanged.

- [ ] **Step 4: Run data tests**

Run:

```powershell
.\gradlew.bat test --tests "com.suian.xaeroregionsrev.data.*" --tests "com.suian.xaeroregionsrev.region.*"
.\gradlew.bat test --tests "com.suian.xaeroregionsrev.network.payload.*"
.\gradlew.bat test --tests "com.suian.xaeroregionsrev.client.editor.RegionSubmissionStateTest" --tests "com.suian.xaeroregionsrev.network.payload.RegionEditResultPacketTest"
.\gradlew.bat test --tests "com.suian.xaeroregionsrev.client.xaero.XaeroMapInputHandlerTest" --tests "com.suian.xaeroregionsrev.client.xaero.XaeroMapInputRouterTest"
```

Expected: data, region, packet, edit-result, and key/input tests pass, proving NBT fields remain compatible, request/response packet behavior still preserves `requestId`, success, close-screen, and failure-message semantics, and key handling remains scoped to Xaero/editor contexts.

- [ ] **Step 5: Commit SavedData and compile repairs**

Run:

```powershell
git add src/main/java src/test/java
git commit -m "fix: 适配 Minecraft 1.21.1 数据 API" -m "更新 SavedData 读取和保存签名，并处理编译暴露的 1.21.1 API 差异。" -m "保持区域 NBT 字段不变，避免 loader 迁移破坏现有存档数据。"
```

---

### Task 8: Full Test, Build, And Runtime Dependency Resolution

**Files:**
- Modify: `build.gradle` only if runtime dependency coordinates fail to resolve
- Modify: `gradle.properties` only if `xaero_world_map_runtime_version` must change to a resolving NeoForge 1.21.1 artifact

- [ ] **Step 1: Run full build**

Run:

```powershell
.\gradlew.bat clean test build
```

Expected:

- `BUILD SUCCESSFUL`
- `verifyArchiveName` passes.
- `build/libs/Xaero-Map-Regions-Rev-<mod_version>+neoforge-1.21.1.jar` exists.

- [ ] **Step 2: If Xaero runtime dependency fails, resolve coordinates**

Run:

```powershell
.\gradlew.bat dependencies --configuration clientRuntimeMods
```

If `curse.maven:xaeros-world-map-317780:${xaero_world_map_runtime_file_id}` does not resolve, update `xaero_world_map_runtime_file_id` to a compatible NeoForge 1.21.1 CurseForge file. If moving to another Maven source, update both the dependency notation and the runtime dependency properties. Re-run:

```powershell
.\gradlew.bat dependencies --configuration clientRuntimeMods
.\gradlew.bat build
```

Expected: dependency resolution succeeds without bundling Xaero into the jar.

- [ ] **Step 3: Run dedicated server smoke test**

Run:

```powershell
.\gradlew.bat runServer
```

Manual acceptance:

- Dedicated server starts without loading `net.minecraft.client.*` classes.
- The mod registers commands and network payloads.
- Stop the server after startup is confirmed.

- [ ] **Step 4: Run dev client smoke test**

Run:

```powershell
.\gradlew.bat runClient
```

Manual acceptance:

- NeoForge 1.21.1 client reaches main menu.
- Development run loads this mod.
- If Xaero World Map resolves, load or create a world with at least one region and open the Xaero map.
- Existing regions show fill, border, and label text.
- Zooming or dragging the map does not obviously detach region fill, border, or label from the region.
- Regions from another dimension do not display in the current dimension.
- Edit mode shows draft points/lines/fill and selected-region decorations.
- Create success closes the edit screen through `RegionEditResultPacket`; permission or validation failure keeps the edit screen open and shows the failure message.
- Login sync sends both region snapshot and shared color history; refresh sync and remember-color broadcast still update clients.
- Logging out and joining another world clears region cache, color history cache, and overlay/editor session state.
- If IMBlocker is unavailable for NeoForge 1.21.1, note it in README as a development-smoke-test caveat.

- [ ] **Step 5: Verify jar contents do not bundle runtime mods**

Run:

```powershell
jar tf build/libs/Xaero-Map-Regions-Rev-${mod_version}+neoforge-1.21.1.jar | rg "xaero|imblocker|META-INF/jarjar"
```

Expected: no Xaero World Map or IMBlocker classes are bundled. If `META-INF/jarjar` appears, inspect it and remove unintended runtime mod bundling.

- [ ] **Step 6: Commit verification fixes**

Run:

```powershell
git add build.gradle gradle.properties
git commit -m "test: 验证 NeoForge 构建与运行期依赖" -m "运行完整测试和构建，确认产物名包含 neoforge-1.21.1。" -m "如有必要，修正 Xaero World Map 开发运行期依赖坐标。"
```

Skip the commit if no files changed in this task.

---

### Task 9: Documentation And Project Rules

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Update README project summary**

Replace the first paragraph with:

```markdown
Xaero Map Regions Rev 是一个面向 Minecraft 1.21.1 NeoForge 的区域标注模组，用于在 Xaero's World Map 页面上显示由服务端同步的多边形区域、半透明填充和标签。
```

Add a maintenance note after the MVP paragraph:

```markdown
主力维护线为 NeoForge 1.21.1。Forge 1.20.1 版本保留在 `forge/1.20.1` 分支，用于历史版本维护。
```

- [ ] **Step 2: Update README version table**

Use:

```markdown
| 项目 | 版本 |
|------|------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.235 或更高 21.1.x |
| Java | 21 |
| 模组版本 | 见 `gradle.properties` 的 `mod_version` |
| Xaero's World Map | 可选客户端依赖，见 `gradle.properties` 的 `xaero_world_map_version_range` |
```

- [ ] **Step 3: Update README artifact paths**

Replace both Forge artifact examples with:

```text
build/libs/Xaero-Map-Regions-Rev-<mod_version>+neoforge-1.21.1.jar
```

- [ ] **Step 4: Update AGENTS technical conventions**

Change:

```markdown
- 目标版本：Minecraft 1.20.1 + Forge 47.3.33。
```

to:

```markdown
- 主力目标版本：Minecraft 1.21.1 + NeoForge 21.1.235。
- Forge 1.20.1 历史维护线保留在 `forge/1.20.1` 分支。
```

Update the build command text to mention ModDevGradle and NeoForge.

- [ ] **Step 5: Update AGENTS file tree**

Change build and platform descriptions:

```text
├── build.gradle
│   ModDevGradle 构建脚本、NeoForge 运行配置、客户端运行期模组依赖与测试依赖声明。
...
│   ├── platform/
│   │   Minecraft 权限适配。
...
├── src/main/resources/
│   NeoForge 模组元数据、资源包描述、中英文客户端文本资源与地图编辑器 UI 贴图资源。
```

Keep existing specs/plans entries and add:

```text
│       │   └── 2026-07-04-neoforge-1211-port-implementation.md
│       │       NeoForge 1.21.1 主线迁移实施计划。
│           └── 2026-07-04-neoforge-1211-port-design.md
│               NeoForge 1.21.1 主线迁移设计文档。
```

- [ ] **Step 6: Update CHANGELOG**

Add a top unreleased section if none exists:

```markdown
## [Unreleased]

本阶段将主力维护线迁移到 Minecraft 1.21.1 + NeoForge，并保留 Forge 1.20.1 历史分支。

### 其他改进

- 迁移主线构建到 NeoForge 1.21.1，并让构建产物名包含 loader 与 Minecraft 版本。
- 保留 `forge/1.20.1` 分支作为 Forge 历史维护线。
```

- [ ] **Step 7: Verify docs mention no stale Forge mainline wording**

Run:

```powershell
rg -n "1\.20\.1 Forge|ForgeGradle|forge-1\.20\.1|net\.minecraftforge" README.md AGENTS.md CHANGELOG.md build.gradle gradle.properties src/main/resources src/main/java
rg --files | Sort-Object
Get-Content CLAUDE.md
```

Expected: matches are either in historical branch notes or absent from mainline code/config.

Use the `rg --files` output to confirm the AGENTS file tree reflects `META-INF/neoforge.mods.toml`, removed `META-INF/mods.toml`, renamed `MinecraftPermissionAdapter`, and the new NeoForge spec/plan files. `CLAUDE.md` must still only import `@AGENTS.md` plus Claude-specific additions.

- [ ] **Step 8: Commit docs**

Run:

```powershell
git add README.md AGENTS.md CHANGELOG.md
git commit -m "docs: 更新 NeoForge 主线维护说明" -m "更新 README、AGENTS 和 CHANGELOG，说明 NeoForge 1.21.1 为主力维护线，Forge 1.20.1 保留在历史分支。" -m "同步文件树、构建命令和产物命名说明，避免文档继续指向 Forge 主线。"
```

Only stage `docs/superpowers/plans/2026-07-04-neoforge-1211-port-implementation.md` in this task if the implementation process intentionally updates checklist state or review notes.

---

### Task 10: Final Verification And Review Handoff

**Files:**
- Inspect only: entire repository

- [ ] **Step 1: Run final static checks**

Run:

```powershell
rg -n "net\.minecraftforge|MinecraftForge|ForgePermissionAdapter|SimpleChannel|NetworkRegistry|NetworkEvent" src/main/java build.gradle gradle.properties src/main/resources
git diff --check
```

Expected: no stale Forge implementation references and no whitespace errors.

- [ ] **Step 2: Run final test/build**

Run:

```powershell
.\gradlew.bat build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Confirm final jar name**

Run:

```powershell
Get-ChildItem build/libs | Select-Object -ExpandProperty Name
```

Expected output includes:

```text
Xaero-Map-Regions-Rev-<mod_version>+neoforge-1.21.1.jar
```

Expected output does not include a newly built `+forge-1.20.1.jar` on `master`.

- [ ] **Step 4: Review git history**

Run:

```powershell
git log --oneline --decorate -8
git status --short
```

Expected: migration commits are small and ordered; status is clean.

- [ ] **Step 5: Request implementation code review**

After implementation is complete, dispatch reviewer agents with these scopes:

```text
Reviewer 1: NeoForge API correctness and build metadata.
Reviewer 2: Network payload safety and packet validation.
Reviewer 3: Runtime behavior and regression risk for Xaero overlay/editor workflows.
Reviewer 4: Documentation, release hygiene, and branch strategy.
```

Block completion on Critical or Important findings.

Do not push, create a PR, create a release, or merge before the user reviews the final implementation and explicitly approves the result.

---

## Plan Review Requirement

Before executing this implementation plan, dispatch read-only review agents to review this plan document:

- Correctness: verify NeoForge 1.21.1 API usage, task order, and file coverage.
- Safety: verify network limits, permission checks, branch safety, and no destructive Git operations.
- Efficiency: verify task decomposition, commit granularity, and minimal churn.
- Regression: verify coverage for data compatibility, client editor flow, Xaero overlay, and docs.

The plan may be executed only after those review findings are addressed or explicitly rejected with technical reasoning, the revised plan is committed, and the user gives explicit approval to start implementation.
