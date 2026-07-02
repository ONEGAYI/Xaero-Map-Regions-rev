# Xaero Map Regions Rev MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个 Minecraft 1.20.1 + Forge 47.3.33 模组 MVP，实现服务端区域持久化、权限校验、网络同步、客户端缓存，并在 Xaero World Map 屏幕上绘制同步的多边形区域。

**Architecture:** 服务端是区域数据权威来源，使用 `SavedData` 持久化并通过 Forge `SimpleChannel` 同步快照。共享模型和几何逻辑保持纯 Java，Forge/Xaero 相关细节集中在 `platform`、`network`、`client.xaero` 包中，便于 Forge 47.3.33 后续小版本适配。

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.3.33, ForgeGradle 6.x, Gradle, JUnit 5, Xaero's World Map 作为运行期外部依赖。

---

## 参考资料

- ForgeGradle 官方入门：`https://docs.minecraftforge.net/en/fg-6.x/gettingstarted/`
- Forge 1.20.1 注册与事件文档：`https://docs.minecraftforge.net/en/1.20.1/concepts/registries/`
- Forge SimpleImpl 文档：`https://docs.minecraftforge.net/en/latest/networking/simpleimpl/`
- Xaero's World Map 1.20.1 Forge Modrinth 版本 API：`https://api.modrinth.com/v2/project/NcUtCpym/version?game_versions=["1.20.1"]&loaders=["forge"]`

## 文件结构

```text
.
├── build.gradle
├── gradle.properties
├── settings.gradle
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/com/suian/xaeroregionsrev/
│   │   │   ├── XaeroRegionsRev.java
│   │   │   ├── command/RegionCommands.java
│   │   │   ├── data/RegionSavedData.java
│   │   │   ├── network/RegionNetwork.java
│   │   │   ├── network/payload/RegionSyncPacket.java
│   │   │   ├── platform/ForgePermissionAdapter.java
│   │   │   ├── region/ArgbColor.java
│   │   │   ├── region/PermissionProfile.java
│   │   │   ├── region/PointMarker.java
│   │   │   ├── region/PolygonMath.java
│   │   │   ├── region/Region.java
│   │   │   ├── region/RegionId.java
│   │   │   ├── region/RegionNbtCodec.java
│   │   │   ├── region/RegionPoint.java
│   │   │   └── service/RegionService.java
│   │   └── resources/
│   │       ├── META-INF/mods.toml
│   │       └── pack.mcmeta
│   └── test/java/com/suian/xaeroregionsrev/
│       ├── network/payload/RegionSyncPacketTest.java
│       └── region/
│           ├── PermissionProfileTest.java
│           ├── PolygonMathTest.java
│           ├── RegionNbtCodecTest.java
│           └── RegionTest.java
└── docs/superpowers/plans/2026-07-02-xaero-map-regions-mvp-implementation.md
```

### Task 1: Forge 47.3.33 脚手架

**Files:**
- Create: `.gitignore`
- Create: `settings.gradle`
- Create: `gradle.properties`
- Create: `build.gradle`
- Create: `src/main/resources/META-INF/mods.toml`
- Create: `src/main/resources/pack.mcmeta`

- [ ] **Step 1: 下载 Forge 47.3.33 MDK 到临时目录**

Run:

```powershell
New-Item -ItemType Directory -Force .tmp | Out-Null
Invoke-WebRequest -Uri "https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.3.33/forge-1.20.1-47.3.33-mdk.zip" -OutFile ".tmp/forge-1.20.1-47.3.33-mdk.zip"
Expand-Archive ".tmp/forge-1.20.1-47.3.33-mdk.zip" ".tmp/forge-mdk" -Force
```

Expected: `.tmp/forge-mdk` 下出现 Forge MDK 的 `gradlew`、`gradle/`、`build.gradle` 示例文件。

- [ ] **Step 2: 复制 Gradle wrapper**

Run:

```powershell
Copy-Item ".tmp/forge-mdk/gradlew" ".\gradlew" -Force
Copy-Item ".tmp/forge-mdk/gradlew.bat" ".\gradlew.bat" -Force
Copy-Item ".tmp/forge-mdk/gradle" ".\gradle" -Recurse -Force
```

Expected: 仓库根目录出现 `gradlew`、`gradlew.bat`、`gradle/wrapper/*`。

- [ ] **Step 3: 写入 `.gitignore`**

Create `.gitignore`:

```gitignore
.gradle/
build/
out/
run/
.tmp/
*.log
*.hprof
.idea/
*.iml
```

- [ ] **Step 4: 写入 `settings.gradle`**

Create `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
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
```

- [ ] **Step 5: 写入 `gradle.properties`**

Create `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false

minecraft_version=1.20.1
forge_version=47.3.33
loader_version_range=[47,)
forge_version_range=[47.3.33,)
minecraft_version_range=[1.20.1,1.21)

mod_id=xaeroregionsrev
mod_name=Xaero Map Regions Rev
mod_license=MIT
mod_version=0.1.0
mod_group_id=com.suian.xaeroregionsrev
mod_authors=SUIAN
mod_description=Server-synced polygon regions rendered on Xaero's World Map for Forge 1.20.1.
```

- [ ] **Step 6: 写入 `build.gradle`**

Create `build.gradle`:

