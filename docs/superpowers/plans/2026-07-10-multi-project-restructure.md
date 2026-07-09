# 多子项目重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将双分支（master NeoForge 1.21.1 + forge/1.20.1）合并为单分支多子项目结构，抽离公共逻辑到 common 模块。

**Architecture:** Gradle 多子项目：`common`（纯 Java 公共逻辑）+ `neoforge/mc-1.21.1`（NeoForge 适配）+ `forge/mc-1.20.1`（Forge 适配）。平台子项目通过 `mods {}` sourceSet 纳入 common 源码。MC API 依赖通过抽象接口（PacketBuffer、NbtCompound、ServerContext、RegionStore、EditResultHandler）隔离。

**Tech Stack:** Gradle 8.8、Java 17（common/forge）/ Java 21（neoforge）、ModDevGradle 2.0.141、ForgeGradle [6.0.16,6.2)、JUnit 5、JOML 1.10.5

**Spec:** `docs/superpowers/specs/2026-07-10-multi-project-restructure-design.md`

## Global Constraints

- common 模块用 Java 17 编译（两端最低值），不得使用 Java 18+ 语法
- common 不得出现任何 `net.minecraft` / `net.minecraftforge` / `net.neoforged` import
- NeoForge 子项目 Java 21，Forge 子项目 Java 17
- Gradle Wrapper 版本保持 8.8 不变
- 抽象接口只暴露业务逻辑实际使用的方法子集，不做全量镜像
- 资源 assets 放 common，META-INF toml 和 pack.mcmeta 各平台独立
- 每个阶段结束创建 Git commit，前一阶段验证通过才进下一阶段
- 提交信息使用中文，格式 `类型: 简述`

---

## 阶段 0：分支准备

### Task 0: 创建重构分支

**Files:**
- 无文件变更

- [ ] **Step 1: 确认工作树干净**

Run: `git status --short`
Expected: 空输出（无未提交变更）

- [ ] **Step 2: 创建并切换到重构分支**

Run: `git checkout -b refactor/multi-project-structure`
Expected: `Switched to a new branch 'refactor/multi-project-structure'`

- [ ] **Step 3: 确认当前 HEAD**

Run: `git log --oneline -1`
Expected: 显示最新 master commit（设计文档提交）

- [ ] **Step 4: Commit（标记起点）**

此步骤无代码变更，跳过 commit。分支创建即为完成标志。

---

## 阶段 1：Gradle 骨架 + common 可编译

**目标**：多项目结构成形，common 的纯 Java 代码和测试通过 `./gradlew :common:test`。

### Task 1: 创建 Gradle 多项目骨架

**Files:**
- Modify: `settings.gradle`
- Modify: `gradle.properties`
- Create: `gradle/region-repositories.gradle`
- Create: `gradle/region-platform.gradle`
- Create: `common/build.gradle`
- Create: `neoforge/mc-1.21.1/.gitkeep`（占位，阶段 2 填充）
- Create: `forge/mc-1.20.1/.gitkeep`（占位，阶段 3 填充）

**Interfaces:**
- Produces: Gradle 多项目结构，`./gradlew projects` 列出 common + neoforge:mc-1.21.1 + forge:mc-1.20.1

- [ ] **Step 1: 重写 settings.gradle**

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = 'NeoForged'
            url = 'https://maven.neoforged.net/releases'
        }
        maven {
            name = 'MinecraftForge'
            url = 'https://maven.minecraftforge.net/'
        }
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

- [ ] **Step 2: 重写根 gradle.properties（只保留全局共享属性）**

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

- [ ] **Step 3: （不再需要 buildSrc，共享脚本直接放 gradle/ 目录）**

跳过此步骤。共享 Gradle 脚本放在 `gradle/` 目录，通过 `apply from: "$rootDir/gradle/xxx.gradle"` 引用，避免 buildSrc 预编译脚本插件机制的复杂性。

- [ ] **Step 4: 创建 region-repositories.gradle**

```groovy
// gradle/region-repositories.gradle
// 所有子项目共享的仓库声明
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
```

- [ ] **Step 5: 创建 region-platform.gradle**

```groovy
// gradle/region-platform.gradle
// 平台子项目共享逻辑：jar manifest 模板
// bump 任务不在此处注册（在根 build.gradle 统一注册，避免子项目并行冲突）

apply from: "$rootDir/gradle/region-repositories.gradle"

tasks.named('jar', Jar).configure {
    manifest {
        attributes([
                'Specification-Title'     : mod_id,
                'Specification-Vendor'    : mod_authors,
                'Specification-Version'   : '1',
                'Implementation-Title'    : project.name,
                'Implementation-Version'  : project.jar.archiveVersion,
                'Implementation-Vendor'   : mod_authors,
                'Implementation-Timestamp': new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")
        ])
    }
}
```

- [ ] **Step 6: 创建 common/build.gradle**

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

tasks.named('test', Test) {
    useJUnitPlatform()
}
```

- [ ] **Step 7: 创建占位文件让 Gradle 识别子项目目录**

创建 `neoforge/mc-1.21.1/.gitkeep` 和 `forge/mc-1.20.1/.gitkeep`（空文件）。

同时创建最小化的占位 build.gradle 让 Gradle 不会因缺少构建脚本报错：

`neoforge/mc-1.21.1/build.gradle`:
```groovy
// 占位，阶段 2 填充
```

`forge/mc-1.20.1/build.gradle`:
```groovy
// 占位，阶段 3 填充
```

- [ ] **Step 8: 修改根 build.gradle（移除旧的单项目配置，注册 bump 任务）**

将根 `build.gradle` 替换为：

```groovy
// 根项目不 apply 任何 MC 插件
// 各子项目的构建配置在各自的 build.gradle 中

tasks.register('buildAll') {
    group = 'build'
    description = 'Build all platform subprojects.'
    dependsOn ':neoforge:mc-1.21.1:build', ':forge:mc-1.20.1:build'
}

// bump 任务只在根项目注册，避免子项目并行执行冲突
tasks.register('bump') {
    group = 'versioning'
    description = 'Bump mod_version in root gradle.properties. Usage: bump "-PVERSION=0.1.1", bump -PBUMP=patch, or bump -Ppatch/-Pminor/-Pmajor.'

    doLast {
        File propertiesFile = file('gradle.properties')
        List<String> lines = propertiesFile.readLines('UTF-8')
        String currentVersion = lines.find { it.startsWith('mod_version=') }?.substring('mod_version='.length())
        if (currentVersion == null || currentVersion.isBlank()) {
            throw new GradleException('Cannot find mod_version in gradle.properties.')
        }

        List<String> flagModes = ['major', 'minor', 'patch'].findAll { project.hasProperty(it) }
        String bumpMode = project.findProperty('BUMP') as String
        if (bumpMode != null && !['major', 'minor', 'patch'].contains(bumpMode)) {
            throw new GradleException("Unsupported bump mode '${bumpMode}'. Use patch, minor, or major.")
        }
        if (bumpMode != null) {
            flagModes.add(bumpMode)
        }
        flagModes = flagModes.unique()

        String explicitVersion = project.findProperty('VERSION') as String
        if (explicitVersion != null && !flagModes.isEmpty()) {
            throw new GradleException('Use either -PVERSION=<version> or one of -Ppatch/-Pminor/-Pmajor, not both.')
        }
        if (explicitVersion == null && flagModes.size() != 1) {
            throw new GradleException('Specify exactly one version target: -PVERSION=<version>, -Ppatch, -Pminor, or -Pmajor.')
        }

        String nextVersion = explicitVersion ?: bumpedVersion(currentVersion, flagModes.first())
        if (!(nextVersion ==~ /\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?/)) {
            throw new GradleException("Invalid VERSION '${nextVersion}'. Expected semantic version like 0.1.1.")
        }

        List<String> updatedLines = lines.collect { line ->
            line.startsWith('mod_version=') ? "mod_version=${nextVersion}" : line
        }
        propertiesFile.write(updatedLines.join(System.lineSeparator()) + System.lineSeparator(), 'UTF-8')
        logger.lifecycle("Bumped mod_version: ${currentVersion} -> ${nextVersion}")
    }
}

static String bumpedVersion(String currentVersion, String bumpMode) {
    def matcher = currentVersion =~ /^(\d+)\.(\d+)\.(\d+)(?:-[0-9A-Za-z.-]+)?$/
    if (!matcher.matches()) {
        throw new GradleException("Cannot ${bumpMode}-bump non-semver mod_version '${currentVersion}'.")
    }
    int major = matcher.group(1) as int
    int minor = matcher.group(2) as int
    int patch = matcher.group(3) as int
    switch (bumpMode) {
        case 'major':
            return "${major + 1}.0.0"
        case 'minor':
            return "${major}.${minor + 1}.0"
        case 'patch':
            return "${major}.${minor}.${patch + 1}"
        default:
            throw new GradleException("Unsupported bump mode '${bumpMode}'.")
    }
}
```

- [ ] **Step 9: 验证 Gradle 多项目结构**

Run: `./gradlew projects`
Expected: 列出 `Root project` + `Project ':common'` + `Project ':neoforge:mc-1.21.1'` + `Project ':forge:mc-1.20.1'`

- [ ] **Step 10: Commit**

```bash
git add settings.gradle gradle.properties build.gradle gradle/region-*.gradle common/build.gradle neoforge/ forge/
git commit -m "refactor: 搭建 Gradle 多子项目骨架

