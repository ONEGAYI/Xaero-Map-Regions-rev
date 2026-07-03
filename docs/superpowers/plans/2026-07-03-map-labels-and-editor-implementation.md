# 地图标注与地图内编辑 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` to execute this plan.

**Goal:** 在 Minecraft 1.20.1 + Forge 47.3.33 中，为现有区域同步与 Xaero World Map 覆盖层补齐区域地名标注、标签颜色、可改快捷键、地图内编辑模式、地图内创建/删除区域，以及选中区域后的右键菜单改色/改标签能力。

**Architecture:** 服务端继续作为区域数据权威来源。客户端只负责 Xaero 地图 GUI 上的编辑交互、临时草稿、选择状态和渲染；所有持久化变更通过 C2S 请求包提交到服务端，由服务端进行权限、维度、点数、字符串长度、颜色和多边形校验，再写入 `RegionSavedData` 并广播完整 `RegionSyncPacket`。

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.3.33, ForgeGradle 6.x, JUnit 5, Xaero's World Map runtime jar in `libs/runtime/`.

**Authoritative Specs:**

- `docs/superpowers/specs/2026-07-03-map-labels-and-editor-design.md`
- Forge 1.20.x Key Mappings: `RegisterKeyMappingsEvent`, GUI 内用 `IForgeKeyMapping#isActiveAndMatches`，不硬编码物理按键。
- Forge 1.20.x SimpleImpl: 网络包处理必须 `enqueueWork`；服务端处理客户端包时必须防御式校验。

---

## Current State

- `Region` 目前只有 `name` 和填充 `color`，没有独立 `label` 与 `labelColor`。
- `RegionNbtCodec` 和 `RegionSyncPacket` 编解码的是当前模型字段。
- `RegionNetwork` 只注册 S2C 同步包，已有 `sendToPlayer` 和 `sendToAll`。
- `RegionCommands` 支持 `/region createpoly`、`delete`、`list`、`sync`，但颜色解析是命令私有逻辑。
- `XaeroMapOverlayRenderer` 已能在 `xaero.map.gui.GuiMap` 上渲染同步区域填充。
- `MapProjectionAdapter` 只有 world -> screen 投影，没有 screen -> world 反投影。
- `AGENTS.md` 文件树还未记录 2026-07-03 的标注/编辑 spec 和本计划。

---

## Desired Behavior

1. 区域可以显示地名标签，标签内容与标签字体颜色可自定义。
2. 旧区域数据和旧命令创建的区域自动使用 `name` 作为标签，标签颜色默认白色。
3. Xaero 地图页增加编辑模式切换按钮。
4. 注册可在 MC 控制设置中修改的快捷键：
   - 默认 `R`：切换区域编辑模式。
   - 默认 `T`：普通游戏视角打开区域管理器。
5. 编辑模式中：
   - 鼠标中键在当前地图光标位置添加草稿顶点。
   - 左键选择已有区域。
   - Enter 在草稿顶点数达到 3 后打开保存区域表单。
   - Esc 清空草稿或退出编辑模式。
   - 选中区域后右键打开上下文菜单。
6. 右键菜单至少包含：
   - 删除区域。
   - 修改填充颜色。
   - 修改标签内容。
   - 修改标签颜色。
7. 所有创建、删除、改色、改标签请求必须经过服务端权限与数据校验。

---

## Security and Compatibility Rules

- Client-only 类必须保留在 `client` 包内，并通过 `DistExecutor` 或 client-only event subscriber 注册，避免 dedicated server classloading。
- C2S 包不信任客户端：
  - 创建请求不携带维度字段，服务端使用 `ServerPlayer.serverLevel().dimension().location().toString()` 作为最终维度。
  - 删除和更新只作用于 `sender.serverLevel()` 对应的 `SavedData`，本阶段不允许跨维度删除或更新。
  - 不访问客户端给出的区块、方块实体或强制加载区块；区域点只作为纯坐标保存。
  - 所有 C2S 字符串 decode 必须使用 `readUtf(maxLength)`，handler 内再次 trim 并校验非空。
  - 限制区域点数、标签长度、名称长度、颜色文本长度。
  - 校验 `PolygonMath.isValidPolygon`。
  - 创建区域时由服务端根据名称生成 `RegionId`，发现重复 id 必须拒绝，不能调用覆盖式 upsert。
  - 校验玩家权限，沿用 `ForgePermissionAdapter.canManageRegions()`。