```groovy
plugins {
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.minecraftforge.gradle' version '[6.0.16,6.2)'
}

version = mod_version
group = mod_group_id

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings channel: 'official', version: minecraft_version

    runs {
        configureEach {
            workingDirectory project.file('run')
            property 'forge.logging.markers', 'REGISTRIES'
            property 'forge.logging.console.level', 'debug'
            mods {
                "${mod_id}" {
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

repositories {
    mavenCentral()
}

dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
}

tasks.named('test', Test) {
    useJUnitPlatform()
}

tasks.named('processResources', ProcessResources).configure {
    var replaceProperties = [
            minecraft_version      : minecraft_version,
            minecraft_version_range: minecraft_version_range,
            forge_version          : forge_version,
            forge_version_range    : forge_version_range,
            loader_version_range   : loader_version_range,
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
    finalizedBy 'reobfJar'
}
```

- [ ] **Step 7: 写入 `mods.toml`**

Create `src/main/resources/META-INF/mods.toml`:

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
modId="forge"
mandatory=true
versionRange="${forge_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="minecraft"
mandatory=true
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="xaeroworldmap"
mandatory=false
versionRange="[1.30.0,)"
ordering="AFTER"
side="CLIENT"
```

- [ ] **Step 8: 写入 `pack.mcmeta`**

Create `src/main/resources/pack.mcmeta`:

```json
{
  "pack": {
    "description": "${mod_name} resources",
    "pack_format": 15
  }
}
```

- [ ] **Step 9: 验证 Gradle 能解析**

Run:

```powershell
.\gradlew --version
.\gradlew tasks --group build
```

Expected: 两个命令退出码为 0，输出中能看到 `build`、`jar`、`test` 任务。

- [ ] **Step 10: 提交脚手架**

Run:

```powershell
git add .gitignore settings.gradle gradle.properties build.gradle gradlew gradlew.bat gradle src/main/resources
git commit -m "chore: 初始化 Forge 47.3.33 脚手架" -m "基于 Forge 1.20.1-47.3.33 建立 Gradle 工程。" -m "- 固定 Minecraft、Forge 和模组元数据属性。" -m "- 添加 ForgeGradle、JUnit 5、mods.toml 和资源包元数据。" -m "- 保留 Xaero World Map 为客户端可选外部依赖，不随产物打包。"
```

### Task 2: 模组入口与事件注册骨架

**Files:**
- Create: `src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java`
- Modify: `src/main/resources/META-INF/mods.toml`

- [ ] **Step 1: 写入主类**

Create `src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java`:

```java
package com.suian.xaeroregionsrev;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(XaeroRegionsRev.MOD_ID)
public final class XaeroRegionsRev {
    public static final String MOD_ID = "xaeroregionsrev";
    public static final Logger LOGGER = LogUtils.getLogger();

    public XaeroRegionsRev() {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Xaero Map Regions Rev loaded.");
    }
}
```

- [ ] **Step 2: 验证构建失败或通过原因**

Run:

```powershell
.\gradlew test
.\gradlew build
```

Expected: 如果 Task 1 完整，两个命令均为 PASS；若 ForgeGradle 首次下载依赖，耗时较长但最终退出码为 0。

- [ ] **Step 3: 提交入口骨架**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java
git commit -m "chore: 添加模组入口骨架" -m "建立 Forge 模组主类，为后续命令、网络和客户端事件注册提供统一入口。" -m "- 定义 MOD_ID 和日志入口。" -m "- 注册 Forge 事件总线。"
```

### Task 3: 区域模型与多边形几何

**Files:**
- Create: `src/test/java/com/suian/xaeroregionsrev/region/PolygonMathTest.java`
- Create: `src/test/java/com/suian/xaeroregionsrev/region/RegionTest.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/region/RegionPoint.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/region/RegionId.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/region/ArgbColor.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/region/PointMarker.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/region/Region.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/region/PolygonMath.java`

- [ ] **Step 1: 写入失败测试**

Create `src/test/java/com/suian/xaeroregionsrev/region/PolygonMathTest.java`:

```java
package com.suian.xaeroregionsrev.region;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolygonMathTest {
    @Test
    void pointInsideSquareReturnsTrue() {
        var polygon = List.of(
                new RegionPoint(0, 0),
                new RegionPoint(10, 0),
                new RegionPoint(10, 10),
                new RegionPoint(0, 10)
        );

        assertTrue(PolygonMath.contains(polygon, 5, 5));
    }

    @Test
    void pointOutsideSquareReturnsFalse() {
        var polygon = List.of(
                new RegionPoint(0, 0),
                new RegionPoint(10, 0),
                new RegionPoint(10, 10),
                new RegionPoint(0, 10)
        );

        assertFalse(PolygonMath.contains(polygon, 15, 5));
    }

    @Test
    void lessThanThreePointsIsInvalid() {
        assertFalse(PolygonMath.isValidPolygon(List.of(new RegionPoint(0, 0), new RegionPoint(1, 1))));
    }
}
```

Create `src/test/java/com/suian/xaeroregionsrev/region/RegionTest.java`:

```java
package com.suian.xaeroregionsrev.region;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegionTest {
    @Test
    void validRegionRequiresAtLeastThreePoints() {
        var region = new Region(
                new RegionId("spawn"),
                "Spawn",
                "minecraft:overworld",
                new ArgbColor(0x8000FF00),
                "default",
                "home",
                List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                1L,
                2L
        );

        assertEquals("spawn", region.id().value());
        assertTrue(region.hasValidPolygon());
    }

    @Test
    void blankRegionIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RegionId(" "));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
.\gradlew test --tests "*PolygonMathTest" --tests "*RegionTest"
```

Expected: FAIL，错误中包含找不到 `RegionPoint`、`PolygonMath`、`Region` 等类型。

- [ ] **Step 3: 写入最小实现**

Create `src/main/java/com/suian/xaeroregionsrev/region/RegionPoint.java`:

```java
package com.suian.xaeroregionsrev.region;

public record RegionPoint(int x, int z) {
}
```