- 重写 settings.gradle，include common + neoforge:mc-1.21.1 + forge:mc-1.20.1
- 拆分 gradle.properties 为根级全局属性（mod 元数据 + joml）
- 创建 gradle/ 共享脚本（region-repositories + region-platform）
- 根 build.gradle 注册 bump 任务（避免子项目并行冲突）
- 创建 common/build.gradle（java-library，Java 17，JOML 依赖）
- 平台子项目创建占位 build.gradle，后续阶段填充"
```

---

### Task 2: 搬入 region 包纯 Java 文件到 common

**Files:**
- Move: `src/main/java/com/suian/xaeroregionsrev/region/{ArgbColor,ColorPaletteLimits,PermissionProfile,PointMarker,PolygonMath,Region,RegionColorParser,RegionId,RegionPoint,RegionRequestValidator,RegionStyleUpdater}.java` → `common/src/main/java/com/suian/xaeroregionsrev/region/`
- Move: `src/main/java/com/suian/xaeroregionsrev/region/RegionLimits.java` → `common/src/main/java/com/suian/xaeroregionsrev/region/`（master 版本，含 forge 缺少的 4 个常量）

**Interfaces:**
- Produces: common 的 `region` 包，11 个零依赖文件 + RegionLimits（master 版本）

- [ ] **Step 1: 创建目标目录**

Run: `mkdir -p common/src/main/java/com/suian/xaeroregionsrev/region`

- [ ] **Step 2: 用 git mv 搬移 11 个零依赖文件 + RegionLimits**

```bash
for f in ArgbColor ColorPaletteLimits PermissionProfile PointMarker PolygonMath Region RegionColorParser RegionId RegionPoint RegionRequestValidator RegionStyleUpdater RegionLimits; do
    git mv "src/main/java/com/suian/xaeroregionsrev/region/$f.java" "common/src/main/java/com/suian/xaeroregionsrev/region/$f.java"
done
```

> 注意：RegionNbtCodec 暂不搬，它依赖 CompoundTag/ListTag，阶段 2 处理。

- [ ] **Step 3: 验证 common 编译**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL（这些文件零依赖，且 package 声明不变）

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: 搬移 region 包纯 Java 文件到 common 模块

搬入 11 个零平台依赖文件 + RegionLimits（master 版本含 forge 缺少的
NBT 字段长度常量）。RegionNbtCodec 待阶段 2 随 NBT 抽象接口一起搬入。"
```

---

### Task 3: 搬入 client 纯逻辑文件到 common

**Files:**
- Move: `src/main/java/com/suian/xaeroregionsrev/client/editor/{ColorPickerModel,RegionEditSession,RegionManagerListModel,RegionSelection}.java` → `common/src/main/java/com/suian/xaeroregionsrev/client/editor/`
- Move: `src/main/java/com/suian/xaeroregionsrev/client/xaero/{RegionLabelDisplay,RegionRenderStyle,XaeroMapInputRouter,PolygonTriangulator}.java` → `common/src/main/java/com/suian/xaeroregionsrev/client/xaero/`

> **注意：** `RegionSubmissionState` 看似零依赖，但其 `receive()` 方法签名接收 `RegionEditResultPacket`（payload wrapper 类），**不能在阶段 1 搬入 common**。推迟到阶段 2，届时先将 `receive` 签名改为基本类型参数（`long requestId, boolean success, boolean closeScreen, long nowMillis`），再搬入 common。

**Interfaces:**
- Produces: common 的 `client/editor`（4 个）和 `client/xaero`（4 个，含 JOML 依赖）包

- [ ] **Step 1: 创建目标目录**

Run: `mkdir -p common/src/main/java/com/suian/xaeroregionsrev/client/editor common/src/main/java/com/suian/xaeroregionsrev/client/xaero`

- [ ] **Step 2: 用 git mv 搬移 editor 文件**

```bash
for f in ColorPickerModel RegionEditSession RegionManagerListModel RegionSelection; do
    git mv "src/main/java/com/suian/xaeroregionsrev/client/editor/$f.java" "common/src/main/java/com/suian/xaeroregionsrev/client/editor/$f.java"
done
```

- [ ] **Step 3: 用 git mv 搬移 xaero 文件**

```bash
for f in RegionLabelDisplay RegionRenderStyle XaeroMapInputRouter PolygonTriangulator; do
    git mv "src/main/java/com/suian/xaeroregionsrev/client/xaero/$f.java" "common/src/main/java/com/suian/xaeroregionsrev/client/xaero/$f.java"
done
```

- [ ] **Step 4: 验证 common 编译（含 JOML 依赖）**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL（PolygonTriangulator 的 org.joml import 由 common 的 api 依赖提供）

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: 搬入 client 纯逻辑文件到 common 模块

- client/editor: ColorPickerModel、RegionEditSession、RegionManagerListModel、
  RegionSelection、RegionSubmissionState（5 个零依赖文件）
- client/xaero: RegionLabelDisplay、RegionRenderStyle、XaeroMapInputRouter、
  PolygonTriangulator（后者依赖 JOML，由 common 的 api 依赖提供）"
```

---

### Task 4: 搬入改 import 的 cache 文件 + 资源文件

**Files:**
- Move: `src/main/java/com/suian/xaeroregionsrev/client/{ClientRegionCache,ClientColorHistoryCache}.java` → `common/src/main/java/com/suian/xaeroregionsrev/client/`
- Modify: 上述两个文件，将 `import com.mojang.logging.LogUtils` 改为 `import org.slf4j.LoggerFactory`
- Move: `src/main/resources/assets/` → `common/src/main/resources/assets/`

**Interfaces:**
- Produces: common 的 `client/ClientRegionCache`、`client/ClientColorHistoryCache`（用 SLF4J 直接获取 Logger）和 `assets/` 资源

- [ ] **Step 1: 创建目标目录**

Run: `mkdir -p common/src/main/java/com/suian/xaeroregionsrev/client common/src/main/resources`

- [ ] **Step 2: 搬移 cache 文件**

```bash
for f in ClientRegionCache ClientColorHistoryCache; do
    git mv "src/main/java/com/suian/xaeroregionsrev/client/$f.java" "common/src/main/java/com/suian/xaeroregionsrev/client/$f.java"
done
```

- [ ] **Step 3: 修改 ClientRegionCache.java 的 import**

将 `common/src/main/java/com/suian/xaeroregionsrev/client/ClientRegionCache.java` 中的：
```java
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
```
改为：
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```
同时将 `LogUtils.getLogger()` 改为 `LoggerFactory.getLogger(ClientRegionCache.class)`。

- [ ] **Step 4: 修改 ClientColorHistoryCache.java 的 import**

将 `common/src/main/java/com/suian/xaeroregionsrev/client/ClientColorHistoryCache.java` 中的：
```java
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
```
改为：
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```
同时将 `LogUtils.getLogger()` 改为 `LoggerFactory.getLogger(ClientColorHistoryCache.class)`。

- [ ] **Step 5: 搬移 assets 资源**

```bash
git mv src/main/resources/assets common/src/main/resources/assets
```

- [ ] **Step 6: 验证 common 编译**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: 搬入 cache 文件和资源到 common

- ClientRegionCache、ClientColorHistoryCache：LogUtils 改为 SLF4J LoggerFactory
- assets 资源（lang、textures）搬入 common/src/main/resources"
```

---

### Task 5: 搬入纯 Java 测试到 common

**Files:**
- Move: 以下 23 个测试文件 → `common/src/test/java/com/suian/xaeroregionsrev/`（保持包结构）

纯 Java 测试文件清单（无 MC/LWJGL/Netty 依赖）：
```
client/ClientLocalConfigTest.java
client/XaeroRegionsClientTest.java
client/editor/ClientFavoriteColorStoreTest.java
client/editor/ColorPickerModelTest.java
client/editor/RegionContextMenuTest.java
client/editor/RegionEditSessionTest.java
client/editor/RegionEditorOverlayTest.java
client/editor/RegionManagerListModelTest.java
client/editor/RegionSelectionTest.java
client/editor/RegionStyleEditScreenTest.java
client/editor/RegionSubmissionStateTest.java
client/xaero/MapProjectionAdapterTest.java
client/xaero/PolygonTriangulatorTest.java
client/xaero/RegionLabelDisplayTest.java
client/xaero/RegionRenderStyleTest.java
client/xaero/XaeroMapInputHandlerTest.java
client/xaero/XaeroMapInputRouterTest.java
client/xaero/XaeroScreenDetectorTest.java
network/RegionEditRequestHandlerTest.java
region/PermissionProfileTest.java
region/PolygonMathTest.java
region/RegionColorParserTest.java
region/RegionRequestValidatorTest.java
region/RegionStyleUpdaterTest.java
region/RegionTest.java
```

> 注意：以下测试依赖被测类仍在 common 中，但部分被测类将在后续阶段才搬入（如 RegionEditRequestHandler），搬入后如编译失败需暂时注释或跳过。**实际上所有被测类到阶段 2 末才全部就位，此阶段只搬入被测类已在 common 的测试。**

**调整后的搬入清单（被测类已在 common 的测试）：**

```
client/editor/ColorPickerModelTest.java
client/editor/RegionEditSessionTest.java
client/editor/RegionManagerListModelTest.java
client/editor/RegionSelectionTest.java
client/xaero/PolygonTriangulatorTest.java
client/xaero/RegionLabelDisplayTest.java
client/xaero/RegionRenderStyleTest.java
client/xaero/XaeroMapInputRouterTest.java
region/PermissionProfileTest.java
region/PolygonMathTest.java
region/RegionColorParserTest.java
region/RegionRequestValidatorTest.java
region/RegionStyleUpdaterTest.java
region/RegionTest.java
```

> 注意：`RegionSubmissionStateTest` 不在此清单——被测类 `RegionSubmissionState` 的 `receive()` 方法依赖 `RegionEditResultPacket`，推迟到阶段 2 签名改造后再搬入。`ClientFavoriteColorStoreTest` 的被测类依赖 `FMLPaths`（NeoForge 专属），永不进 common。

> 其余 8 个测试（依赖 ClientFavoriteColorStore、RegionContextMenu、RegionEditorOverlay、MapProjectionAdapter、XaeroMapInputHandler、XaeroScreenDetector、XaeroRegionsClient、RegionEditRequestHandler、ClientLocalConfig）的被测类此时尚未搬入 common，留待对应被测类搬入后再一起搬入。