- S2C 仍用完整快照同步，避免客户端局部状态与服务端持久化不一致。
- 网络协议版本需要递增，因为 `RegionSyncPacket` 增加字段，且新增 C2S 消息。
- NBT 读旧数据时必须兼容缺失 `label` 和 `labelColor`。
- `/region createpoly` 保持现有参数兼容，新增标签相关命令不要破坏旧脚本。

---

## Implementation Tasks

### 1. Region 模型增加标签字段

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/region/Region.java`
- `src/test/java/com/suian/xaeroregionsrev/region/RegionTest.java`

**Tests first:**

- 构造包含 `label` 和 `labelColor` 的 `Region` 后字段可读。
- 旧构造方式仍可用，`label == name`，`labelColor == 0xFFFFFFFF`。
- 空白 `label` 被拒绝。
- `labelColor == null` 被拒绝。

**Implementation:**

Add fields near visual metadata:

```java
public record Region(
        RegionId id,
        String name,
        String dimension,
        ArgbColor color,
        String label,
        ArgbColor labelColor,
        String category,
        String iconName,
        List<RegionPoint> points,
        long createdAt,
        long updatedAt
) {
    public Region(
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
        this(id, name, dimension, color, name, new ArgbColor(0xFFFFFFFF), category, iconName, points, createdAt, updatedAt);
    }
}
```

Keep the compact constructor validation and `List.copyOf(points)`.

### 2. 提取共享颜色解析与格式化

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/region/RegionColorParser.java`
- `src/test/java/com/suian/xaeroregionsrev/region/RegionColorParserTest.java`
- `src/main/java/com/suian/xaeroregionsrev/command/RegionCommands.java`

**Tests first:**

- Accept `#RRGGBB`, `RRGGBB`, `0xRRGGBB` -> alpha defaults to `FF`.
- Accept `#AARRGGBB`, `AARRGGBB`, `0xAARRGGBB`.
- Reject blank, too long, non-hex.

**Implementation:**

- Move command-private parsing into `RegionColorParser.parse(String)`.
- RGB 输入一律按不透明色处理，即 `RRGGBB` 解析为 `0xFFRRGGBB`；旧命令测试若覆盖 RGB 行为，应按该规则更新。
- Add `format(ArgbColor)` returning `#AARRGGBB` for UI defaults and command output.

### 3. NBT 与 S2C 同步兼容新字段

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/region/RegionNbtCodec.java`
- `src/main/java/com/suian/xaeroregionsrev/network/payload/RegionSyncPacket.java`
- Existing codec/packet tests under `src/test/java/...`

**Tests first:**

- NBT 写入包含 `label`、`labelColor`。
- NBT 读取缺失 `label` 的旧数据时默认 `name`。
- NBT 读取缺失 `labelColor` 的旧数据时默认白色。
- `RegionSyncPacket` round-trip 保留 `label` 和 `labelColor`。
- Decode 时保持区域数与点数上限。

**Implementation:**

- `writeRegion` 增加 `label` 与 `labelColor`。
- `readRegion` 对新字段使用兼容读取，不通过 `requireField` 强制旧存档必须存在。
- `RegionSyncPacket` encode/decode 在 `color` 后写入 `label` 和 `labelColor`。
- `PROTOCOL_VERSION` 在 `RegionNetwork` 中从当前值递增。

### 4. 服务层提供样式更新能力

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/service/RegionService.java`
- `src/test/java/com/suian/xaeroregionsrev/service/RegionServiceTest.java`

**Tests first:**

- `updateStyle` 只改变填充颜色、标签内容、标签颜色与 `updatedAt`。
- `updateStyle` 对不存在的 id 返回失败结果或空 Optional。
- 删除后再次查找为空。

**Implementation:**

- 增加 `Optional<Region> updateStyle(ServerLevel level, RegionId id, ArgbColor fillColor, String label, ArgbColor labelColor, long now)`。
- 保持 `createdAt`、`points`、`dimension`、`category`、`iconName` 不变。
- 抽出纯 Java 可测的 `RegionStyleUpdate` 或 `RegionStyleUpdater.withStyle(...)` helper；`RegionServiceTest` 不直接依赖真实 `ServerLevel`。