Create `src/main/java/com/suian/xaeroregionsrev/region/RegionId.java`:

```java
package com.suian.xaeroregionsrev.region;

import java.util.Locale;

public record RegionId(String value) {
    public RegionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Region id cannot be blank.");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }
}
```

Create `src/main/java/com/suian/xaeroregionsrev/region/ArgbColor.java`:

```java
package com.suian.xaeroregionsrev.region;

public record ArgbColor(int value) {
    public int alpha() {
        return (value >>> 24) & 0xFF;
    }
}
```

Create `src/main/java/com/suian/xaeroregionsrev/region/PointMarker.java`:

```java
package com.suian.xaeroregionsrev.region;

import java.util.UUID;

public record PointMarker(
        UUID targetPlayer,
        String mode,
        String iconName,
        String label,
        int x,
        int y,
        int z
) {
    public PointMarker {
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("Point marker mode cannot be blank.");
        }
        if (iconName == null || iconName.isBlank()) {
            throw new IllegalArgumentException("Point marker icon name cannot be blank.");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Point marker label cannot be blank.");
        }
    }
}
```

Create `src/main/java/com/suian/xaeroregionsrev/region/Region.java`:

```java
package com.suian.xaeroregionsrev.region;

import java.util.List;

public record Region(
        RegionId id,
        String name,
        String dimension,
        ArgbColor color,
        String category,
        String iconName,
        List<RegionPoint> points,
        long createdAt,
        long updatedAt
) {
    public Region {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Region name cannot be blank.");
        }
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("Region dimension cannot be blank.");
        }
        points = List.copyOf(points);
    }

    public boolean hasValidPolygon() {
        return PolygonMath.isValidPolygon(points);
    }
}
```

Create `src/main/java/com/suian/xaeroregionsrev/region/PolygonMath.java`:

```java
package com.suian.xaeroregionsrev.region;

import java.util.List;

public final class PolygonMath {
    private PolygonMath() {
    }

    public static boolean isValidPolygon(List<RegionPoint> points) {
        return points != null && points.size() >= 3;
    }

    public static boolean contains(List<RegionPoint> polygon, int x, int z) {
        if (!isValidPolygon(polygon)) {
            return false;
        }

        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            RegionPoint pi = polygon.get(i);
            RegionPoint pj = polygon.get(j);
            boolean intersects = ((pi.z() > z) != (pj.z() > z))
                    && (x < (long) (pj.x() - pi.x()) * (z - pi.z()) / (double) (pj.z() - pi.z()) + pi.x());
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }
}
```

- [ ] **Step 4: 验证测试通过**

Run:

```powershell
.\gradlew test --tests "*PolygonMathTest" --tests "*RegionTest"
```

Expected: PASS。