- [ ] **Step 1: 创建测试目标目录**

```bash
mkdir -p common/src/test/java/com/suian/xaeroregionsrev/{region,client/editor,client/xaero}
```

- [ ] **Step 2: 搬入被测类已在 common 的 15 个测试**

```bash
cd common/src/test/java/com/suian/xaeroregionsrev

for f in PermissionProfileTest PolygonMathTest RegionColorParserTest RegionRequestValidatorTest RegionStyleUpdaterTest RegionTest; do
    git mv "../../../../../src/test/java/com/suian/xaeroregionsrev/region/$f.java" "region/$f.java"
done

for f in ColorPickerModelTest RegionEditSessionTest RegionManagerListModelTest RegionSelectionTest; do
    git mv "../../../../../src/test/java/com/suian/xaeroregionsrev/client/editor/$f.java" "client/editor/$f.java"
done

for f in PolygonTriangulatorTest RegionLabelDisplayTest RegionRenderStyleTest XaeroMapInputRouterTest; do
    git mv "../../../../../src/test/java/com/suian/xaeroregionsrev/client/xaero/$f.java" "client/xaero/$f.java"
done
```

> 上面的相对路径写法不好操作。实际操作时用 `git mv` 配合绝对路径或从仓库根开始的相对路径逐个执行。

- [ ] **Step 3: 运行 common 测试**

Run: `./gradlew :common:test`
Expected: BUILD SUCCESSFUL，15 个测试全绿

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test: 搬入纯 Java 测试到 common 模块

搬入 15 个被测类已在 common 的测试文件。依赖尚未搬入的被测类的
测试（ClientFavoriteColorStore、RegionContextMenu、RegionEditorOverlay 等）
留待对应被测类搬入后再一起搬入。"
```

---

### Task 6: 阶段 1 验证检查点

**Files:**
- 无文件变更

- [ ] **Step 1: 验证 common 完整编译和测试**

Run: `./gradlew :common:clean :common:test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 验证 Gradle 项目结构**

Run: `./gradlew projects`
Expected: 列出 common + neoforge:mc-1.21.1 + forge:mc-1.20.1

- [ ] **Step 3: 确认旧的 src/ 目录仍保留平台文件**

旧 `src/main/java` 中应还保留：XaeroRegionsRev、client/ 下平台相关文件、command/、data/、network/、platform/、service/ 等。这些在阶段 2-3 迁移。

Run: `git ls-files 'src/main/java/**/*.java' | wc -l`
Expected: 约 30-35 个文件（搬走了约 22 个）

- [ ] **Step 4: 此检查点无需 commit（上一步已提交）**

---

## 阶段 2：NeoForge 子项目迁移

**目标**：`./gradlew :neoforge:mc-1.21.1:runClient` 能启动游戏，所有 NeoForge 功能正常。

### Task 7: 创建 NeoForge 子项目构建配置

**Files:**
- Modify: `neoforge/mc-1.21.1/build.gradle`（替换占位内容）
- Create: `neoforge/mc-1.21.1/gradle.properties`
- Move: `src/main/resources/META-INF/neoforge.mods.toml` → `neoforge/mc-1.21.1/src/main/resources/META-INF/neoforge.mods.toml`
- Move: `src/main/resources/pack.mcmeta` → `neoforge/mc-1.21.1/src/main/resources/pack.mcmeta`

**Interfaces:**
- Consumes: common 子项目（通过 sourceSet 纳入）
- Produces: 可编译的 NeoForge 子项目

- [ ] **Step 1: 创建 neoforge/mc-1.21.1/gradle.properties**

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

- [ ] **Step 2: 重写 neoforge/mc-1.21.1/build.gradle**

```groovy
plugins {
    id 'java-library'
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.neoforged.moddev' version '2.0.141'
}

apply from: "$rootDir/buildSrc/src/main/groovy/region-platform.gradle"

version = "${mod_version}+${artifact_loader}-${minecraft_version}"
group = mod_group_id

base {
    archivesName = archives_base_name
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = project.neo_version

    runs {
        configureEach {
            gameDirectory = project.file('run')
            systemProperty 'forge.logging.markers', 'REGISTRIES'
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
            logLevel = org.slf4j.event.Level.DEBUG
        }

        client {
            client()
            gameDirectory = project.file('run-client')
        }

        server {
            server()
            gameDirectory = project.file('run-server')
            programArgument '--nogui'
        }

        gameTestServer {
            type = 'gameTestServer'
        }

        data {
            data()
            gameDirectory = project.file('run-data')
            programArguments = [
                    '--mod', mod_id,
                    '--all',
                    '--output', file('src/generated/resources/').absolutePath,
                    '--existing', file('src/main/resources/').absolutePath
            ]
        }
    }

    mods {
        "${mod_id}" {
            sourceSet project(':common').sourceSets.main
            sourceSet sourceSets.main
        }
    }

    unitTest {
        enable()
        testedMod = mods."${mod_id}"
    }
}

sourceSets.main.resources { srcDir 'src/generated/resources' }

configurations {
    clientRuntimeMods {
        canBeConsumed = false
        canBeResolved = true
    }
}

dependencies {
    implementation project(':common')

    clientRuntimeMods "curse.maven:xaeros-world-map-317780:${xaero_world_map_runtime_file_id}"
    clientRuntimeMods "curse.maven:imblocker-483760:${imblocker_runtime_file_id}"

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test', Test) {
    useJUnitPlatform()
}

tasks.named('clean', Delete).configure {
    delete 'bin', 'logs', '.tmp', 'run/logs', 'run-client/logs', 'run-server/logs', 'run-data/logs'
}

tasks.register('syncClientRuntimeMods', Sync) {
    group = 'neoForge runs'
    description = 'Sync external runtime mods for the NeoForge client run.'
    from configurations.clientRuntimeMods
    into layout.projectDirectory.dir('run-client/mods')
}

tasks.named('prepareClientRun') {
    dependsOn tasks.named('syncClientRuntimeMods')
}

tasks.named('runClient') {
    dependsOn tasks.named('syncClientRuntimeMods')
}

tasks.named('processResources', ProcessResources).configure {
    from(project(':common').sourceSets.main.resources)

    var replaceProperties = [
            minecraft_version           : minecraft_version,
            minecraft_version_range     : minecraft_version_range,
            neo_version                 : neo_version,
            neo_version_range           : neo_version_range,
            loader_version_range        : loader_version_range,
            xaero_world_map_version_range: xaero_world_map_version_range,
            mod_id                      : mod_id,
            mod_name                    : mod_name,
            mod_license                 : mod_license,
            mod_version                 : mod_version,
            mod_authors                 : mod_authors,
            mod_description             : mod_description
    ]
    inputs.properties replaceProperties

    filesMatching(['META-INF/neoforge.mods.toml', 'pack.mcmeta']) {
        expand replaceProperties + [project: project]
    }
}

tasks.register('verifyArchiveName') {
    group = 'verification'
    description = 'Verify the built jar name includes the loader and Minecraft version.'
    dependsOn tasks.named('jar')

    doLast {
        String expectedName = "${archives_base_name}-${mod_version}+${artifact_loader}-${minecraft_version}.jar"
        String actualName = tasks.named('jar', Jar).get().archiveFileName.get()
        if (actualName != expectedName) {
            throw new GradleException("Unexpected jar name '${actualName}'. Expected '${expectedName}'.")
        }
    }
}

tasks.named('check') {
    dependsOn tasks.named('verifyArchiveName')
}
```

- [ ] **Step 3: 搬移 NeoForge 特有资源**

```bash
mkdir -p neoforge/mc-1.21.1/src/main/resources/META-INF
git mv src/main/resources/META-INF/neoforge.mods.toml neoforge/mc-1.21.1/src/main/resources/META-INF/neoforge.mods.toml
# pack.mcmeta 也搬过去（NeoForge 版，pack_format=34）
git mv src/main/resources/pack.mcmeta neoforge/mc-1.21.1/src/main/resources/pack.mcmeta
```

> 注意：src/main/resources 下此时应该只剩 META-INF/mods.toml（如果旧的 forge 版本还留着的话）或为空。assets 已在 Task 4 搬到 common。

- [ ] **Step 4: 验证 NeoForge 子项目配置可解析**

Run: `./gradlew :neoforge:mc-1.21.1:tasks`
Expected: 列出 runClient、build、test 等任务（不执行，只确认配置无语法错误）

> 注意：此时 `src/main/java` 仍为空（源码还没搬），编译会失败，但任务列表应能正确解析。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: 创建 NeoForge 子项目构建配置

- neoforge/mc-1.21.1/build.gradle：ModDevGradle + region-platform +
  common sourceSet 纳入 + syncClientRuntimeMods + verifyArchiveName
- neoforge/mc-1.21.1/gradle.properties：MC 1.21.1 专属属性
- 搬移 neoforge.mods.toml 和 pack.mcmeta 到子项目 resources"
```

---

### Task 8: 创建网络层抽象接口（PacketBuffer）

**Files:**
- Create: `common/src/main/java/com/suian/xaeroregionsrev/network/buffer/PacketBuffer.java`
- Create: `common/src/main/java/com/suian/xaeroregionsrev/network/buffer/PacketBufferExtensions.java`（可选，如有 List 读写需求）

**Interfaces:**
- Produces: `PacketBuffer` 接口，定义所有 payload 使用到的读写方法

- [ ] **Step 1: 分析 payload 使用的所有 FriendlyByteBuf 方法**

从调研已知 payload 使用的方法：
- `writeUtf(String, int)` / `readUtf(int)` — 带最大长度的字符串
- `writeInt(int)` / `readInt()`
- `writeLong(long)` / `readLong()`
- `writeBoolean(boolean)` / `readBoolean()`
- `writeVarInt(int)` / `readVarInt()`

- [ ] **Step 2: 创建 PacketBuffer 接口**

```java
package com.suian.xaeroregionsrev.network.buffer;