### 5. 增加请求限制与验证器

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/region/RegionLimits.java`
- `src/main/java/com/suian/xaeroregionsrev/region/RegionRequestValidator.java`
- `src/test/java/com/suian/xaeroregionsrev/region/RegionRequestValidatorTest.java`

**Tests first:**

- 名称和标签会 trim，空白内容被拒绝。
- 名称、标签、点数超过上限会被拒绝。
- 点数少于 3 或非法多边形会被拒绝。
- 合法请求返回规范化后的名称、标签、颜色和点集合。

**Implementation:**

- `RegionLimits` 统一定义 `MAX_NAME_LENGTH`、`MAX_LABEL_LENGTH`、`MAX_POINTS_PER_REQUEST` 等限制。
- `RegionRequestValidator` 提供纯 Java 校验，供命令和 C2S handler 共用。
- C2S payload decode 使用这些长度上限，handler 再调用 validator 做二次校验。

### 6. 增加 C2S 编辑请求包

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/network/payload/CreateRegionRequestPacket.java`
- `src/main/java/com/suian/xaeroregionsrev/network/payload/DeleteRegionRequestPacket.java`
- `src/main/java/com/suian/xaeroregionsrev/network/payload/UpdateRegionStyleRequestPacket.java`
- `src/main/java/com/suian/xaeroregionsrev/network/RegionNetwork.java`
- `src/main/java/com/suian/xaeroregionsrev/network/RegionEditRequestHandler.java`
- `src/test/java/com/suian/xaeroregionsrev/network/payload/*Test.java`

**Tests first:**

- Create packet round-trip 保留 name, fillColor, label, labelColor, points。
- Delete packet round-trip 保留 id。
- Update style packet round-trip 保留 id, fillColor, label, labelColor。
- Decode 拒绝超出上限的点数与过长字符串。
  - 字符串必须通过 `readUtf(RegionLimits.MAX_NAME_LENGTH)` 和 `readUtf(RegionLimits.MAX_LABEL_LENGTH)` 读取。

**Implementation:**

- Create request fields:
  - `String name`
  - `ArgbColor fillColor`
  - `String label`
  - `ArgbColor labelColor`
  - `List<RegionPoint> points`
- Delete request fields:
  - `RegionId id`
- Update request fields:
  - `RegionId id`
  - `ArgbColor fillColor`
  - `String label`
  - `ArgbColor labelColor`
- Use shared constants from `RegionLimits`:
  - `MAX_NAME_LENGTH = 80`
  - `MAX_LABEL_LENGTH = 80`
  - `MAX_POINTS_PER_REQUEST = 256`
- Register all messages with explicit `NetworkDirection`:
  - `PLAY_TO_CLIENT`: `RegionSyncPacket`
  - `PLAY_TO_SERVER`: create/delete/update requests
- Handler shape:

```java
public static void handle(CreateRegionRequestPacket packet, Supplier<NetworkEvent.Context> context) {
    context.get().enqueueWork(() -> {
        ServerPlayer sender = context.get().getSender();
        if (sender == null || !ForgePermissionAdapter.from(sender).canManageRegions()) {
            return;
        }
        ServerLevel level = sender.serverLevel();
        // Validate packet again here, create Region using server dimension, reject duplicate id, save, then broadcast all regions.
    });
    context.get().setPacketHandled(true);
}
```

- Do not call client classes from handlers.
- Create handler must `find(level, generatedId)` before saving; if it exists, send a chat error and return.
- Delete/update handlers only operate on `sender.serverLevel()` and must reject missing `RegionId`.
- Add a `sendToServer(Object packet)` convenience method only if it keeps client code simple; otherwise call `CHANNEL.sendToServer(...)` from a client wrapper.

