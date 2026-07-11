# 重叠区域循环选中设计

> 日期：2026-07-11
> 状态：待实现

## 背景与动机

当前编辑器中，当多个区域在同一位置重叠时，左键点击只能选中渲染顺序最靠后的那一个（"顶层"）。如果用户想选中被遮挡的下层区域，只能先删除上层区域——这很不科学。

用户需求：

1. **循环切换图层**：第一次点击选中顶层；在命中同一组重叠区域的前提下，再次点击推进到下一层（顶层 → 底层 → 回到顶层）。
2. **右上角 HUD**：显示当前选中的层数和区域名称。命中 ≥2 个区域时显示 `当前/总数`，否则只显示名称。名称过长截断显示，悬停 tooltip 显示完整名称。

## 设计决策摘要

| 决策点 | 结论 |
|--------|------|
| "再次点击同一位置"判定 | 命中同一组重叠区域即可（按命中区域 id 集合判定，不要求精确坐标） |
| 循环方向 | 顶层 → 底层 → 回到顶层 |
| HUD 显示时机 | `selectedRegionId` 不为空即显示（不限编辑模式） |
| 层数显示时机 | 仅命中 ≥2 个区域时显示 `当前/总数`；单个区域只显示名称 |
| HUD 位置 | 编辑按钮正下方，右对齐到按钮右边缘 |

## 架构选型

**方案：在 `RegionEditSession`（common 模块）中存储候选堆栈与循环索引。**

核心理由：

- **可测试性**：状态机逻辑（候选栈推进、循环回顶、新位置重置）全部在 common 模块的纯 Java 类中，可用现有 JUnit 5 契约测试完整覆盖，不依赖游戏运行环境。
- **改动内聚**：`RegionEditSession` 本就是编辑会话状态持有者，扩展它最自然；平台层只改两处薄薄的转发（`ActionRouter` 左键分支）和 HUD 渲染。
- **与现有代码一致**：选中状态已在 session 中，`setEditing(false)` / `reset()` 清理选中只需扩展清理候选栈。

被否决的替代方案：

- *在客户端缓存层维护候选堆栈*：选中状态与编辑会话紧耦合（`setEditing(false)` 要 `clearSelection`），拆到两处需要更多胶水代码，一致性维护复杂。
- *不改 session，每次点击实时计算*：无法可靠判断"是否还在同一组重叠区域内"——区域被删除或服务端同步导致顺序变化时，当前索引会丢失；且逻辑全在平台层无法用 common 纯 Java 测试覆盖。

---

## 第一部分：核心数据模型与状态机（common 模块）

### 1.1 `RegionSelection.selectStack`（新增）

`RegionSelection`（`common/src/main/java/.../client/editor/RegionSelection.java`）新增方法，返回所有命中区域，顺序与渲染顺序一致（列表中靠后的 = 视觉顶层）：

```java
public static List<Region> selectStack(List<Region> regions, String dimension, int blockX, int blockZ) {
    List<Region> hits = new ArrayList<>();
    for (Region region : regions) {
        if (!region.dimension().equals(dimension)) {
            continue;
        }
        if (region.points().size() >= 3 && PolygonMath.contains(region.points(), blockX, blockZ)) {
            hits.add(region);
        }
    }
    return hits;
}
```

`selectTopmost` 改为基于 `selectStack` 取最后一个元素，保持原语义不变（已有测试 `selectsLastMatchingRegionWhenPolygonsOverlap` 继续通过）。

### 1.2 `RegionEditSession` 扩展选中状态

在现有 `selectedRegionId` 基础上新增字段：

```java
private List<RegionId> candidateStack = List.of();   // 命中栈，顶层→底层顺序（渲染顺序的反转）
private int selectionIndex = -1;                      // 当前在栈中的索引，-1 表示无选中
private int lastClickX = Integer.MIN_VALUE;           // 上次命中的世界坐标
private int lastClickZ = Integer.MIN_VALUE;
```