/**
 * 平台无关的网络缓冲区接口，抽象 FriendlyByteBuf 的 payload 编解码方法子集。
 * 各平台子项目提供适配实现（如 FriendlyByteBufPacketBuffer）。
 */
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

- [ ] **Step 3: 验证 common 编译**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/com/suian/xaeroregionsrev/network/buffer/PacketBuffer.java
git commit -m "feat: 添加 PacketBuffer 网络缓冲区抽象接口

定义 payload 编解码使用的 FriendlyByteBuf 方法子集（writeUtf/readUtf、
writeInt/readInt、writeLong/readLong、writeBoolean/readBoolean、
writeVarInt/readVarInt），供平台子项目适配实现。"
```

---

### Task 9: 拆分 payload data 层到 common

**Files:**
- Create: `common/src/main/java/com/suian/xaeroregionsrev/network/data/` 下 8 个 data record
- 修改旧 payload 文件使其委托给 data record（此步在 NeoForge 子项目源码搬入后完成）

**Interfaces:**
- Consumes: `PacketBuffer`（Task 8）、`Region`、`RegionId`、`RegionPoint`、`ArgbColor`、`RegionLimits`
- Produces: 8 个 data record，每个暴露 `encode(PacketBuffer, Data)` 和 `decode(PacketBuffer)` 静态方法

> 这是一个大任务。8 个 payload 的 encode/decode 方法逐字相同，只是参数类型从 FriendlyByteBuf 改为 PacketBuffer。

- [ ] **Step 1: 创建 RegionSyncData**

从 `RegionSyncPacket.java`（行 38-131）提取 encode/decode/validate 逻辑，`FriendlyByteBuf` 全部替换为 `PacketBuffer`：

```java
package com.suian.xaeroregionsrev.network.data;

import com.suian.xaeroregionsrev.network.buffer.PacketBuffer;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionLimits;
import com.suian.xaeroregionsrev.region.RegionPoint;

import java.util.ArrayList;
import java.util.List;

public record RegionSyncData(List<Region> regions) {
    public static final int MAX_REGIONS = 4096;
    public static final int MAX_POINTS_PER_REGION = RegionLimits.MAX_POINTS_PER_REQUEST;
    public static final int MAX_TOTAL_POINTS = 8192;

    public RegionSyncData {
        regions = List.copyOf(regions);
    }

    public static void encode(PacketBuffer buffer, RegionSyncData data) {
        validateRegionCount(data.regions.size());
        validateTotalPoints(data.regions);
        buffer.writeVarInt(data.regions.size());
        for (Region region : data.regions) {
            validatePointCount(region.points().size());
            buffer.writeUtf(region.id().value(), RegionLimits.MAX_ID_LENGTH);
            buffer.writeUtf(region.name(), RegionLimits.MAX_NAME_LENGTH);
            buffer.writeUtf(region.dimension(), RegionLimits.MAX_DIMENSION_LENGTH);
            buffer.writeInt(region.color().value());
            buffer.writeUtf(region.label(), RegionLimits.MAX_LABEL_LENGTH);
            buffer.writeInt(region.labelColor().value());
            buffer.writeUtf(region.category(), RegionLimits.MAX_CATEGORY_LENGTH);
            buffer.writeUtf(region.iconName(), RegionLimits.MAX_ICON_NAME_LENGTH);
            buffer.writeLong(region.createdAt());
            buffer.writeLong(region.updatedAt());
            buffer.writeVarInt(region.points().size());
            for (RegionPoint point : region.points()) {
                buffer.writeInt(point.x());
                buffer.writeInt(point.z());
            }
        }
    }

    public static RegionSyncData decode(PacketBuffer buffer) {
        int size = readBoundedCount(buffer, MAX_REGIONS, regionCountMessage());
        List<Region> regions = new ArrayList<>(size);
        int totalPoints = 0;
        for (int i = 0; i < size; i++) {
            RegionId id = new RegionId(buffer.readUtf(RegionLimits.MAX_ID_LENGTH));
            String name = buffer.readUtf(RegionLimits.MAX_NAME_LENGTH);
            String dimension = buffer.readUtf(RegionLimits.MAX_DIMENSION_LENGTH);
            ArgBColor color = new ArgbColor(buffer.readInt());
            String label = buffer.readUtf(RegionLimits.MAX_LABEL_LENGTH);
            ArgBColor labelColor = new ArgbColor(buffer.readInt());
            String category = buffer.readUtf(RegionLimits.MAX_CATEGORY_LENGTH);
            String iconName = buffer.readUtf(RegionLimits.MAX_ICON_NAME_LENGTH);
            long createdAt = buffer.readLong();
            long updatedAt = buffer.readLong();
            int pointCount = readBoundedCount(buffer, MAX_POINTS_PER_REGION, pointCountMessage());
            if (pointCount < 3) {
                throw new IllegalArgumentException("Region points must form a valid polygon.");
            }
            totalPoints += pointCount;
            if (totalPoints > MAX_TOTAL_POINTS) {
                throw new IllegalArgumentException("Total synced region point count cannot exceed " + MAX_TOTAL_POINTS + ".");
            }
            List<RegionPoint> points = new ArrayList<>(pointCount);
            for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
                points.add(new RegionPoint(buffer.readInt(), buffer.readInt()));
            }
            regions.add(new Region(id, name, dimension, color, label, labelColor, category, iconName, points, createdAt, updatedAt));
        }
        return new RegionSyncData(regions);
    }

    // ... validateRegionCount, validatePointCount, validateTotalPoints,
    //     readBoundedCount, regionCountMessage, pointCountMessage
    // （从 RegionSyncPacket 原样搬入，FriendlyByteBuf → PacketBuffer）
}
```

> **注意：** 上面的 `ArgBColor` 是笔误，应为 `ArgbColor`。实现时以原始 payload 代码为准。

- [ ] **Step 1b: 每个 data record 保留原始构造器和紧凑构造器**

data record 必须保留原始 payload record 的所有公共构造器（包括便捷构造器）和紧凑构造器中的防御性拷贝逻辑（如 `List.copyOf`）。实现时逐个核对原始 payload 的构造器，原样搬入 data record。

特别注意：
- `DeleteRegionRequestData`：保留 `idText()` 和派生的 `id()` 方法
- `UpdateRegionStyleRequestData`：保留 4 个构造器（含 `requestId=0` 默认、`RegionId` 转换等）
- `RegionEditResultData`：字段为 `requestId, success, closeScreen, message`（4 个字段，**不可遗漏 closeScreen**）

- [ ] **Step 2: 对剩余 7 个 payload 重复同样的拆分**

逐个读取原始 payload 文件，提取 encode/decode 方法，将 `FriendlyByteBuf` 替换为 `PacketBuffer`，创建对应的 `*Data.java`：

- `CreateRegionRequestData.java` ← CreateRegionRequestPacket
- `DeleteRegionRequestData.java` ← DeleteRegionRequestPacket
- `UpdateRegionStyleRequestData.java` ← UpdateRegionStyleRequestPacket
- `ColorHistorySyncData.java` ← ColorHistorySyncPacket
- `ColorHistoryUpdateRequestData.java` ← ColorHistoryUpdateRequestPacket
- `RegionRefreshData.java` ← RegionRefreshRequestPacket
- `RegionEditResultData.java` ← RegionEditResultPacket

每个 data record：
1. 保留 record 的字段定义和构造器
2. encode/decode 签名改为 `(PacketBuffer, Data)` / `(PacketBuffer)`
3. 所有验证方法原样搬入
4. 常量原样搬入

- [ ] **Step 3: 淘汰 PacketCodecs 帮助类**

`PacketCodecs` 原来依赖 `StreamCodec`（MC 类），不能进 common。**删除 PacketCodecs.java**，各平台 wrapper 的 `STREAM_CODEC` 改用 MC 原生 `StreamCodec.of(encode, decode)`（NeoForge）或 SimpleChannel 的注册方式（Forge）。NeoForge 的 `StreamCodec.of` 签名与原 `PacketCodecs.of` 一致，直接替换即可。

- [ ] **Step 4: 验证 common 编译**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/suian/xaeroregionsrev/network/data/
git commit -m "feat: 拆分 payload data 层到 common

将 8 个 payload 的 encode/decode 纯逻辑提取为 data record
（RegionSyncData、CreateRegionRequestData、DeleteRegionRequestData、
UpdateRegionStyleRequestData、ColorHistorySyncData、
ColorHistoryUpdateRequestData、RegionRefreshData、RegionEditResultData），
参数类型从 FriendlyByteBuf 改为 PacketBuffer 抽象接口。"
```

---

### Task 10: 创建 NBT 抽象接口 + 搬入 RegionNbtCodec

**Files:**
- Create: `common/src/main/java/com/suian/xaeroregionsrev/region/nbt/NbtCompound.java`
- Create: `common/src/main/java/com/suian/xaeroregionsrev/region/nbt/NbtList.java`
- Move + Modify: `src/main/java/com/suian/xaeroregionsrev/region/RegionNbtCodec.java` → `common/src/main/java/com/suian/xaeroregionsrev/region/nbt/RegionNbtCodec.java`

**Interfaces:**
- Consumes: `Region`、`RegionPoint`、`PolygonMath`、`RegionLimits`、`ArgbColor`、`RegionId`
- Produces: `NbtCompound` 接口、`NbtList` 接口、改造后的 `RegionNbtCodec`

- [ ] **Step 1: 分析 RegionNbtCodec 使用的 CompoundTag/ListTag 方法**

从代码已知：
- CompoundTag: `putString`、`getString`、`putInt`、`getInt`、`putLong`、`getLong`、`put`（存 ListTag）、`getList`、`contains(key, type)`
- ListTag: `add`、`size`、`getCompound`
- 静态常量: TAG_INT=3、TAG_LONG=4、TAG_STRING=8、TAG_LIST=9、TAG_COMPOUND=10