### 7. 注册可改快捷键与客户端入口

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/client/RegionKeyMappings.java`
- `src/main/java/com/suian/xaeroregionsrev/client/XaeroRegionsClient.java`
- `src/main/java/com/suian/xaeroregionsrev/client/editor/RegionManagerScreen.java`
- `src/main/java/com/suian/xaeroregionsrev/XaeroRegionsRev.java`
- `src/main/resources/assets/xaeroregionsrev/lang/en_us.json`
- `src/main/resources/assets/xaeroregionsrev/lang/zh_cn.json`

**Tests first:**

- 对可纯 Java 测试的 key action router 写测试：当 action 为 toggle/open-manager 时只改变编辑会话或产生打开管理器动作，不直接依赖物理按键码。

**Implementation:**

- `RegionKeyMappings` 用 `Lazy<KeyMapping>` 定义：
  - `key.xaeroregionsrev.toggle_edit_mode`
  - `key.xaeroregionsrev.open_region_manager`
  - category `key.categories.xaeroregionsrev`
- 默认输入使用 `InputConstants.Type.KEYSYM` + GLFW key token。
- conflict context 使用 GUI 语境或自定义只在 Xaero map screen active 的 context。
- 在 client-only MOD event bus 订阅 `RegisterKeyMappingsEvent`，通过 `FMLJavaModLoadingContext.get().getModEventBus()` 或等价入口注册。
- 在 `XaeroRegionsRev` 构造中通过 `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> XaeroRegionsClient::register)` 注册客户端事件。
- `T` 在普通游戏视角打开 `RegionManagerScreen`，该屏幕先实现当前维度区域列表与刷新入口；跨维度管理留到后续阶段。

### 8. 客户端编辑会话与选择逻辑

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditSession.java`
- `src/main/java/com/suian/xaeroregionsrev/client/editor/RegionSelection.java`
- `src/test/java/com/suian/xaeroregionsrev/client/editor/RegionEditSessionTest.java`
- `src/test/java/com/suian/xaeroregionsrev/client/editor/RegionSelectionTest.java`

**Tests first:**

- Toggle edit mode enters/exits cleanly.
- Middle-click adding points only works in edit mode.
- Three or more points can be submitted; fewer points cannot.
- Selecting a screen point returns topmost matching region when polygons overlap.
- Esc clears draft first, second Esc exits edit mode.

**Implementation:**

- Keep session pure Java:
  - `boolean editing`
  - `List<RegionPoint> draftPoints`
  - `RegionId selectedRegionId`
  - context menu state can be separate view state.
- Selection uses current `ClientRegionCache.snapshot()` plus projected points supplied by UI layer.
- No networking inside session; session emits action objects such as `CreateDraftRequested`, `DeleteSelectedRequested`.

### 9. 投影增加反投影与可测试核心

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/client/xaero/MapProjectionAdapter.java`
- `src/test/java/com/suian/xaeroregionsrev/client/xaero/MapProjectionAdapterTest.java`

**Tests first:**

- Given center/player/scale, project then unproject returns original block coordinate within rounding rules.
- Rounding strategy is stable for negative coordinates.

**Implementation:**

- Extract pure helpers:

```java
static Vector2f projectRelative(int blockX, int blockZ, double playerX, double playerZ, float centerX, float centerY, float pixelsPerBlock)
static RegionPoint unprojectRelative(double screenX, double screenY, double playerX, double playerZ, float centerX, float centerY, float pixelsPerBlock)
```

- Public `unproject(Screen, double x, double y)` uses current player position and same scale as `project`.
- Keep adapter isolated so future Xaero API-specific projection can replace the approximation without touching editor state.

### 10. 地图覆盖层控制器、渲染标签、按钮、草稿和菜单

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayRenderer.java`
- `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java`
- `src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java`
- `src/main/java/com/suian/xaeroregionsrev/client/editor/RegionContextMenu.java`
- `src/main/java/com/suian/xaeroregionsrev/client/editor/RegionStyleEditScreen.java`

**Tests first:**

- Pure helper test: label anchor uses polygon centroid or bounding box center.
- Pure helper test: edit button hitbox does not move when text changes.
- Pure helper test: context menu actions map to delete/update commands.
- Pure action-router test: middle click adds point, left click selects region, Enter opens save form when draft is valid.

**Implementation:**

- Existing fill polygon rendering stays in `XaeroMapOverlayRenderer`.
- Add label rendering after polygon fill:
  - Anchor at polygon centroid/bounds center.
  - Use `GuiGraphics.drawString(font, label, x, y, labelColor.value(), true)`.
  - Skip labels outside screen bounds.
- Add edit mode button:
  - Stable size and top-right or bottom-right placement.
  - Button text/icon localized.
  - Active state visually distinct.
- Add draft rendering:
  - Points and connecting lines while editing.
  - Preview fill only after 3 points.
- Add selected region outline.
- Add context menu:
  - Opens on right-click selected region.
  - Contains delete, fill color, label text, label color commands.
  - Delete must open a confirmation screen before sending `DeleteRegionRequestPacket`.
- Add style edit screen:
  - `EditBox` for label.
  - `EditBox` for fill color.
  - `EditBox` for label color.
  - Validate client-side before sending, but still rely on server validation.