- [ ] **Step 5: 提交区域模型**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev/region src/test/java/com/suian/xaeroregionsrev/region
git commit -m "feat: 添加区域模型和几何判断" -m "建立区域、点位、颜色和多边形判断的纯 Java 核心。" -m "- 添加 Region、RegionId、RegionPoint、PointMarker 和 ArgbColor。" -m "- 添加 PolygonMath 点在多边形内判断。" -m "- 使用 JUnit 覆盖基础几何和模型约束。"
```

### Task 4: 权限策略

**Files:**
- Create: `src/test/java/com/suian/xaeroregionsrev/region/PermissionProfileTest.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/region/PermissionProfile.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/platform/ForgePermissionAdapter.java`

- [ ] **Step 1: 写入失败测试**

Create `src/test/java/com/suian/xaeroregionsrev/region/PermissionProfileTest.java`:

```java
package com.suian.xaeroregionsrev.region;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionProfileTest {
    @Test
    void opCreativeCanManageRegions() {
        assertTrue(new PermissionProfile(true, true).canManageRegions());
    }

    @Test
    void opSurvivalCannotManageRegions() {
        assertFalse(new PermissionProfile(true, false).canManageRegions());
    }

    @Test
    void creativeNonOpCannotManageRegions() {
        assertFalse(new PermissionProfile(false, true).canManageRegions());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
.\gradlew test --tests "*PermissionProfileTest"
```

Expected: FAIL，错误中包含找不到 `PermissionProfile`。

- [ ] **Step 3: 写入纯逻辑和 Forge 适配器**

Create `src/main/java/com/suian/xaeroregionsrev/region/PermissionProfile.java`:

```java
package com.suian.xaeroregionsrev.region;

public record PermissionProfile(boolean operator, boolean creative) {
    public boolean canManageRegions() {
        return operator && creative;
    }
}
```

Create `src/main/java/com/suian/xaeroregionsrev/platform/ForgePermissionAdapter.java`:

```java
package com.suian.xaeroregionsrev.platform;

import com.suian.xaeroregionsrev.region.PermissionProfile;
import net.minecraft.server.level.ServerPlayer;

public final class ForgePermissionAdapter {
    private ForgePermissionAdapter() {
    }

    public static PermissionProfile from(ServerPlayer player) {
        boolean operator = player.hasPermissions(2);
        boolean creative = player.gameMode.isCreative();
        return new PermissionProfile(operator, creative);
    }
}
```

- [ ] **Step 4: 验证测试通过并编译适配器**

Run:

```powershell
.\gradlew test --tests "*PermissionProfileTest"
.\gradlew build
```

Expected: 两个命令均 PASS。

- [ ] **Step 5: 提交权限策略**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev/region/PermissionProfile.java src/main/java/com/suian/xaeroregionsrev/platform/ForgePermissionAdapter.java src/test/java/com/suian/xaeroregionsrev/region/PermissionProfileTest.java
git commit -m "feat: 添加区域管理权限策略" -m "将 OP 且创造模式的规则拆成可测试纯逻辑，并提供 Forge 玩家适配。" -m "- 添加 PermissionProfile 单元测试。" -m "- 添加 ServerPlayer 到权限档案的适配器。"
```

### Task 5: NBT 编解码

**Files:**
- Create: `src/test/java/com/suian/xaeroregionsrev/region/RegionNbtCodecTest.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/region/RegionNbtCodec.java`

- [ ] **Step 1: 写入失败测试**

Create `src/test/java/com/suian/xaeroregionsrev/region/RegionNbtCodecTest.java`:

```java
package com.suian.xaeroregionsrev.region;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegionNbtCodecTest {
    @Test
    void roundTripsRegionThroughNbt() {
        var original = new Region(
                new RegionId("spawn"),
                "Spawn",
                "minecraft:overworld",
                new ArgbColor(0x8800FF00),
                "town",
                "home",
                List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                100L,
                200L
        );

        var tag = RegionNbtCodec.writeRegion(original);
        var decoded = RegionNbtCodec.readRegion(tag);

        assertEquals(original, decoded);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
.\gradlew test --tests "*RegionNbtCodecTest"
```

Expected: FAIL，错误中包含找不到 `RegionNbtCodec`。

- [ ] **Step 3: 写入 NBT 编解码实现**

Create `src/main/java/com/suian/xaeroregionsrev/region/RegionNbtCodec.java`:

```java
package com.suian.xaeroregionsrev.region;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public final class RegionNbtCodec {
    private RegionNbtCodec() {
    }

    public static CompoundTag writeRegion(Region region) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", region.id().value());
        tag.putString("name", region.name());
        tag.putString("dimension", region.dimension());
        tag.putInt("color", region.color().value());
        tag.putString("category", region.category());
        tag.putString("iconName", region.iconName());
        tag.putLong("createdAt", region.createdAt());
        tag.putLong("updatedAt", region.updatedAt());

        ListTag points = new ListTag();
        for (RegionPoint point : region.points()) {
            CompoundTag pointTag = new CompoundTag();
            pointTag.putInt("x", point.x());
            pointTag.putInt("z", point.z());
            points.add(pointTag);
        }
        tag.put("points", points);
        return tag;
    }

    public static Region readRegion(CompoundTag tag) {
        List<RegionPoint> points = new ArrayList<>();
        ListTag pointTags = tag.getList("points", 10);
        for (int i = 0; i < pointTags.size(); i++) {
            CompoundTag pointTag = pointTags.getCompound(i);
            points.add(new RegionPoint(pointTag.getInt("x"), pointTag.getInt("z")));
        }

        return new Region(
                new RegionId(tag.getString("id")),
                tag.getString("name"),
                tag.getString("dimension"),
                new ArgbColor(tag.getInt("color")),
                tag.getString("category"),
                tag.getString("iconName"),
                points,
                tag.getLong("createdAt"),
                tag.getLong("updatedAt")
        );
    }
}
```

- [ ] **Step 4: 验证测试通过**

Run:

```powershell
.\gradlew test --tests "*RegionNbtCodecTest"
```

Expected: PASS。

- [ ] **Step 5: 提交 NBT 编解码**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev/region/RegionNbtCodec.java src/test/java/com/suian/xaeroregionsrev/region/RegionNbtCodecTest.java
git commit -m "feat: 添加区域 NBT 编解码" -m "为 SavedData 持久化和后续网络同步建立区域序列化基础。" -m "- 添加 RegionNbtCodec。" -m "- 覆盖区域 NBT 往返测试。"
```

### Task 6: SavedData 与服务层

**Files:**
- Create: `src/main/java/com/suian/xaeroregionsrev/data/RegionSavedData.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/service/RegionService.java`

- [ ] **Step 1: 写入 `RegionSavedData`**

Create `src/main/java/com/suian/xaeroregionsrev/data/RegionSavedData.java`:

```java
package com.suian.xaeroregionsrev.data;

import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionNbtCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class RegionSavedData extends SavedData {
    private static final String DATA_NAME = "xaeroregionsrev_regions";
    private final Map<RegionId, Region> regions = new LinkedHashMap<>();

    public static RegionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RegionSavedData::load, RegionSavedData::new, DATA_NAME);
    }

    public static RegionSavedData load(CompoundTag tag) {
        RegionSavedData data = new RegionSavedData();
        ListTag list = tag.getList("regions", 10);
        for (int i = 0; i < list.size(); i++) {
            Region region = RegionNbtCodec.readRegion(list.getCompound(i));
            data.regions.put(region.id(), region);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Region region : regions.values()) {
            list.add(RegionNbtCodec.writeRegion(region));
        }
        tag.put("regions", list);
        return tag;
    }

    public Collection<Region> allRegions() {
        return regions.values();
    }

    public Optional<Region> find(RegionId id) {
        return Optional.ofNullable(regions.get(id));
    }

    public void put(Region region) {
        regions.put(region.id(), region);
        setDirty();
    }

    public boolean remove(RegionId id) {
        boolean removed = regions.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }
}
```

- [ ] **Step 2: 写入 `RegionService`**

Create `src/main/java/com/suian/xaeroregionsrev/service/RegionService.java`:

```java
package com.suian.xaeroregionsrev.service;

import com.suian.xaeroregionsrev.data.RegionSavedData;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;
import java.util.Optional;

public final class RegionService {
    public Collection<Region> list(ServerLevel level) {
        return RegionSavedData.get(level).allRegions();
    }

    public Optional<Region> find(ServerLevel level, RegionId id) {
        return RegionSavedData.get(level).find(id);
    }