- [ ] **Step 2: 创建 NbtCompound 接口**

```java
package com.suian.xaeroregionsrev.region.nbt;

/**
 * 平台无关的 NBT 复合标签接口。
 * 各平台子项目提供适配实现（如 CompoundTagNbtCompound）。
 */
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
    void remove(String key);

    // NBT type id 常量（与 Minecraft 的 TagTags 对应）
    int TAG_INT = 3;
    int TAG_LONG = 4;
    int TAG_STRING = 8;
    int TAG_LIST = 9;
    int TAG_COMPOUND = 10;
}
```

- [ ] **Step 3: 创建 NbtList 接口**

```java
package com.suian.xaeroregionsrev.region.nbt;

/**
 * 平台无关的 NBT 列表标签接口。
 */
public interface NbtList {
    void add(NbtCompound compound);
    NbtCompound getCompound(int index);
    int size();
}
```

- [ ] **Step 4: 改造 RegionNbtCodec，搬入 common**

将 `src/main/java/com/suian/xaeroregionsrev/region/RegionNbtCodec.java` 搬到 `common/src/main/java/com/suian/xaeroregionsrev/region/nbt/RegionNbtCodec.java`，做以下改动：

1. package 改为 `com.suian.xaeroregionsrev.region.nbt`
2. import `CompoundTag` / `ListTag` 改为引用同包的 `NbtCompound` / `NbtList`
3. `writeRegion` 返回类型改为 `NbtCompound`，内部 `new CompoundTag()` → 由接口的工厂方法创建（需要一个 `NbtCompound create()` 静态方法或工厂接口）

> **设计决策：** RegionNbtCodec 需要创建 NbtCompound/NbtList 实例。在 common 中不能直接 new。方案：在 `NbtCompound` 接口中增加静态工厂注册点，或让 `writeRegion` 接收工厂参数。
>
> **采用工厂参数方案**（更简单、无全局状态）：
>
> 定义 `NbtFactory` 接口：
> ```java
> package com.suian.xaeroregionsrev.region.nbt;
> public interface NbtFactory {
>     NbtCompound createCompound();
>     NbtList createList();
> }
> ```
> `writeRegion` 签名改为 `writeRegion(NbtFactory factory, Region region)`。

- [ ] **Step 5: 创建 NbtFactory 接口**

```java
package com.suian.xaeroregionsrev.region.nbt;

/**
 * NBT 对象工厂接口，供平台子项目注入 CompoundTag/ListTag 创建逻辑。
 */
public interface NbtFactory {
    NbtCompound createCompound();
    NbtList createList();
}
```

- [ ] **Step 6: 完成改造后的 RegionNbtCodec**

```java
package com.suian.xaeroregionsrev.region.nbt;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.PolygonMath;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionLimits;
import com.suian.xaeroregionsrev.region.RegionPoint;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RegionNbtCodec {
    private RegionNbtCodec() {
    }

    public static NbtCompound writeRegion(NbtFactory factory, Region region) {
        Objects.requireNonNull(region, "Region cannot be null.");

        NbtCompound tag = factory.createCompound();
        tag.putString("id", region.id().value());
        tag.putString("name", region.name());
        tag.putString("dimension", region.dimension());
        tag.putInt("color", region.color().value());
        tag.putString("label", region.label());
        tag.putInt("labelColor", region.labelColor().value());
        tag.putString("category", region.category());
        tag.putString("iconName", region.iconName());
        tag.putLong("createdAt", region.createdAt());
        tag.putLong("updatedAt", region.updatedAt());

        NbtList points = factory.createList();
        for (RegionPoint point : region.points()) {
            NbtCompound pointTag = factory.createCompound();
            pointTag.putInt("x", point.x());
            pointTag.putInt("z", point.z());
            points.add(pointTag);
        }
        tag.put("points", points);
        return tag;
    }

    public static Region readRegion(NbtCompound tag) {
        Objects.requireNonNull(tag, "Region tag cannot be null.");
        requireField(tag, "id", NbtCompound.TAG_STRING);
        requireField(tag, "name", NbtCompound.TAG_STRING);
        requireField(tag, "dimension", NbtCompound.TAG_STRING);
        requireField(tag, "color", NbtCompound.TAG_INT);
        requireField(tag, "category", NbtCompound.TAG_STRING);
        requireField(tag, "iconName", NbtCompound.TAG_STRING);
        requireField(tag, "createdAt", NbtCompound.TAG_LONG);
        requireField(tag, "updatedAt", NbtCompound.TAG_LONG);
        requireField(tag, "points", NbtCompound.TAG_LIST);

        List<RegionPoint> points = new ArrayList<>();
        NbtList pointTags = tag.getList("points", NbtCompound.TAG_COMPOUND);
        if (pointTags.size() > RegionLimits.MAX_POINTS_PER_REQUEST) {
            throw new IllegalArgumentException("Region point count cannot exceed "
                    + RegionLimits.MAX_POINTS_PER_REQUEST + ".");
        }
        for (int i = 0; i < pointTags.size(); i++) {
            NbtCompound pointTag = pointTags.getCompound(i);
            requireField(pointTag, "x", NbtCompound.TAG_INT);
            requireField(pointTag, "z", NbtCompound.TAG_INT);
            points.add(new RegionPoint(pointTag.getInt("x"), pointTag.getInt("z")));
        }
        if (!PolygonMath.isValidPolygon(points)) {
            throw new IllegalArgumentException("Region points must form a valid polygon.");
        }

        String id = boundedString(tag, "id", RegionLimits.MAX_ID_LENGTH);
        String name = boundedString(tag, "name", RegionLimits.MAX_NAME_LENGTH);
        String dimension = boundedString(tag, "dimension", RegionLimits.MAX_DIMENSION_LENGTH);
        String label = tag.contains("label", NbtCompound.TAG_STRING)
                ? boundedString(tag, "label", RegionLimits.MAX_LABEL_LENGTH)
                : name;
        String category = boundedString(tag, "category", RegionLimits.MAX_CATEGORY_LENGTH);
        String iconName = boundedString(tag, "iconName", RegionLimits.MAX_ICON_NAME_LENGTH);

        return new Region(
                new RegionId(id),
                name,
                dimension,
                new ArgbColor(tag.getInt("color")),
                label,
                tag.contains("labelColor", NbtCompound.TAG_INT) ? new ArgbColor(tag.getInt("labelColor")) : new ArgbColor(0xFFFFFFFF),
                category,
                iconName,
                points,
                tag.getLong("createdAt"),
                tag.getLong("updatedAt")
        );
    }

    private static void requireField(NbtCompound tag, String key, int type) {
        if (!tag.contains(key, type)) {
            throw new IllegalArgumentException("Missing or invalid region NBT field: " + key);
        }
    }

    private static String boundedString(NbtCompound tag, String key, int maxBytes) {
        String value = tag.getString(key);
        if (value.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            throw new IllegalArgumentException("Saved region NBT field '" + key
                    + "' cannot exceed " + maxBytes + " UTF-8 bytes.");
        }
        return value;
    }
}
```

- [ ] **Step 7: 删除旧的 RegionNbtCodec**

```bash
git rm src/main/java/com/suian/xaeroregionsrev/region/RegionNbtCodec.java
```

- [ ] **Step 8: 验证 common 编译**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: 添加 NBT 抽象接口并搬入 RegionNbtCodec

- 新增 NbtCompound、NbtList、NbtFactory 三个接口隔离 MC NBT API
- RegionNbtCodec 从 region 包移到 region.nbt 包，改用抽象接口
- writeRegion 新增 NbtFactory 参数用于创建 NBT 实例
- boundedString 校验逻辑随之下沉到 common（forge 线自动获得）"
```

---

### Task 11: 创建服务端/客户端平台抽象接口

**Files:**
- Create: `common/src/main/java/com/suian/xaeroregionsrev/service/ServerContext.java`
- Create: `common/src/main/java/com/suian/xaeroregionsrev/service/RegionStore.java`
- Move + Modify: `src/main/java/com/suian/xaeroregionsrev/service/RegionService.java` → `common/src/main/java/com/suian/xaeroregionsrev/service/RegionService.java`
- Create: `common/src/main/java/com/suian/xaeroregionsrev/client/EditResultHandler.java`
- Move + Modify: `src/main/java/com/suian/xaeroregionsrev/client/ClientRegionEditResultHandler.java` → `common/src/main/java/com/suian/xaeroregionsrev/client/ClientRegionEditResultHandler.java`

**Interfaces:**
- Consumes: `Region`、`RegionId`、`ArgbColor`、`RegionStyleUpdater`
- Produces: `ServerContext`、`RegionStore`、改造后的 `RegionService`、`EditResultHandler`、改造后的 `ClientRegionEditResultHandler`

- [ ] **Step 1: 分析 RegionService 对 MC API 的依赖**

从代码已知 RegionService 调用：
- `ServerLevel` → `RegionSavedData.get(level)` → `data.allRegions()` / `find(id)` / `put(region)` / `remove(id)` / `colorHistory()` / `rememberColor(color, limit)`
- `MinecraftServer` → `server.getAllLevels()` → 遍历 ServerLevel
- `MinecraftServer` → `server.overworld()` → 获取主世界 ServerLevel

- [ ] **Step 2: 创建 RegionStore 接口**

```java
package com.suian.xaeroregionsrev.service;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 区域持久化存储抽象接口。
 * 各平台子项目通过适配 RegionSavedData 实现。
 */
public interface RegionStore {
    Collection<Region> allRegions();
    Optional<Region> find(RegionId id);
    void put(Region region);
    boolean remove(RegionId id);
    List<ArgbColor> colorHistory();
    void rememberColor(ArgbColor color, int limit);
}
```

- [ ] **Step 3: 创建 ServerContext 接口**

```java
package com.suian.xaeroregionsrev.service;