**`candidateStack` 排序约定**：存储为"顶层→底层"顺序，即渲染顺序的反转。`selectionIndex = 0` 对应顶层。这样循环方向（顶层→底层）即为索引递增方向，语义直观。

### 1.3 状态机方法 `advanceSelection`（核心逻辑）

```java
/**
 * 推进区域选中。同一组重叠区域内连续点击循环切换图层；
 * 命中不同的区域组合时重新从顶层开始。
 *
 * @param hitStack selectStack 返回的命中区域列表（渲染顺序：底层在前，顶层在后）
 * @param clickX   本次点击的世界坐标 X
 * @param clickZ   本次点击的世界坐标 Z
 * @return true 表示选中了某个区域（新选中或循环切换）；false 表示未命中任何区域
 */
public boolean advanceSelection(List<Region> hitStack, int clickX, int clickZ)
```

状态机流程：

```
advanceSelection(hitStack, clickX, clickZ):
  if hitStack 为空:
    clearSelection()
    return false

  // 反转为顶层→底层顺序
  reversedStack = hitStack 中各 Region 的 id 提取后反转

  // 判定"是否仍是同一组重叠区域"：比较 id 集合（Set<RegionId>），不要求顺序一致
  if selectionIndex >= 0 且 new HashSet<>(reversedStack) == new HashSet<>(candidateStack):
    // 仍然是同一组重叠区域 → 推进到下一层
    // 注意：推进基于上次的 candidateStack 快照，不受服务端同步导致的顺序变化影响
    selectionIndex = (selectionIndex + 1) % candidateStack.size()
    selectedRegionId = candidateStack.get(selectionIndex)
    lastClickX = clickX
    lastClickZ = clickZ
    return true

  else:
    // 新的点击位置 / 不同的候选组 → 从顶层开始
    candidateStack = reversedStack
    selectionIndex = 0
    selectedRegionId = candidateStack.get(0)
    lastClickX = clickX
    lastClickZ = clickZ
    return true
```

**"同一组候选"判定**：比较两次点击命中的区域 id 集合（`Set<RegionId>`）是否相同。不要求精确坐标一致——只要还在同一片重叠区域内任意位置点击，都能继续往下翻。区域 id 集合不同（例如鼠标移到了重叠区的不同位置，命中了不同组合）则重新从顶层开始。

**为什么基于 id 集合而非顺序**：服务端同步可能导致 `LinkedHashMap` 插入顺序变化（区域被删除再重建），如果按顺序比较会误判为"新候选组"而重置索引。按 id 集合比较更稳健；循环推进则始终基于 `candidateStack` 的上次快照，保证用户在同一片重叠区连续点击时体验稳定。

### 1.4 `select(RegionId)` 语义收窄

现有的直接 `select(RegionId)` 方法保留（区域管理器列表点击等仍可能使用），但走"直接指定"路径：

- `candidateStack = List.of(regionId)`（单元素）
- `selectionIndex = 0`
- `lastClickX = lastClickZ = Integer.MIN_VALUE`（表示"非地图点击来源"）

下次地图左键点击时，`advanceSelection` 的 id 集合对比必然不匹配（单元素 vs 新命中的栈），会重新从顶层开始循环。

### 1.5 `selectionInfo()` 查询方法（供 HUD 使用）

`RegionEditSession` 只持有 `RegionId`，不持有 `Region` 对象。为了 session 保持轻量、且区域重命名后 HUD 能立即反映最新名称，`SelectionInfo` 不携带 `label`，label 由 HUD 渲染层（平台层）从 `ClientRegionCache.regions()` 中按 id 实时查找（见 3.5）。

```java
public record SelectionInfo(RegionId id, int index, int total) {}

public Optional<SelectionInfo> selectionInfo() {
    if (selectedRegionId == null || selectionIndex < 0) {
        return Optional.empty();
    }
    return Optional.of(new SelectionInfo(
            selectedRegionId, selectionIndex + 1, candidateStack.size()));
}
```