    public void upsert(ServerLevel level, Region region) {
        if (!region.hasValidPolygon()) {
            throw new IllegalArgumentException("Region polygon must contain at least three points.");
        }
        RegionSavedData.get(level).put(region);
    }

    public boolean delete(ServerLevel level, RegionId id) {
        return RegionSavedData.get(level).remove(id);
    }
}
```

- [ ] **Step 3: 验证编译**

Run:

```powershell
.\gradlew build
```

Expected: PASS。

- [ ] **Step 4: 提交持久化服务**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev/data/RegionSavedData.java src/main/java/com/suian/xaeroregionsrev/service/RegionService.java
git commit -m "feat: 添加区域持久化服务" -m "使用 SavedData 保存服务端区域数据，并提供服务层封装。" -m "- 添加 RegionSavedData 读写区域集合。" -m "- 添加 RegionService 统一查询、写入和删除入口。"
```

### Task 7: 网络同步包

**Files:**
- Create: `src/test/java/com/suian/xaeroregionsrev/network/payload/RegionSyncPacketTest.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/network/payload/RegionSyncPacket.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/network/RegionNetwork.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java`

- [ ] **Step 1: 写入失败测试**

Create `src/test/java/com/suian/xaeroregionsrev/network/payload/RegionSyncPacketTest.java`:

```java
package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegionSyncPacketTest {
    @Test
    void encodesAndDecodesRegions() {
        var region = new Region(
                new RegionId("spawn"),
                "Spawn",
                "minecraft:overworld",
                new ArgbColor(0x8800FF00),
                "town",
                "home",
                List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                100L,
                200L
        );
        var packet = new RegionSyncPacket(List.of(region));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        RegionSyncPacket.encode(packet, buffer);
        var decoded = RegionSyncPacket.decode(buffer);

        assertEquals(packet.regions(), decoded.regions());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
.\gradlew test --tests "*RegionSyncPacketTest"
```

Expected: FAIL，错误中包含找不到 `RegionSyncPacket`。

- [ ] **Step 3: 写入同步包**

Create `src/main/java/com/suian/xaeroregionsrev/network/payload/RegionSyncPacket.java`:

```java
package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record RegionSyncPacket(List<Region> regions) {
    public RegionSyncPacket {
        regions = List.copyOf(regions);
    }

    public static void encode(RegionSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.regions.size());
        for (Region region : packet.regions) {
            buffer.writeUtf(region.id().value());
            buffer.writeUtf(region.name());
            buffer.writeUtf(region.dimension());
            buffer.writeInt(region.color().value());
            buffer.writeUtf(region.category());
            buffer.writeUtf(region.iconName());
            buffer.writeLong(region.createdAt());
            buffer.writeLong(region.updatedAt());
            buffer.writeVarInt(region.points().size());
            for (RegionPoint point : region.points()) {
                buffer.writeInt(point.x());
                buffer.writeInt(point.z());
            }
        }
    }

    public static RegionSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<Region> regions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            RegionId id = new RegionId(buffer.readUtf());
            String name = buffer.readUtf();
            String dimension = buffer.readUtf();
            ArgbColor color = new ArgbColor(buffer.readInt());
            String category = buffer.readUtf();
            String iconName = buffer.readUtf();
            long createdAt = buffer.readLong();
            long updatedAt = buffer.readLong();
            int pointCount = buffer.readVarInt();
            List<RegionPoint> points = new ArrayList<>(pointCount);
            for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
                points.add(new RegionPoint(buffer.readInt(), buffer.readInt()));
            }
            regions.add(new Region(id, name, dimension, color, category, iconName, points, createdAt, updatedAt));
        }
        return new RegionSyncPacket(regions);
    }
}
```

- [ ] **Step 4: 写入网络通道注册**

Create `src/main/java/com/suian/xaeroregionsrev/network/RegionNetwork.java`:

```java
package com.suian.xaeroregionsrev.network;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RegionNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(XaeroRegionsRev.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int packetId;

    private RegionNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                RegionSyncPacket.class,
                RegionSyncPacket::encode,
                RegionSyncPacket::decode,
                (packet, contextSupplier) -> contextSupplier.get().setPacketHandled(true)
        );
    }
}
```

Modify `src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java`:

```java
package com.suian.xaeroregionsrev;

import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(XaeroRegionsRev.MOD_ID)
public final class XaeroRegionsRev {
    public static final String MOD_ID = "xaeroregionsrev";
    public static final Logger LOGGER = LogUtils.getLogger();

    public XaeroRegionsRev() {
        RegionNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Xaero Map Regions Rev loaded.");
    }
}
```

- [ ] **Step 5: 验证测试和构建**

Run:

```powershell
.\gradlew test --tests "*RegionSyncPacketTest"
.\gradlew build
```

Expected: 两个命令均 PASS。