/**
 * 服务端上下文抽象接口。
 * 各平台子项目包装 MinecraftServer/ServerLevel 实现。
 */
public interface ServerContext {
    /** 获取主世界存储 */
    RegionStore overworld();

    /** 获取指定维度的存储 */
    RegionStore getLevel(String dimensionKey);

    /** 遍历所有维度的存储 */
    Iterable<RegionStore> allLevels();
}
```

- [ ] **Step 4: 改造 RegionService**

```java
package com.suian.xaeroregionsrev.service;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionStyleUpdater;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RegionService {
    public Collection<Region> list(RegionStore store) {
        Objects.requireNonNull(store, "Region store cannot be null.");
        return store.allRegions();
    }

    public List<Region> snapshot(ServerContext server) {
        Objects.requireNonNull(server, "Server context cannot be null.");
        List<Region> regions = new ArrayList<>();
        for (RegionStore store : server.allLevels()) {
            regions.addAll(list(store));
        }
        return List.copyOf(regions);
    }

    public Optional<Region> find(RegionStore store, RegionId id) {
        Objects.requireNonNull(store, "Region store cannot be null.");
        Objects.requireNonNull(id, "Region id cannot be null.");
        return store.find(id);
    }

    public void upsert(RegionStore store, Region region) {
        Objects.requireNonNull(store, "Region store cannot be null.");
        Objects.requireNonNull(region, "Region cannot be null.");
        if (!region.hasValidPolygon()) {
            throw new IllegalArgumentException("Region polygon must contain at least three points.");
        }
        store.put(region);
    }

    public Optional<Region> updateStyle(
            RegionStore store,
            RegionId id,
            ArgbColor fillColor,
            String label,
            ArgbColor labelColor,
            long now
    ) {
        Objects.requireNonNull(store, "Region store cannot be null.");
        Objects.requireNonNull(id, "Region id cannot be null.");
        Optional<Region> existing = store.find(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        Region updated = RegionStyleUpdater.withStyle(existing.get(), fillColor, label, labelColor, now);
        store.put(updated);
        return Optional.of(updated);
    }

    public boolean delete(RegionStore store, RegionId id) {
        Objects.requireNonNull(store, "Region store cannot be null.");
        Objects.requireNonNull(id, "Region id cannot be null.");
        return store.remove(id);
    }

    public List<ArgbColor> colorHistory(ServerContext server) {
        Objects.requireNonNull(server, "Server context cannot be null.");
        return server.overworld().colorHistory();
    }

    public List<ArgbColor> rememberColor(ServerContext server, ArgbColor color, int limit) {
        Objects.requireNonNull(server, "Server context cannot be null.");
        Objects.requireNonNull(color, "Color cannot be null.");
        server.overworld().rememberColor(color, limit);
        return server.overworld().colorHistory();
    }
}
```

- [ ] **Step 5: 创建 EditResultHandler 接口**

```java
package com.suian.xaeroregionsrev.client;

/**
 * 编辑结果回调接口。
 * 平台子项目注入实现，检查当前 Screen 类型并转发编辑结果。
 */
public interface EditResultHandler {
    /**
     * 处理编辑结果，如果当前屏幕是编辑器则转发结果。
     * @param requestId 请求 ID
     * @param success 是否成功
     * @param closeScreen 是否应关闭编辑器屏幕（success && closeScreen 才真正关屏）
     * @param message 结果消息
     */
    void handleEditResult(long requestId, boolean success, boolean closeScreen, String message);
}
```

> **设计说明：** `ClientRegionEditResultHandler` 原来检查 `minecraft.screen instanceof RegionStyleEditScreen` 并调用 `screen.handleEditResult(packet)`。抽象后用 `EditResultHandler` 接口封装这个行为。平台实现负责检查当前 Screen 类型并转发。`RegionEditResultPacket` 的字段拆分为 data record（Task 9），这里通过基本类型参数传递。**closeScreen 参数不可省略**——`RegionSubmissionState.receive` 依赖 `success && closeScreen` 判断是否关闭屏幕。

- [ ] **Step 6: 改造 ClientRegionEditResultHandler**

```java
package com.suian.xaeroregionsrev.client;

public final class ClientRegionEditResultHandler {
    private static EditResultHandler handler;

    private ClientRegionEditResultHandler() {
    }

    public static void setEditResultHandler(EditResultHandler editResultHandler) {
        handler = editResultHandler;
    }

    public static void handle(long requestId, boolean success, boolean closeScreen, String message) {
        if (handler != null) {
            handler.handleEditResult(requestId, success, closeScreen, message);
        }
    }
}
```

- [ ] **Step 7: 删除旧的平台依赖文件**

```bash
git rm src/main/java/com/suian/xaeroregionsrev/service/RegionService.java
git rm src/main/java/com/suian/xaeroregionsrev/client/ClientRegionEditResultHandler.java
```

- [ ] **Step 8: 验证 common 编译**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: 添加服务端/客户端平台抽象接口

- ServerContext/RegionStore：隔离 MinecraftServer/ServerLevel/RegionSavedData
- RegionService 改用 ServerContext/RegionStore 接口
- EditResultHandler：隔离 Minecraft.getInstance()/Screen 依赖
- ClientRegionEditResultHandler 改用 EditResultHandler 回调"
```

---

### Task 12: 搬移 NeoForge 源码到子项目

**Files:**
- Move: `src/main/java/com/suian/xaeroregionsrev/` 下剩余的 NeoForge 平台文件 → `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/`
- Move: 对应测试文件 → `neoforge/mc-1.21.1/src/test/java/`
- Modify: 搬移后的文件适配抽象接口（payload wrapper 委托 data record、RegionService 调用方式等）

**Interfaces:**
- Consumes: common 的全部公共类和接口
- Produces: 完整的 NeoForge 子项目，可编译可运行

> 这是工作量最大的 Task，涉及约 30 个文件的搬移和改造。

- [ ] **Step 1: 创建 NeoForge 子项目源码目录**

```bash
mkdir -p neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev
mkdir -p neoforge/mc-1.21.1/src/test/java/com/suian/xaeroregionsrev
```

- [ ] **Step 2: 批量搬移所有剩余 main 源文件**

将 `src/main/java/com/suian/xaeroregionsrev/` 下**所有剩余文件**用 `git mv` 搬到 `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/`，保持包结构。

```bash
# 列出剩余文件
git ls-files 'src/main/java/**/*.java'
# 逐个搬移（保持目录结构）
```

> 搬移清单包括（但不限于）：
> - `XaeroRegionsRev.java`
> - `client/` 下的平台文件（XaeroRegionsClient、RegionKeyMappings、ClientLocalConfig、ClientboundPayloadBridge、ClientboundPayloadDispatch、client/command/RegionClientCommands）
> - `client/editor/` 下的 GUI 文件（ColorPickerScreen、RegionContextMenu、RegionEditorOverlay、RegionManagerScreen、RegionStyleEditScreen、ClientFavoriteColorStore）
> - `client/xaero/` 下的平台文件（MapProjectionAdapter、PolygonFillRenderer、XaeroMapInputHandler、XaeroMapOverlayController、XaeroMapOverlayRenderer、XaeroScreenDetector）
> - `command/RegionCommands.java`
> - `data/RegionSavedData.java`
> - `network/` 全部（RegionNetwork、RegionEditRequestHandler、ClientboundPayloadDispatch）
> - `network/payload/` 全部 8 个 payload wrapper
> - `platform/MinecraftPermissionAdapter.java`

- [ ] **Step 3: 搬移所有剩余 test 文件**

将 `src/test/java/` 下有 MC 依赖的 12 个测试 + 其余纯 Java 但被测类在平台的测试，用 `git mv` 搬到 `neoforge/mc-1.21.1/src/test/java/`，保持包结构。

```bash
git ls-files 'src/test/**/*.java'
# 全部搬移
```

- [ ] **Step 4: 删除旧的 src/ 目录（如已全部搬完）**

```bash
# 确认 src/ 下无文件
git ls-files 'src/'
# 如果为空，删除目录
rm -rf src/
```

- [ ] **Step 5: 适配 payload wrapper 委托 data record**

对 8 个 payload 文件，改造为 wrapper：
1. `implements CustomPacketPayload`
2. 保留 `TYPE` 和 `STREAM_CODEC`
3. encode/decode 方法改为委托 `*Data.encode(PacketBuffer, data)` / `*Data.decode(PacketBuffer)`
4. 需要创建 `FriendlyByteBufPacketBuffer` 适配器

创建 `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/network/buffer/FriendlyByteBufPacketBuffer.java`：

```java
package com.suian.xaeroregionsrev.network.buffer;

import net.minecraft.network.FriendlyByteBuf;

public class FriendlyByteBufPacketBuffer implements PacketBuffer {
    private final FriendlyByteBuf buf;

    public FriendlyByteBufPacketBuffer(FriendlyByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public void writeUtf(String value, int maxLength) {
        buf.writeUtf(value, maxLength);
    }

    @Override
    public String readUtf(int maxLength) {
        return buf.readUtf(maxLength);
    }

    @Override
    public void writeInt(int value) {
        buf.writeInt(value);
    }

    @Override
    public int readInt() {
        return buf.readInt();
    }

    @Override
    public void writeLong(long value) {
        buf.writeLong(value);
    }

    @Override
    public long readLong() {
        return buf.readLong();
    }

    @Override
    public void writeBoolean(boolean value) {
        buf.writeBoolean(value);
    }

    @Override
    public boolean readBoolean() {
        return buf.readBoolean();
    }

    @Override
    public void writeVarInt(int value) {
        buf.writeVarInt(value);
    }

    @Override
    public int readVarInt() {
        return buf.readVarInt();
    }

    public FriendlyByteBuf buf() {
        return buf;
    }
}
```

- [ ] **Step 6: 改造各 payload wrapper**

以 `CreateRegionRequestPacket` 为例：

```java
package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.buffer.PacketBuffer;
import com.suian.xaeroregionsrev.network.data.CreateRegionRequestData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CreateRegionRequestPacket(CreateRegionRequestData data) implements CustomPacketPayload {
    public static final Type<CreateRegionRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "create_region_request")
    );
    public static final StreamCodec<FriendlyByteBuf, CreateRegionRequestPacket> STREAM_CODEC = StreamCodec.of(
            CreateRegionRequestPacket::encode,
            CreateRegionRequestPacket::decode
    );

    // 向后兼容的构造器（如果外部代码直接构造 packet）
    public CreateRegionRequestPacket(
            long requestId,
            String name,
            ArgbColor fillColor,
            String label,
            ArgbColor labelColor,
            List<RegionPoint> points
    ) {
        this(new CreateRegionRequestData(requestId, name, fillColor, label, labelColor, points));
    }

    // 委托方法
    public long requestId() { return data.requestId(); }
    public String name() { return data.name(); }
    // ... 其他委托方法

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(CreateRegionRequestPacket packet, FriendlyByteBuf buffer) {
        CreateRegionRequestData.encode(new FriendlyByteBufPacketBuffer(buffer), packet.data);
    }

    public static CreateRegionRequestPacket decode(FriendlyByteBuf buffer) {
        CreateRegionRequestData data = CreateRegionRequestData.decode(new FriendlyByteBufPacketBuffer(buffer));
        return new CreateRegionRequestPacket(data);
    }
}
```

对其他 7 个 payload 重复同样模式。

- [ ] **Step 7: 创建 NeoForge 端 NBT 适配器**

`neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/region/nbt/CompoundTagNbtFactory.java`：

```java
package com.suian.xaeroregionsrev.region.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class CompoundTagNbtFactory implements NbtFactory {
    @Override
    public NbtCompound createCompound() {
        return new CompoundTagNbtCompound(new CompoundTag());
    }

    @Override
    public NbtList createList() {
        return new ListTagNbtList(new ListTag());
    }
}
```

（同时创建 `CompoundTagNbtCompound` 和 `ListTagNbtList` 适配器实现，逐方法委托 CompoundTag/ListTag）

- [ ] **Step 8: 改造 RegionSavedData 使用 NBT 适配器**

`RegionSavedData` 调用 `RegionNbtCodec.writeRegion`/`readRegion` 的地方改为传入 `CompoundTagNbtFactory` 和 `CompoundTagNbtCompound` 包装。

- [ ] **Step 9: 创建 NeoForge 端服务端/客户端适配器**

- `NeoForgeServerContext` 实现 `ServerContext`
- `RegionSavedDataStore` 实现 `RegionStore`（委托给 RegionSavedData）
- NeoForge 端 `EditResultHandler` 实现（检查 `Minecraft.getInstance().screen instanceof RegionStyleEditScreen`）

- [ ] **Step 10: 更新 RegionEditResultPacket 和 ClientRegionEditResultHandler 的调用链**

`ClientRegionEditResultHandler` 已在 common 中改为通过 `EditResultHandler` 回调。在 NeoForge 端注册 `EditResultHandler` 实现（在 `XaeroRegionsClient` 或 mod 入口初始化时调用 `ClientRegionEditResultHandler.setEditResultHandler(...)`）。

- [ ] **Step 11: 编译 NeoForge 子项目**

Run: `./gradlew :neoforge:mc-1.21.1:compileJava`
Expected: BUILD SUCCESSFUL

如有编译错误，逐个修复（主要是 import 调整、data record 的委托方法、适配器集成）。

- [ ] **Step 12: 运行 NeoForge 子项目测试**

Run: `./gradlew :neoforge:mc-1.21.1:test`
Expected: 全绿（或修复因接口改造导致的测试失败）

- [ ] **Step 13: 构建验证**

Run: `./gradlew :neoforge:mc-1.21.1:build`
Expected: BUILD SUCCESSFUL，jar 文件名 `Xaero-Map-Regions-Rev-0.1.5+neoforge-1.21.1.jar`

- [ ] **Step 14: 烟测（手动）**

Run: `./gradlew :neoforge:mc-1.21.1:runClient`
验证：游戏能启动、进入世界、创建区域、编辑区域、删除区域、颜色历史同步。

- [ ] **Step 15: Commit**

```bash
git add -A
git commit -m "refactor: 完成 NeoForge 子项目迁移

- 搬移全部平台源码和测试到 neoforge/mc-1.21.1/
- 8 个 payload 改造为 wrapper，委托 common 的 data record
- 创建 FriendlyByteBufPacketBuffer、CompoundTagNbtFactory 适配器
- RegionSavedData 通过 NBT 适配器调用 RegionNbtCodec
- 创建 NeoForgeServerContext、RegionSavedDataStore、EditResultHandler 实现
- NeoForge 子项目编译、测试、构建全通过"
```

---

### Task 13: 搬入阶段 1 延后的测试

**Files:**
- Move: `src/test/` 下被测类已全部就位的测试（现已搬到 neoforge/mc-1.21.1/src/test/，需分类哪些应回到 common）

> 阶段 2 完成后，被测类已全部就位（common 或 neoforge）。现在把那些被测类在 common 中、但测试还在 neoforge 子项目的测试搬回 common。

**Interfaces:**
- Consumes: 阶段 2 完成的所有 common 类
- Produces: common 的完整测试集

- [ ] **Step 1: 识别哪些测试的被测类在 common 中**

以下测试的被测类已在 common，应从 neoforge 子项目搬回 common：
- `RegionNbtCodecTest` — 被测类 RegionNbtCodec 在 common（但测试本身可能依赖 CompoundTag，需检查）
- `RegionEditRequestHandlerTest` — 被测类 RegionEditRequestHandler 在 neoforge（留 neoforge）

> 需要逐个检查测试文件的 import，确认被测类在 common 还是 platform。

- [ ] **Step 2: 搬回可回 common 的测试**

仅搬入**不依赖 MC API 且被测类在 common** 的测试。

- [ ] **Step 3: 验证 common 和 neoforge 测试**

Run: `./gradlew :common:test :neoforge:mc-1.21.1:test`
Expected: 全绿

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test: 回搬被测类在 common 的测试

阶段 2 完成后部分测试的被测类已在 common，从 neoforge 子项目
搬回 common 模块。依赖 MC API 的测试留在 neoforge 子项目。"
```

---

### Task 14: 阶段 2 验证检查点

- [ ] **Step 1: 验证 common 测试**

Run: `./gradlew :common:test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 验证 NeoForge 子项目完整构建**

Run: `./gradlew :neoforge:mc-1.21.1:clean :neoforge:mc-1.21.1:test :neoforge:mc-1.21.1:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 烟测验证（手动）**

Run: `./gradlew :neoforge:mc-1.21.1:runClient`
Expected: 游戏正常启动，区域功能完整可用

- [ ] **Step 4: Commit（检查点标记）**

如无变更跳过。如有修复则提交。

---

## 阶段 3：Forge 子项目迁移

**目标**：`./gradlew :forge:mc-1.20.1:runClient` 能启动游戏，Forge 功能正常。

### Task 15: 创建 Forge 子项目构建配置

**Files:**
- Modify: `forge/mc-1.20.1/build.gradle`（替换占位内容）
- Create: `forge/mc-1.20.1/gradle.properties`
- Create: `forge/mc-1.20.1/src/main/resources/META-INF/mods.toml`（从 forge/1.20.1 分支获取）
- Create: `forge/mc-1.20.1/src/main/resources/pack.mcmeta`（pack_format=15）

**Interfaces:**
- Consumes: common 子项目
- Produces: 可编译的 Forge 子项目

- [ ] **Step 1: 创建 forge/mc-1.20.1/gradle.properties**

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

- [ ] **Step 2: 重写 forge/mc-1.20.1/build.gradle**

```groovy
plugins {
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.minecraftforge.gradle' version '[6.0.16,6.2)'
}

apply from: "$rootDir/buildSrc/src/main/groovy/region-platform.gradle"

version = "${mod_version}+${artifact_loader}-${minecraft_version}"
group = mod_group_id

base {
    archivesName = archives_base_name
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings channel: 'official', version: minecraft_version

    runs {
        configureEach {
            workingDirectory project.file('run')
            property 'forge.logging.markers', 'REGISTRIES'
            property 'forge.logging.console.level', 'debug'
            property 'mixin.env.remapRefMap', 'true'
            property 'mixin.env.refMapRemappingFile', "${buildDir}/createSrgToMcp/output.srg"
            mods {
                "${mod_id}" {
                    source project(':common').sourceSets.main
                    source sourceSets.main
                }
            }
        }

        client {
            property 'forge.enabledGameTestNamespaces', mod_id
        }

        server {
            property 'forge.enabledGameTestNamespaces', mod_id
            args '--nogui'
        }

        gameTestServer {
            property 'forge.enabledGameTestNamespaces', mod_id
        }

        data {
            workingDirectory project.file('run-data')
            args '--mod', mod_id, '--all',
                    '--output', file('src/generated/resources/'),
                    '--existing', file('src/main/resources/')
        }
    }
}

sourceSets.main.resources { srcDir 'src/generated/resources' }

configurations {
    clientRuntimeMods
}

dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"
    implementation project(':common')

    clientRuntimeMods fg.deobf("maven.modrinth:xaeros-world-map:${artifact_loader}-${minecraft_version}-${xaero_world_map_runtime_version}")
    clientRuntimeMods fg.deobf("curse.maven:imblocker-483760:${imblocker_runtime_file_id}")

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test', Test) {
    useJUnitPlatform()
}