### 11. GUI 输入事件接入

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapInputHandler.java`
- `src/main/java/com/suian/xaeroregionsrev/client/XaeroRegionsClient.java`

**Tests first:**

- Unit test pure action router:
  - Toggle key toggles editing.
  - Enter opens the create form only when draft polygon is valid.
  - Middle click adds a draft point.
  - Left click selects an existing region.
  - Right click selected region opens menu.
  - Left click edit button toggles editing.

**Implementation:**

- Listen on Forge event bus:
  - `ScreenEvent.KeyPressed.Pre`
  - `ScreenEvent.MouseButtonPressed.Pre`
  - optionally `ScreenEvent.MouseButtonReleased.Pre` if needed for button state.
- First check `XaeroScreenDetector.isXaeroMapScreen(event.getScreen())`.
- For key mappings inside foreign GUI, use `RegionKeyMappings.X.get().isActiveAndMatches(InputConstants.getKey(key, scanCode))`.
- For mouse mappings, use `InputConstants.Type.MOUSE.getOrCreate(button)`.
- Consume events only when our overlay handled the interaction.
- Do not process keyboard input while `RegionStyleEditScreen` text fields are focused.

### 12. 命令补充标签能力

**Files:**

- `src/main/java/com/suian/xaeroregionsrev/command/RegionCommands.java`
- `src/test/java/com/suian/xaeroregionsrev/command/RegionCommandsTest.java` if command helpers are testable.

**Tests first:**

- Shared color parser is used.
- Existing `/region createpoly` behavior remains compatible.

**Implementation:**

- Keep `/region createpoly <name> <argb> <points>` unchanged.
- Add explicit style update command if useful for smoke:
  - `/region style <id> <fillColor> <labelColor> <label>`
- Add optional labeled creation only if command grammar stays unambiguous:
  - Prefer separate command over complex greedy string parsing.

### 13. Verification

Run after implementation:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runServer
```

Manual GUI smoke:

```powershell
.\gradlew.bat runClient
```

Smoke checklist:

- Client reaches main menu.
- Create or load `run/saves/New World`.
- Open Xaero World Map.
- Existing command-created regions still render fill and labels.
- `R` toggles edit mode, and key appears in Controls menu.
- `T` opens the region manager from normal gameplay, and key appears in Controls menu.
- Edit button toggles edit mode.
- Middle-click adds points.
- Left-click selects existing regions.
- Enter opens the save form for a valid polygon.
- New region appears without restarting world.
- Select region -> right-click menu -> change label, label color, fill color.
- Delete region from menu, confirm it disappears and stays deleted after relog.
- Dedicated server classloading check: `.\gradlew.bat runServer` reaches startup without client class errors.

### 14. Documentation and Commit Hygiene

**Files:**

- `AGENTS.md`
- `docs/superpowers/plans/2026-07-03-map-labels-and-editor-implementation.md`

**Implementation:**

- Update `AGENTS.md` file tree to list:
  - `2026-07-03-map-labels-and-editor-design.md`
  - `2026-07-03-map-labels-and-editor-implementation.md`
- Commit plan separately before implementation:

```text
docs: 添加地图标注与编辑实现计划

- 规划区域标签字段、NBT/网络兼容与服务端权威编辑请求
- 规划 Xaero 地图内编辑模式、可改快捷键、右键菜单和 GUI 烟测
- 补充安全边界，明确 C2S 包不信任客户端输入
```

---

## Execution Order

1. Data model and parser: Tasks 1-3.
2. Server helpers, validator, and network requests: Tasks 4-6.
3. Client key mappings, manager entry, and pure editor state: Tasks 7-9.
4. Xaero overlay controller, rendering, and input UI: Tasks 10-11.
5. Command helper and docs: Tasks 12-14.
6. Full verification and GUI smoke.

This order keeps serialization compatibility and server validation in place before client UI can send mutations.

---

## Risk Register

- **Xaero map projection may remain approximate.** Current renderer already uses an approximate player-centered scale. This feature should reuse that adapter and keep it replaceable.
- **Foreign GUI event handling can conflict with Xaero controls.** Only consume events when the edit overlay explicitly handles them, and only on detected Xaero map screens.
- **Dedicated server classloading risk.** All client classes must remain behind client-only registration; `runServer` is a required verification step before completion.
- **NBT migration risk.** Missing new fields must default cleanly so existing saved regions remain readable.
- **Network abuse risk.** Request packets must be bounded and revalidated server-side, with no chunk/block access based on client coordinates.