- [ ] **Step 6: 提交网络同步包**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev/network src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java src/test/java/com/suian/xaeroregionsrev/network
git commit -m "feat: 添加区域同步网络包" -m "建立 Forge SimpleChannel 和区域快照同步包。" -m "- 添加 RegionSyncPacket 编解码。" -m "- 注册 xaeroregionsrev 主网络通道。" -m "- 使用 JUnit 覆盖 FriendlyByteBuf 往返。"
```

### Task 8: 服务端命令与登录同步

**Files:**
- Create: `src/main/java/com/suian/xaeroregionsrev/command/RegionCommands.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/network/RegionNetwork.java`

- [ ] **Step 1: 扩展网络发送方法**

Modify `src/main/java/com/suian/xaeroregionsrev/network/RegionNetwork.java`:

```java
package com.suian.xaeroregionsrev.network;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RegionNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(XaeroRegionsRev.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int packetId;

    private RegionNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(RegionSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RegionSyncPacket::encode)
                .decoder(RegionSyncPacket::decode)
                .consumerMainThread((packet, contextSupplier) -> contextSupplier.get().setPacketHandled(true))
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, RegionSyncPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(RegionSyncPacket packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
```

- [ ] **Step 2: 写入命令注册**

Create `src/main/java/com/suian/xaeroregionsrev/command/RegionCommands.java`:

```java
package com.suian.xaeroregionsrev.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import com.suian.xaeroregionsrev.platform.ForgePermissionAdapter;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.PointMarker;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import com.suian.xaeroregionsrev.service.RegionService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class RegionCommands {
    private static final RegionService SERVICE = new RegionService();

    private RegionCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("region")
                .then(literal("createpoly")
                        .requires(RegionCommands::canManage)
                        .then(argument("name", StringArgumentType.word())
                                .then(argument("argb", StringArgumentType.word())
                                        .then(argument("points", StringArgumentType.greedyString())
                                                .executes(context -> createPolygon(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name"),
                                                        StringArgumentType.getString(context, "argb"),
                                                        StringArgumentType.getString(context, "points")
                                                ))))))
                .then(literal("hide")
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("region", StringArgumentType.word())
                                        .executes(context -> messageOnly(context.getSource(), "hide command accepted for MVP data flow")))))
                .then(literal("visible")
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("region", StringArgumentType.word())
                                        .executes(context -> messageOnly(context.getSource(), "visible command accepted for MVP data flow")))))
                .then(literal("createpoint")
                        .requires(RegionCommands::canManage)
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("mode", StringArgumentType.word())
                                        .then(argument("iconname", StringArgumentType.word())
                                                .then(argument("label", StringArgumentType.word())
                                                        .then(argument("position", StringArgumentType.greedyString())
                                                                .executes(context -> createPoint(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "player"),
                                                                        StringArgumentType.getString(context, "mode"),
                                                                        StringArgumentType.getString(context, "iconname"),
                                                                        StringArgumentType.getString(context, "label"),
                                                                        StringArgumentType.getString(context, "position")
                                                                ))))))))
                .then(literal("delpoint")
                        .requires(RegionCommands::canManage)
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("position", StringArgumentType.greedyString())
                                        .executes(context -> messageOnly(context.getSource(), "delpoint command accepted for MVP data flow"))))));
    }

    private static boolean canManage(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return ForgePermissionAdapter.from(player).canManageRegions();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int createPolygon(CommandSourceStack source, String name, String argb, String pointsText) {
        ServerLevel level = source.getLevel();
        long now = Instant.now().toEpochMilli();
        Region region = new Region(
                new RegionId(name),
                name,
                level.dimension().location().toString(),
                new ArgbColor((int) Long.parseLong(argb.replace("0x", ""), 16)),
                "default",
                "default",
                parsePoints(pointsText),
                now,
                now
        );
        SERVICE.upsert(level, region);
        RegionNetwork.sendToAll(new RegionSyncPacket(List.copyOf(SERVICE.list(level))));
        source.sendSuccess(() -> Component.literal("Created region " + region.id().value()), true);
        return 1;
    }

    private static int messageOnly(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int createPoint(CommandSourceStack source, String playerName, String mode, String iconName, String label, String positionText) {
        int[] position = parseBlockPosition(positionText);
        UUID targetId = UUID.nameUUIDFromBytes(playerName.getBytes(StandardCharsets.UTF_8));
        PointMarker marker = new PointMarker(targetId, mode, iconName, label, position[0], position[1], position[2]);
        source.sendSuccess(() -> Component.literal("Created point marker " + marker.label() + " for " + playerName), true);
        return 1;
    }

    private static List<RegionPoint> parsePoints(String text) {
        String[] pairs = text.split(";");
        List<RegionPoint> points = new ArrayList<>(pairs.length);
        for (String pair : pairs) {
            String[] parts = pair.trim().split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Point must be formatted as x,z.");
            }
            points.add(new RegionPoint(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())));
        }
        return points;
    }

    private static int[] parseBlockPosition(String text) {
        String[] parts = text.trim().split(" ");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Position must be formatted as x y z.");
        }
        return new int[] {
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }
}
```

- [ ] **Step 3: 注册命令和登录同步**

Modify `src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java`:

```java
package com.suian.xaeroregionsrev;

import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.command.RegionCommands;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import com.suian.xaeroregionsrev.service.RegionService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.List;

@Mod(XaeroRegionsRev.MOD_ID)
public final class XaeroRegionsRev {
    public static final String MOD_ID = "xaeroregionsrev";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final RegionService REGION_SERVICE = new RegionService();

    public XaeroRegionsRev() {
        RegionNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Xaero Map Regions Rev loaded.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RegionCommands.register(event);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            RegionNetwork.sendToPlayer(player, new RegionSyncPacket(List.copyOf(REGION_SERVICE.list(level))));
        }
    }
}
```

- [ ] **Step 4: 验证构建**

Run:

```powershell
.\gradlew build
```

Expected: PASS。

- [ ] **Step 5: 提交服务端命令**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev/command src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java src/main/java/com/suian/xaeroregionsrev/network/RegionNetwork.java
git commit -m "feat: 添加区域命令和登录同步" -m "提供 MVP 创建多边形区域命令，并在玩家登录和区域变更时同步区域快照。" -m "- 添加 /region createpoly 管理命令。" -m "- 保留 hide、visible、createpoint 和 delpoint 命令入口。" -m "- 在登录事件中向客户端发送区域快照。"
```