`index` 从 1 开始（用户友好），`total` = `candidateStack.size()`。

### 1.7 清理逻辑

`clearSelection()` 扩展为同时清空 `candidateStack`、`selectionIndex`、`lastClickX/Z`：

```java
public void clearSelection() {
    selectedRegionId = null;
    candidateStack = List.of();
    selectionIndex = -1;
    lastClickX = Integer.MIN_VALUE;
    lastClickZ = Integer.MIN_VALUE;
}
```

`setEditing(false)`、`reset()` 调用 `clearSelection()` 的现有语义不变。

---

## 第二部分：点击交互改动（neoforge + forge 平台层）

### 2.1 `ActionRouter.handleMouse` 左键分支

当前代码（`RegionEditorOverlay.java`，行 285-292）：

```java
if (button == MouseButton.LEFT) {
    return RegionSelection.selectTopmost(regions, dimension, worldPoint.x(), worldPoint.z())
            .map(region -> {
                session.select(region.id());
                return Action.SELECTED_REGION;
            })
            .orElse(Action.IGNORED);
}
```

改为：

```java
if (button == MouseButton.LEFT) {
    List<Region> stack = RegionSelection.selectStack(regions, dimension, worldPoint.x(), worldPoint.z());
    boolean selected = session.advanceSelection(stack, worldPoint.x(), worldPoint.z());
    return selected ? Action.SELECTED_REGION : Action.IGNORED;
}
```

- `advanceSelection` 返回 `true`（命中了区域）时返回 `SELECTED_REGION`；返回 `false`（未命中，已清空选中）时返回 `IGNORED`。
- 右键分支（行 293-295）不变：仍然依赖 `session.selectedRegionId().isPresent()` 打开上下文菜单，此时拿到的是当前循环到的那个区域。

### 2.2 平台代码改动范围

- neoforge 和 forge 的 `RegionEditorOverlay.ActionRouter.handleMouse` 改动完全一致（仅左键分支那几行）。
- `XaeroMapInputRouter` 是纯转发，无需改动。
- `XaeroMapOverlayController.handleMouse` 无需改动——它只负责拦截按钮/工具栏/右键菜单命中，其余转发给 ROUTER。

---

## 第三部分：右上角 HUD 渲染（common 截断计算 + 平台层渲染）

### 3.1 `SelectionHudText`（common 模块新增，纯计算可测试）

```java
package com.suian.xaeroregionsrev.client.editor;

import java.util.function.IntUnaryOperator;

/**
 * 选中 HUD 的显示文本计算。纯计算逻辑，不依赖 Minecraft API，可单元测试。
 */
public record SelectionHudText(String displayText, String fullText, boolean truncated) {

    /**
     * 构造 HUD 显示文本。
     *
     * @param label     区域名称
     * @param index     当前层（从 1 开始）
     * @param total     总层数
     * @param textWidth 文本宽度计算函数（对应 Font::width）
     * @param maxWidth  最大允许宽度（像素）
     */
    public static SelectionHudText of(String label, int index, int total,
                                       IntUnaryOperator textWidth, int maxWidth) { ... }
}
```

**格式规则**：

- `total <= 1`：无层数前缀，`fullText = label`，`displayText = label`（截断时加 `...`）
- `total >= 2`：`fullText = "{index}/{total}  {label}"`（层数与名称间 2 空格分隔）
- 截断只作用于名称部分，层数前缀（`2/5  `）始终完整保留
- 截断后 `displayText = "{prefix}{truncatedLabel}..."`，`truncated = true`

### 3.2 HUD 布局规格

```
                                            ┌────────┐
                                            │ [编辑] │  editButton (W-34, 12) 22×22
                                            └────────┘
                                          ┌──────────────┐
                                          │ 2/5  区域名称 │  SelectionHud
                                          └──────────────┘
```