tasks.named('clean', Delete).configure {
    delete 'bin', 'logs', '.tmp', 'run/logs', 'run-data/logs'
}

tasks.matching { it.name == 'runClient' }.configureEach {
    dependsOn 'createSrgToMcp'
    classpath += configurations.clientRuntimeMods
}

tasks.named('processResources', ProcessResources).configure {
    from(project(':common').sourceSets.main.resources)

    var replaceProperties = [
            minecraft_version      : minecraft_version,
            minecraft_version_range: minecraft_version_range,
            forge_version          : forge_version,
            forge_version_range    : forge_version_range,
            loader_version_range   : loader_version_range,
            xaero_world_map_version_range: xaero_world_map_version_range,
            mod_id                 : mod_id,
            mod_name               : mod_name,
            mod_license            : mod_license,
            mod_version            : mod_version,
            mod_authors            : mod_authors,
            mod_description        : mod_description
    ]
    inputs.properties replaceProperties

    filesMatching(['META-INF/mods.toml', 'pack.mcmeta']) {
        expand replaceProperties + [project: project]
    }
}

tasks.named('jar', Jar).configure {
    finalizedBy 'reobfJar'
}
```

- [ ] **Step 3: 创建 Forge 特有资源**

从 `forge/1.20.1` 分支获取 mods.toml：
```bash
git show forge/1.20.1:src/main/resources/META-INF/mods.toml > forge/mc-1.20.1/src/main/resources/META-INF/mods.toml
```

创建 pack.mcmeta（pack_format=15）：
```json
{
  "pack": {
    "description": "${mod_name} resources",
    "pack_format": 15
  }
}
```

- [ ] **Step 4: 验证 Forge 子项目配置可解析**

Run: `./gradlew :forge:mc-1.20.1:tasks`
Expected: 列出 runClient、build 等任务

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: 创建 Forge 子项目构建配置

- forge/mc-1.20.1/build.gradle：ForgeGradle + region-platform +
  common sourceSet 纳入 + fg.deobf 运行期模组 + reobfJar
- forge/mc-1.20.1/gradle.properties：MC 1.20.1 专属属性
- 创建 mods.toml 和 pack.mcmeta(pack_format=15)"
```