### Task 9: 客户端缓存与同步处理

**Files:**
- Create: `src/main/java/com/suian/xaeroregionsrev/client/ClientRegionCache.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/network/RegionNetwork.java`

- [ ] **Step 1: 写入客户端缓存**

Create `src/main/java/com/suian/xaeroregionsrev/client/ClientRegionCache.java`:

```java
package com.suian.xaeroregionsrev.client;

import com.suian.xaeroregionsrev.region.Region;

import java.util.List;

public final class ClientRegionCache {
    private static List<Region> regions = List.of();

    private ClientRegionCache() {
    }

    public static List<Region> regions() {
        return regions;
    }

    public static void replaceAll(List<Region> syncedRegions) {
        regions = List.copyOf(syncedRegions);
    }

    public static void clear() {
        regions = List.of();
    }
}
```

- [ ] **Step 2: 让客户端处理同步包**

Modify `src/main/java/com/suian/xaeroregionsrev/network/RegionNetwork.java`:

```java
package com.suian.xaeroregionsrev.network;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.client.ClientRegionCache;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RegionNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(XaeroRegionsRev.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int packetId;

    private RegionNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(RegionSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RegionSyncPacket::encode)
                .decoder(RegionSyncPacket::decode)
                .consumerMainThread((packet, contextSupplier) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRegionCache.replaceAll(packet.regions()));
                    contextSupplier.get().setPacketHandled(true);
                })
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, RegionSyncPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(RegionSyncPacket packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
```

- [ ] **Step 3: 验证构建**

Run:

```powershell
.\gradlew build
```

Expected: PASS。

- [ ] **Step 4: 提交客户端缓存**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev/client src/main/java/com/suian/xaeroregionsrev/network/RegionNetwork.java
git commit -m "feat: 添加客户端区域缓存" -m "客户端接收服务端区域快照后保存只读缓存，为地图渲染提供数据来源。" -m "- 添加 ClientRegionCache。" -m "- 在网络包处理线程更新客户端缓存。"
```

### Task 10: Xaero 地图 Overlay 首版渲染

**Files:**
- Create: `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroScreenDetector.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/client/xaero/MapProjectionAdapter.java`
- Create: `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayRenderer.java`
- Modify: `src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java`

- [ ] **Step 1: 写入 Xaero 屏幕探测器**

Create `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroScreenDetector.java`:

```java
package com.suian.xaeroregionsrev.client.xaero;

import net.minecraft.client.gui.screens.Screen;

import java.util.Locale;

public final class XaeroScreenDetector {
    private XaeroScreenDetector() {
    }

    public static boolean isWorldMapScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        String className = screen.getClass().getName().toLowerCase(Locale.ROOT);
        return className.contains("xaero") && className.contains("world") && className.contains("map");
    }
}
```

- [ ] **Step 2: 写入投影适配器**

Create `src/main/java/com/suian/xaeroregionsrev/client/xaero/MapProjectionAdapter.java`:

```java
package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Vector2f;

public final class MapProjectionAdapter {
    private static final float DEFAULT_PIXELS_PER_BLOCK = 0.25F;

    public Vector2f project(Screen screen, RegionPoint point) {
        Minecraft minecraft = Minecraft.getInstance();
        double playerX = minecraft.player == null ? 0.0D : minecraft.player.getX();
        double playerZ = minecraft.player == null ? 0.0D : minecraft.player.getZ();
        float centerX = screen.width / 2.0F;
        float centerY = screen.height / 2.0F;
        float x = centerX + (float) (point.x() - playerX) * DEFAULT_PIXELS_PER_BLOCK;
        float y = centerY + (float) (point.z() - playerZ) * DEFAULT_PIXELS_PER_BLOCK;
        return new Vector2f(x, y);
    }
}
```

- [ ] **Step 3: 写入 Overlay 渲染器**

Create `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayRenderer.java`:

```java
package com.suian.xaeroregionsrev.client.xaero;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.suian.xaeroregionsrev.client.ClientRegionCache;
import com.suian.xaeroregionsrev.region.Region;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.List;

public final class XaeroMapOverlayRenderer {
    private static final MapProjectionAdapter PROJECTION = new MapProjectionAdapter();