| 属性 | 值 |
|------|-----|
| 右边缘 x | `screenWidth - EDIT_BUTTON_MARGIN`（= `screenWidth - 12`，与编辑按钮右边缘对齐） |
| 顶部 y | `EDIT_BUTTON_MARGIN + EDIT_BUTTON_HEIGHT + 6`（= `40`，按钮下方留 6px） |
| 宽度 | 自适应：`textWidth + padding * 2`（padding = 6） |
| 高度 | `font.lineHeight + 4 * 2`（上下各 4px） |
| 背景 | `0xAA111111`（半透明深色，复用 `RegionContextMenu` 风格） |
| 文字色 | `0xFFFFFFFF`（白色） |
| 最大宽度 | 160px（超出则截断名称部分） |

### 3.3 截断与 tooltip

**截断**：当 `displayText` 宽度超过 160px 时，截断名称部分，保留层数前缀完整，末尾加 `...`。

**tooltip**：仅在 `truncated == true` 且鼠标在 HUD 矩形内时，`graphics.renderTooltip(font, Component.literal(fullText), mouseX, mouseY)` 显示完整名称。未截断时不显示 tooltip（无额外信息）。

### 3.4 渲染注入点

`XaeroMapOverlayController.renderEditor` 新增一步，位于 `renderButton` 之后、`contextMenu.render()` 之前：

```java
public static void renderEditor(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
    if (SESSION.isEditing()) {
        RegionEditorOverlay.renderDraft(graphics, project(SESSION.draftPoints(), screen));
    }
    RegionEditorOverlay.renderToolbar(graphics, screen.width, screen.height, SESSION.isEditing(), mouseX, mouseY);
    RegionEditorOverlay.renderButton(graphics, screen.width, screen.height, SESSION.isEditing(), mouseX, mouseY);
    renderSelectionHud(graphics, screen, mouseX, mouseY);   // 新增
    if (contextMenu != null) {
        contextMenu.render(graphics);
    }
}
```

**触发条件**：`SESSION.selectionInfo().isPresent()` 时渲染。不限 `isEditing()`（用户要求"任何有选中时都显示"）。由于 `renderEditor` 已在 `renderRegions` 末尾被无条件调用，HUD 会随选中状态自然出现/消失。

### 3.5 `renderSelectionHud` 方法（平台层，neoforge + forge 同步）

```java
private static void renderSelectionHud(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
    Optional<RegionEditSession.SelectionInfo> info = SESSION.selectionInfo();
    if (info.isEmpty()) {
        return;
    }
    // 从缓存按 id 查找 label（见 1.6 方案 a）
    String label = ClientRegionCache.regions().stream()
            .filter(r -> r.id().equals(info.get().id()))
            .map(Region::label)
            .findFirst()
            .orElse(info.get().id().value());  // 区域已被删除时回退显示 id

    Font font = Minecraft.getInstance().font;
    SelectionHudText text = SelectionHudText.of(
            label, info.get().index(), info.get().total(), font::width, 160);

    int textWidth = font.width(text.displayText());
    int padding = 6;
    int hudWidth = textWidth + padding * 2;
    int hudHeight = font.lineHeight + 4 * 2;
    int rightEdge = screen.width - RegionEditorOverlay.EDIT_BUTTON_MARGIN;
    int hudX = rightEdge - hudWidth;
    int hudY = RegionEditorOverlay.EDIT_BUTTON_MARGIN + RegionEditorOverlay.EDIT_BUTTON_HEIGHT + 6;

    graphics.fill(hudX, hudY, hudX + hudWidth, hudY + hudHeight, 0xAA111111);
    graphics.drawString(font, text.displayText(), hudX + padding, hudY + 4, 0xFFFFFFFF, false);

    RegionEditorOverlay.Rect hudBounds = new RegionEditorOverlay.Rect(hudX, hudY, hudWidth, hudHeight);
    if (text.truncated() && hudBounds.contains(mouseX, mouseY)) {
        graphics.renderTooltip(font, Component.literal(text.fullText()), mouseX, mouseY);
    }
}
```

### 3.6 lang 文本