---

### Task 16: 从 forge/1.20.1 分支提取 Forge 平台源码

**Files:**
- Create: 从 `forge/1.20.1` 分支提取平台文件，适配 common 接口

> Forge 平台源码不从 master 分支改造，而是从 `forge/1.20.1` 分支提取原始文件，然后适配 common 接口。这样保留 Forge 1.20.1 的原生 API 调用。

- [ ] **Step 1: 创建 Forge 子项目源码目录**

```bash
mkdir -p forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev
mkdir -p forge/mc-1.20.1/src/test/java/com/suian/xaeroregionsrev
```

- [ ] **Step 2: 从 forge/1.20.1 分支提取平台文件**

将 forge/1.20.1 分支中**不属于 common 的文件**提取到 forge 子项目：

```bash
# forge 平台文件列表（从 forge/1.20.1 分支提取）
for f in $(git ls-tree -r --name-only forge/1.20.1 -- 'src/main/java/' | grep '\.java$' | sed 's|src/main/java/com/suian/xaeroregionsrev/||'); do
    # 检查是否已在 common 中（如果在 common 中则跳过）
    if [ ! -f "common/src/main/java/com/suian/xaeroregionsrev/$f" ]; then
        git show "forge/1.20.1:src/main/java/com/suian/xaeroregionsrev/$f" > "forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/$f"
    fi
done
```

> 需要创建目录结构后再提取。

- [ ] **Step 3: 创建 Forge 端适配器**

与 Task 12 的 NeoForge 端类似，创建 Forge 端的：
- `FriendlyByteBufPacketBuffer`（Forge 1.20.1 版本，FriendlyByteBuf 签名可能略有不同）
- `CompoundTagNbtFactory` + `CompoundTagNbtCompound` + `ListTagNbtList`（Forge 1.20.1 NBT API）
- `ForgeServerContext` + `RegionSavedDataStore`
- Forge 端 `EditResultHandler` 实现
- 8 个 payload wrapper（SimpleChannel 注册方式）

- [ ] **Step 4: 改造 Forge 平台文件使用 common 接口**

- payload wrapper 改为委托 common 的 data record
- RegionSavedData 改用 NBT 适配器（两端 FriendlyByteBuf/CompoundTag 签名一致，适配器可共用接口定义）
- RegionService 调用改为通过 ServerContext/RegionStore
- ClientRegionEditResultHandler 调用改为通过 EditResultHandler（forge 的 RegionNetwork consumer 拆字段调用 `handle(requestId, success, closeScreen, message)`）
- **补齐 forge 端缺失功能**：RegionSavedData.load 的 colorHistory 循环改为 `Math.min(colorList.size(), ColorPaletteLimits.MAX_COLORS)`，与 master 对齐
- **MapProjectionAdapter 行为分叉**：forge 端从 forge/1.20.1 分支原样提取，保留无条件校准行为（无 calibrationEnabled 开关），不强行对齐 master 的开关功能。这是已知差异，不在本次重构统一

- [ ] **Step 5: 编译 Forge 子项目**

Run: `./gradlew :forge:mc-1.20.1:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 运行 Forge 子项目测试**

Run: `./gradlew :forge:mc-1.20.1:test`
Expected: 全绿

- [ ] **Step 7: 构建验证**

Run: `./gradlew :forge:mc-1.20.1:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: 烟测（手动）**

Run: `./gradlew :forge:mc-1.20.1:runClient`
验证：游戏能启动、区域功能正常。

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor: 完成 Forge 子项目迁移

- 从 forge/1.20.1 分支提取平台源码到 forge/mc-1.20.1/
- 创建 Forge 端适配器（PacketBuffer、NbtFactory、ServerContext、EditResultHandler）
- payload wrapper 委托 common data record，注册到 SimpleChannel
- RegionSavedData 通过 NBT 适配器调用 RegionNbtCodec
- Forge 子项目编译、测试、构建全通过"
```

---

### Task 17: 阶段 3 验证检查点

- [ ] **Step 1: 验证两端构建**

Run: `./gradlew :forge:mc-1.20.1:clean :forge:mc-1.20.1:test :forge:mc-1.20.1:build`
Expected: BUILD SUCCESSFUL

Run: `./gradlew buildAll`
Expected: 两端都 BUILD SUCCESSFUL

- [ ] **Step 2: 烟测验证（手动）**

两端分别 runClient，验证功能完整。

- [ ] **Step 3: Commit（检查点标记）**

如无变更跳过。

---

## 阶段 4：清理与文档

### Task 18: 清理旧目录和分支

**Files:**
- Delete: `src/`（如还有残留）
- Update: `.gitignore`

- [ ] **Step 1: 确认 src/ 目录已清空**

Run: `git ls-files 'src/' | head`
Expected: 空输出

如有残留文件，确认是否应删除或搬移。

- [ ] **Step 2: 更新 .gitignore**

在根 `.gitignore` 中确保各平台运行目录被忽略：

追加（如尚未包含）：
```
# Platform run directories
neoforge/mc-1.21.1/run/
neoforge/mc-1.21.1/run-client/
neoforge/mc-1.21.1/run-server/
neoforge/mc-1.21.1/run-data/
forge/mc-1.20.1/run/
forge/mc-1.20.1/run-data/
```

保留已有的 `.gradle/`、`build/`、`bin/`、`out/`、`logs/`、`.idea/` 等。

- [ ] **Step 3: Commit**

```bash
git add .gitignore
git rm -r src/  # 如有残留
git commit -m "chore: 清理旧 src 目录并更新 .gitignore

- 删除已全部迁移的旧 src/ 目录
- .gitignore 添加各平台运行目录规则"
```

---

### Task 19: 更新项目文档

**Files:**
- Modify: `AGENTS.md`（文件树、技术约定、构建命令）
- Modify: `CHANGELOG.md`
- Modify: `README.md`

- [ ] **Step 1: 更新 AGENTS.md 文件树**

将文件树更新为多子项目结构，反映 common / neoforge / forge 的目录布局和各文件职责。

- [ ] **Step 2: 更新 AGENTS.md 常用命令**

```markdown
## 常用命令

- `./gradlew :common:test`：运行公共模块纯 Java 测试。
- `./gradlew :neoforge:mc-1.21.1:test`：运行 NeoForge 子项目测试。
- `./gradlew :neoforge:mc-1.21.1:runClient`：启动 NeoForge 开发客户端。
- `./gradlew :forge:mc-1.20.1:test`：运行 Forge 子项目测试。
- `./gradlew :forge:mc-1.20.1:runClient`：启动 Forge 开发客户端。
- `./gradlew buildAll`：构建所有平台。
- `./gradlew :neoforge:mc-1.21.1:bump -Ppatch`：递增版本号。
```

- [ ] **Step 3: 更新 CHANGELOG.md**

添加 `## [Unreleased]` 段落描述重构。

- [ ] **Step 4: 更新 README.md**

更新项目结构描述、构建说明。

- [ ] **Step 5: Commit**

```bash
git add AGENTS.md CHANGELOG.md README.md
git commit -m "docs: 更新项目文档反映多子项目结构

- AGENTS.md：更新文件树、技术约定、常用命令
- CHANGELOG.md：添加重构变更记录
- README.md：更新项目结构和构建说明"
```

---

### Task 20: 最终验证

- [ ] **Step 1: 完整构建所有平台**

Run: `./gradlew clean buildAll`
Expected: 两端都 BUILD SUCCESSFUL

- [ ] **Step 2: 完整测试**

Run: `./gradlew :common:test :neoforge:mc-1.21.1:test :forge:mc-1.20.1:test`
Expected: 全绿

- [ ] **Step 3: 两端烟测（手动）**

分别 `runClient` 验证功能完整。

- [ ] **Step 4: 最终 Commit（如有变更）**

```bash
git add -A
git commit -m "chore: 多子项目重构最终验证通过"
```

- [ ] **Step 5: 等待人类验收**

提示用户验收重构分支 `refactor/multi-project-structure` 的成果。验收通过后再合并回 master。