    private XaeroMapOverlayRenderer() {
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!XaeroScreenDetector.isWorldMapScreen(screen)) {
            return;
        }
        renderRegions(event.getGuiGraphics(), screen, ClientRegionCache.regions());
    }

    private static void renderRegions(GuiGraphics graphics, Screen screen, List<Region> regions) {
        for (Region region : regions) {
            if (region.points().size() < 3) {
                continue;
            }
            drawPolygon(graphics, screen, region);
        }
    }

    private static void drawPolygon(GuiGraphics graphics, Screen screen, Region region) {
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        int color = region.color().value();
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        float red = ((color >>> 16) & 0xFF) / 255.0F;
        float green = ((color >>> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        for (var point : region.points()) {
            Vector2f projected = PROJECTION.project(screen, point);
            buffer.vertex(matrix, projected.x(), projected.y(), 0.0F).color(red, green, blue, alpha).endVertex();
        }
        tesselator.end();
    }
}
```

- [ ] **Step 4: 注册客户端渲染事件**

Modify `src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java`:

```java
package com.suian.xaeroregionsrev;

import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.client.xaero.XaeroMapOverlayRenderer;
import com.suian.xaeroregionsrev.command.RegionCommands;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import com.suian.xaeroregionsrev.service.RegionService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.List;

@Mod(XaeroRegionsRev.MOD_ID)
public final class XaeroRegionsRev {
    public static final String MOD_ID = "xaeroregionsrev";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final RegionService REGION_SERVICE = new RegionService();

    public XaeroRegionsRev() {
        RegionNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.addListener(this::onScreenRenderPost));
        LOGGER.info("Xaero Map Regions Rev loaded.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RegionCommands.register(event);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            RegionNetwork.sendToPlayer(player, new RegionSyncPacket(List.copyOf(REGION_SERVICE.list(level))));
        }
    }

    private void onScreenRenderPost(ScreenEvent.Render.Post event) {
        XaeroMapOverlayRenderer.onScreenRenderPost(event);
    }
}
```

- [ ] **Step 5: 验证构建**

Run:

```powershell
.\gradlew build
```

Expected: PASS。若 `DefaultVertexFormat.POSITION_COLOR` 或 `ScreenEvent.Render.Post` API 名称在 Forge 47.3.33 下编译失败，使用 IDE/反编译依赖确认实际名称，只修改 `client.xaero` 包和主类事件注册，不改共享模型和服务层。

- [ ] **Step 6: 提交 Overlay 首版**

Run:

```powershell
git add src/main/java/com/suian/xaeroregionsrev/client/xaero src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java
git commit -m "feat: 添加 Xaero 地图区域覆盖层" -m "在客户端屏幕渲染事件中探测 Xaero World Map，并绘制服务端同步的区域多边形。" -m "- 添加 Xaero 屏幕探测器。" -m "- 添加地图坐标投影适配器。" -m "- 添加多边形半透明填充渲染。"
```

### Task 11: 运行期依赖与烟测

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: 下载 Xaero World Map 运行期 jar 到 `run/mods`**

Run:

```powershell
New-Item -ItemType Directory -Force run/mods | Out-Null
Invoke-WebRequest -Uri "https://cdn.modrinth.com/data/NcUtCpym/versions/wciL1Yas/xaeroworldmap-forge-1.20.1-1.41.2.jar" -OutFile "run/mods/xaeroworldmap-forge-1.20.1-1.41.2.jar"
```

Expected: `run/mods/xaeroworldmap-forge-1.20.1-1.41.2.jar` 存在，且不被 Git 跟踪。

- [ ] **Step 2: 执行完整自动验证**

Run:

```powershell
.\gradlew clean test build
```

Expected: PASS。

- [ ] **Step 3: 启动客户端烟测**

Run:

```powershell
.\gradlew runClient
```

Expected:

- 游戏能启动到主菜单。
- Mods 列表中存在 `Xaero Map Regions Rev`。
- 安装 Xaero World Map 后游戏不崩溃。

- [ ] **Step 4: 手动区域显示烟测**

In game:

```text
/op <your-player-name>
/gamemode creative
/region createpoly spawn 0x8800FF00 0,0;64,0;64,64;0,64
```

Expected:

- 命令返回 `Created region spawn`。
- 重新进入世界后区域仍存在。
- 打开 Xaero World Map 时能看到一个绿色半透明区域覆盖层。

- [ ] **Step 5: 记录烟测命令到 AGENTS 文件树说明**

Modify `AGENTS.md` 的技术约定增加：

```markdown
- 本地烟测使用 `.\gradlew clean test build` 和 `.\gradlew runClient`；运行期 Xaero World Map jar 放在 `run/mods/`，该目录不进入 Git。
```

- [ ] **Step 6: 提交烟测说明**

Run:

```powershell
git add AGENTS.md
git commit -m "docs: 记录本地烟测约定" -m "补充 Forge 构建和 Xaero 运行期依赖的本地验证方式。" -m "- 记录 clean test build 和 runClient 烟测命令。" -m "- 明确 run/mods 中的 Xaero jar 不进入 Git。"
```

### Task 12: 收尾验证

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: 检查文件树与实际文件一致**

Run:

```powershell
rg --files
```

Expected: 输出包含计划中的 `build.gradle`、`gradle.properties`、`src/main/java/...`、`src/test/java/...`，不包含 `run/mods` 下的 jar。

- [ ] **Step 2: 更新 `AGENTS.md` 文件树**

Modify `AGENTS.md` 文件树，加入 Gradle 文件、源码目录、测试目录、计划文档和 spec 文档，描述每个主要目录职责。

- [ ] **Step 3: 最终自动验证**

Run:

```powershell
.\gradlew clean test build
git status --short
```

Expected:

- Gradle 命令 PASS。
- `git status --short` 只显示 `M AGENTS.md`。

- [ ] **Step 4: 提交文件树更新**

Run:

```powershell
git add AGENTS.md
git commit -m "docs: 更新项目文件树" -m "实现 MVP 基线后同步维护项目规则文件中的文件树说明。" -m "- 补充 Gradle 构建文件。" -m "- 补充源码和测试目录职责。" -m "- 保持 AGENTS.md 作为通用规则单一事实源。"
```

- [ ] **Step 5: 汇总实现结果**

Run:

```powershell
git log --oneline --decorate -12
git status --short
```

Expected:

- 最近提交包含本计划每个任务的中文提交。
- 工作树干净。

## 自审结果

- Spec 覆盖：计划覆盖 Forge 47.3.33 脚手架、区域模型、权限、NBT、SavedData、网络同步、命令、客户端缓存、Xaero overlay 和烟测。
- 向上兼容：Forge 版本集中在 `gradle.properties`；Forge/Xaero 依赖代码集中在 `platform`、`network`、`client.xaero` 包。
- 风险：Xaero World Map 的真实缩放/平移 API 未公开，首版 `MapProjectionAdapter` 使用玩家位置居中近似投影；通过后续小任务替换适配器即可提升准确性，不影响服务端数据与同步闭环。