无需新增 lang key——HUD 显示的区域名称来自 `Region.label()`，层数是纯数字，tooltip 直接显示 label 原文。

---

## 测试策略

### common 模块纯 Java 测试（不依赖游戏环境）

**`RegionSelectionTest` 扩展**：

- `selectStack` 返回所有命中区域，顺序与渲染顺序一致。
- `selectStack` 在无命中时返回空列表。
- `selectStack` 忽略其它维度的区域。
- `selectTopmost` 改为基于 `selectStack` 后仍返回最后一个命中区域（保留原测试）。

**`RegionEditSession` 选中状态机测试（新增 `RegionEditSessionSelectionTest`）**：

- 首次点击命中 1 个区域 → 选中它，`selectionInfo` 返回 `index=1, total=1`。
- 首次点击命中 3 个区域 → 选中顶层（`index=1, total=3`）。
- 同一组候选连续点击 → `index` 依次推进 `1 → 2 → 3 → 1`（循环回顶）。
- 命中不同候选组 → 重新从顶层开始（`index=1`）。
- 未命中任何区域 → 清空选中，`selectionInfo` 为空。
- `select(RegionId)` 直接指定 → 候选栈为单元素，下次 `advanceSelection` 重新开始。
- `clearSelection()` → `candidateStack`、`selectionIndex`、`lastClickX/Z` 全部清空。
- `setEditing(false)` / `reset()` → 清空选中状态。

**`SelectionHudTextTest`（新增）**：

- `total <= 1` 时无层数前缀，`displayText = label`。
- `total >= 2` 时 `displayText = "2/5  名称"`。
- 名称超过最大宽度时截断，保留层数前缀完整，末尾 `...`，`truncated = true`。
- 未超宽时 `truncated = false`。
- 空名称边界情况。

### 平台层

平台层的改动是薄薄的转发和渲染调用，核心逻辑已被 common 测试覆盖。平台层测试不强制要求新增（现有 `ActionRouterTest` / overlay 测试如有则同步更新左键分支断言）。

---

## 改动文件清单

| 模块 | 文件 | 改动类型 |
|------|------|---------|
| common | `client/editor/RegionSelection.java` | 新增 `selectStack`，`selectTopmost` 改为基于它 |
| common | `client/editor/RegionEditSession.java` | 新增候选栈字段、`advanceSelection`、`SelectionInfo`、扩展 `clearSelection` |
| common | `client/editor/SelectionHudText.java` | **新增**，HUD 文本截断计算 |
| common | `test/.../client/editor/RegionSelectionTest.java` | 扩展 `selectStack` 测试 |
| common | `test/.../client/editor/RegionEditSessionSelectionTest.java` | **新增**，状态机测试 |
| common | `test/.../client/editor/SelectionHudTextTest.java` | **新增**，HUD 文本测试 |
| neoforge | `client/editor/RegionEditorOverlay.java` | `ActionRouter.handleMouse` 左键分支改用 `selectStack` + `advanceSelection` |
| neoforge | `client/xaero/XaeroMapOverlayController.java` | 新增 `renderSelectionHud`，`renderEditor` 中调用 |
| forge | `client/editor/RegionEditorOverlay.java` | 同 neoforge |
| forge | `client/xaero/XaeroMapOverlayController.java` | 同 neoforge |

---

## 非目标（YAGNI）

以下不在本次范围内：

- **禁止区域重叠**：当前设计明确允许重叠，不做几何重叠检测或拒绝。
- **显式图层/z-order 字段**：绘制顺序仍由 `LinkedHashMap` 插入顺序决定，不给 `Region` 加 z-order 字段。
- **反向循环（Shift+点击）**：当前仅支持顶层→底层单向循环。
- **HUD 可拖动/可配置位置**：HUD 固定在编辑按钮下方。
- **非编辑模式下的选中功能**：虽然 HUD 在"任何有选中时"都显示，但当前选中操作的触发条件仍是编辑模式下的左键点击（`session.isEditing()`）。
