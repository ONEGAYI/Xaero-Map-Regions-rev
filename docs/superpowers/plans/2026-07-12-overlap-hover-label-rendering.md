# 重叠区域悬停气泡与标签避让 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将重叠区域的多个悬停气泡合并为一个有容量边界、彩色描边的气泡，并在整帧范围内消除区域行内标签碰撞。

**Architecture:** 在 `common` 中新增两个纯 Java 布局单元：一个负责气泡排序、容量裁剪和描边颜色选择，一个负责标签矩形碰撞过滤。NeoForge 1.21.1 与 Forge 1.20.1 的渲染器改为“先收集整帧候选、再统一布局并绘制”，平台侧各自用同签名的 Minecraft `Font.drawInBatch8xOutline(...)` 和 `TooltipRenderUtil` 完成薄适配。

**Tech Stack:** Java 17/21、JUnit 5、Gradle 多项目、Minecraft 1.20.1/1.21.1 Mojang mappings、Forge 47.3.33、NeoForge 21.1.235、JOML。

## Global Constraints

- `common` 必须保持纯 Java 17，不得引用任何 Minecraft、Forge 或 NeoForge 类型。
- NeoForge 1.21.1 与 Forge 1.20.1 必须保持相同显示语义；平台差异只留在 GUI 绘制适配层。
- 气泡硬上限为 8 行；超过容量时最后一行显示本地化的 `……还有 N 个区域`，极小窗口至少保留最高优先级区域。
- 当前选中区域优先，其余区域按现有渲染顺序逆序（顶层到底层）。
- 气泡区域行使用 `labelColor & 0x00FFFFFF`，不得继承 alpha；描边只能是不透明黑色或白色，并选择与文字对比度更高者。
- 行内标签碰撞时保留当前选中区域或更高层区域；不得移动标签锚点。
- 不改变区域填充、边界、选中轮廓、点击命中和循环选中状态机。
- 实现逻辑必须遵循 TDD；每个逻辑任务先看到目标测试失败，再写最小实现。
- 提交信息使用中文 `类型: 简述`，并包含说明“做了什么、为什么做”的正文。

---

## 文件结构与职责

- Create: `common/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionHoverTooltipLayout.java`
  - 纯 Java 气泡候选排序、行数裁剪、遗漏计数、RGB 规范化和黑白描边选择。
- Create: `common/src/test/java/com/suian/xaeroregionsrev/client/xaero/RegionHoverTooltipLayoutTest.java`
  - 气泡排序、硬/自适应上限、极小窗口、alpha 丢弃和对比度契约测试。
- Create: `common/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionLabelCollisionLayout.java`
  - 纯 Java 标签候选优先级排序、带安全间距的矩形碰撞过滤。
- Create: `common/src/test/java/com/suian/xaeroregionsrev/client/xaero/RegionLabelCollisionLayoutTest.java`
  - 标签碰撞、接触边界、选择优先和稳定顺序契约测试。
- Modify: `common/src/main/resources/assets/xaeroregionsrev/lang/zh_cn.json`
- Modify: `common/src/main/resources/assets/xaeroregionsrev/lang/en_us.json`
  - 气泡遗漏汇总文本。
- Modify: `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java`
- Modify: `forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java`
  - 将即时标签/气泡绘制拆为候选计算、标签绘制和悬停判断。
- Modify: `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayRenderer.java`
- Modify: `forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayRenderer.java`
  - 整帧候选收集、公共布局调用和最终绘制编排。
- Create: `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionTooltipRenderer.java`
- Create: `forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionTooltipRenderer.java`
  - 原生风格气泡外框、位置避让和八方向描边文字绘制。
- Modify: `AGENTS.md`
  - 更新 `common/client/xaero` 与平台客户端渲染职责摘要。

---

### Task 1: 公共悬停气泡布局与描边颜色

**Files:**
- Create: `common/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionHoverTooltipLayout.java`
- Create: `common/src/test/java/com/suian/xaeroregionsrev/client/xaero/RegionHoverTooltipLayoutTest.java`
- Modify: `common/src/main/resources/assets/xaeroregionsrev/lang/zh_cn.json`
- Modify: `common/src/main/resources/assets/xaeroregionsrev/lang/en_us.json`

**Interfaces:**
- Consumes: `RegionId`；输入 `List<Candidate>` 的顺序为底层到顶层。
- Produces:
  - `Candidate(RegionId id, String label, int labelArgb)`
  - sealed `Row`，实现为 `RegionRow(Candidate candidate, int textRgb, int outlineArgb)` 与 `OverflowRow(int hiddenCount)`
  - `Layout(List<Row> rows, int hiddenCount)`
  - `layout(List<Candidate>, Optional<RegionId>, int screenHeight, int rowHeight)`
  - `outlineArgbForRgb(int rgb)`

- [ ] **Step 1: 写气泡排序、裁剪与颜色失败测试**

创建 `RegionHoverTooltipLayoutTest.java`，测试主体必须包含以下用例：

```java
package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.region.RegionId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RegionHoverTooltipLayoutTest {
    @Test
    void ordersSelectedFirstThenTopToBottom() {
        var bottom = candidate("bottom", 0xFF112233);
        var middle = candidate("middle", 0xFF445566);
        var top = candidate("top", 0xFF778899);

        var layout = RegionHoverTooltipLayout.layout(
                List.of(bottom, middle, top), Optional.of(middle.id()), 240, 11);

        assertEquals(List.of("middle", "top", "bottom"), regionIds(layout));
    }

    @Test
    void keepsAtMostEightRowsAndUsesLastRowForOverflow() {
        List<RegionHoverTooltipLayout.Candidate> candidates = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            candidates.add(candidate("r" + index, 0xFFFFFFFF));
        }

        var layout = RegionHoverTooltipLayout.layout(candidates, Optional.empty(), 1000, 11);

        assertEquals(8, layout.rows().size());
        assertEquals(List.of("r11", "r10", "r9", "r8", "r7", "r6", "r5"), regionIds(layout));
        assertEquals(5, assertInstanceOf(
                RegionHoverTooltipLayout.OverflowRow.class, layout.rows().get(7)).hiddenCount());
        assertEquals(5, layout.hiddenCount());
    }

    @Test
    void screenHeightCanReduceCapacityBelowHardLimit() {
        var layout = RegionHoverTooltipLayout.layout(
                List.of(candidate("a", -1), candidate("b", -1), candidate("c", -1), candidate("d", -1)),
                Optional.empty(), 49, 11);

        assertEquals(3, layout.rows().size());
        assertEquals(List.of("d", "c"), regionIds(layout));
        assertEquals(2, assertInstanceOf(
                RegionHoverTooltipLayout.OverflowRow.class, layout.rows().get(2)).hiddenCount());
    }

    @Test
    void tinyScreenKeepsHighestPriorityRegionWithoutOverflowRow() {
        var layout = RegionHoverTooltipLayout.layout(
                List.of(candidate("bottom", -1), candidate("top", -1)),
                Optional.empty(), 8, 11);

        assertEquals(List.of("top"), regionIds(layout));
        assertEquals(1, layout.hiddenCount());
    }

    @Test
    void regionRowDropsAlphaAndChoosesContrastingOutline() {
        var darkLayout = RegionHoverTooltipLayout.layout(
                List.of(candidate("dark", 0x00101010)), Optional.empty(), 240, 11);
        var lightLayout = RegionHoverTooltipLayout.layout(
                List.of(candidate("light", 0x80F0F0F0)), Optional.empty(), 240, 11);

        var dark = assertInstanceOf(RegionHoverTooltipLayout.RegionRow.class, darkLayout.rows().get(0));
        var light = assertInstanceOf(RegionHoverTooltipLayout.RegionRow.class, lightLayout.rows().get(0));
        assertEquals(0x101010, dark.textRgb());
        assertEquals(0xFFFFFFFF, dark.outlineArgb());
        assertEquals(0xF0F0F0, light.textRgb());
        assertEquals(0xFF000000, light.outlineArgb());
    }

    @Test
    void contrastBoundaryUsesTheHigherBlackOrWhiteRatio() {
        assertEquals(0xFFFFFFFF, RegionHoverTooltipLayout.outlineArgbForRgb(0x757575));
        assertEquals(0xFF000000, RegionHoverTooltipLayout.outlineArgbForRgb(0x767676));
    }

    private static RegionHoverTooltipLayout.Candidate candidate(String id, int color) {
        return new RegionHoverTooltipLayout.Candidate(new RegionId(id), id, color);
    }

    private static List<String> regionIds(RegionHoverTooltipLayout.Layout layout) {
        return layout.rows().stream()
                .filter(RegionHoverTooltipLayout.RegionRow.class::isInstance)
                .map(RegionHoverTooltipLayout.RegionRow.class::cast)
                .map(row -> row.candidate().id().value())
                .toList();
    }
}
```

- [ ] **Step 2: 运行新测试并确认红灯**

Run: `.\gradlew.bat :common:test --tests "*RegionHoverTooltipLayoutTest"`

Expected: FAIL，编译错误指出 `RegionHoverTooltipLayout` 不存在。

- [ ] **Step 3: 写最小气泡布局实现**

创建 `RegionHoverTooltipLayout.java`：

```java
package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.region.RegionId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RegionHoverTooltipLayout {
    public static final int MAX_ROWS = 8;
    private static final int VERTICAL_SAFE_MARGIN = 16;

    private RegionHoverTooltipLayout() {
    }

    public record Candidate(RegionId id, String label, int labelArgb) {
        public Candidate {
            Objects.requireNonNull(id, "Region id cannot be null.");
            Objects.requireNonNull(label, "Region label cannot be null.");
        }
    }

    public sealed interface Row permits RegionRow, OverflowRow {
    }

    public record RegionRow(Candidate candidate, int textRgb, int outlineArgb) implements Row {
    }

    public record OverflowRow(int hiddenCount) implements Row {
    }

    public record Layout(List<Row> rows, int hiddenCount) {
        public Layout {
            rows = List.copyOf(rows);
        }
    }

    public static Layout layout(List<Candidate> candidates, Optional<RegionId> selectedId,
                                int screenHeight, int rowHeight) {
        Objects.requireNonNull(candidates, "Candidates cannot be null.");
        Objects.requireNonNull(selectedId, "Selected id cannot be null.");
        if (candidates.isEmpty()) {
            return new Layout(List.of(), 0);
        }

        List<Candidate> prioritized = prioritize(candidates, selectedId);
        int safeRowHeight = Math.max(1, rowHeight);
        int heightCapacity = Math.max(1, (Math.max(0, screenHeight - VERTICAL_SAFE_MARGIN)) / safeRowHeight);
        int capacity = Math.min(MAX_ROWS, heightCapacity);
        boolean overflows = prioritized.size() > capacity;
        int detailCount = overflows && capacity > 1 ? capacity - 1 : capacity;
        int hiddenCount = prioritized.size() - detailCount;

        List<Row> rows = new ArrayList<>(capacity);
        for (int index = 0; index < detailCount; index++) {
            Candidate candidate = prioritized.get(index);
            int rgb = candidate.labelArgb() & 0x00FFFFFF;
            rows.add(new RegionRow(candidate, rgb, outlineArgbForRgb(rgb)));
        }
        if (hiddenCount > 0 && capacity > 1) {
            rows.add(new OverflowRow(hiddenCount));
        }
        return new Layout(rows, hiddenCount);
    }

    public static int outlineArgbForRgb(int rgb) {
        double luminance = relativeLuminance(rgb & 0x00FFFFFF);
        double blackContrast = (luminance + 0.05D) / 0.05D;
        double whiteContrast = 1.05D / (luminance + 0.05D);
        return blackContrast >= whiteContrast ? 0xFF000000 : 0xFFFFFFFF;
    }

    private static List<Candidate> prioritize(List<Candidate> candidates, Optional<RegionId> selectedId) {
        List<Candidate> prioritized = new ArrayList<>(candidates.size());
        selectedId.flatMap(id -> candidates.stream().filter(candidate -> candidate.id().equals(id)).findFirst())
                .ifPresent(prioritized::add);
        for (int index = candidates.size() - 1; index >= 0; index--) {
            Candidate candidate = candidates.get(index);
            if (selectedId.isEmpty() || !candidate.id().equals(selectedId.get())) {
                prioritized.add(candidate);
            }
        }
        return prioritized;
    }

    private static double relativeLuminance(int rgb) {
        return 0.2126D * linearChannel((rgb >>> 16) & 0xFF)
                + 0.7152D * linearChannel((rgb >>> 8) & 0xFF)
                + 0.0722D * linearChannel(rgb & 0xFF);
    }

    private static double linearChannel(int channel) {
        double value = channel / 255.0D;
        return value <= 0.04045D ? value / 12.92D : Math.pow((value + 0.055D) / 1.055D, 2.4D);
    }
}
```

- [ ] **Step 4: 运行测试并确认绿灯**

Run: `.\gradlew.bat :common:test --tests "*RegionHoverTooltipLayoutTest"`

Expected: PASS；`0x757575` 选择白色描边，`0x767676` 选择黑色描边，证明实现使用实际对比度分界而非粗略亮度阈值。

- [ ] **Step 5: 增加遗漏汇总语言键**

在两个 JSON 文件现有 Tooltip 键区域加入：

```json
// zh_cn.json（实际 JSON 中不要保留注释）
"tooltip.xaeroregionsrev.more_regions": "……还有 %s 个区域"

// en_us.json（实际 JSON 中不要保留注释）
"tooltip.xaeroregionsrev.more_regions": "…and %s more regions"
```

Run: `.\gradlew.bat :common:processResources`

Expected: BUILD SUCCESSFUL，两个 JSON 均可被 Gradle 处理。

- [ ] **Step 6: 提交公共气泡布局**

```powershell
git add common/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionHoverTooltipLayout.java common/src/test/java/com/suian/xaeroregionsrev/client/xaero/RegionHoverTooltipLayoutTest.java common/src/main/resources/assets/xaeroregionsrev/lang/zh_cn.json common/src/main/resources/assets/xaeroregionsrev/lang/en_us.json
git commit -m "fix: 合并重叠区域悬停气泡布局" -m "- 按选中区域和渲染层级稳定排序气泡候选`n- 设置八行硬上限、自适应高度和遗漏汇总`n- 忽略标签透明度并按实际对比度选择黑白描边"
```

---

### Task 2: 公共行内标签碰撞布局

**Files:**
- Create: `common/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionLabelCollisionLayout.java`
- Create: `common/src/test/java/com/suian/xaeroregionsrev/client/xaero/RegionLabelCollisionLayoutTest.java`

**Interfaces:**
- Consumes: 底层到顶层排列的 `List<Candidate>` 与可选选中区域 ID。
- Produces:
  - `Candidate(RegionId id, String text, int textArgb, int x, int y, int width, int height)`
  - `visibleCandidates(List<Candidate>, Optional<RegionId>, int padding)`，结果按接受优先级排列。

- [ ] **Step 1: 写标签碰撞失败测试**

创建 `RegionLabelCollisionLayoutTest.java`：

```java
package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.region.RegionId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionLabelCollisionLayoutTest {
    @Test
    void keepsNonOverlappingLabelsInTopToBottomPriorityOrder() {
        var bottom = candidate("bottom", 0, 0, 20, 10);
        var top = candidate("top", 40, 0, 20, 10);

        assertEquals(List.of("top", "bottom"), ids(
                RegionLabelCollisionLayout.visibleCandidates(List.of(bottom, top), Optional.empty(), 2)));
    }

    @Test
    void overlappingTopLabelSuppressesBottomLabel() {
        var bottom = candidate("bottom", 0, 0, 20, 10);
        var top = candidate("top", 10, 0, 20, 10);

        assertEquals(List.of("top"), ids(
                RegionLabelCollisionLayout.visibleCandidates(List.of(bottom, top), Optional.empty(), 0)));
    }

    @Test
    void selectedLabelWinsCollisionEvenWhenItIsBottomLayer() {
        var bottom = candidate("bottom", 0, 0, 20, 10);
        var top = candidate("top", 0, 0, 20, 10);

        assertEquals(List.of("bottom"), ids(RegionLabelCollisionLayout.visibleCandidates(
                List.of(bottom, top), Optional.of(bottom.id()), 2)));
    }

    @Test
    void safetyPaddingTreatsNearLabelsAsColliding() {
        var left = candidate("left", 0, 0, 20, 10);
        var right = candidate("right", 24, 0, 20, 10);

        assertEquals(List.of("right"), ids(
                RegionLabelCollisionLayout.visibleCandidates(List.of(left, right), Optional.empty(), 2)));
    }

    private static RegionLabelCollisionLayout.Candidate candidate(
            String id, int x, int y, int width, int height) {
        return new RegionLabelCollisionLayout.Candidate(
                new RegionId(id), id, 0xFFFFFFFF, x, y, width, height);
    }

    private static List<String> ids(List<RegionLabelCollisionLayout.Candidate> candidates) {
        return candidates.stream().map(candidate -> candidate.id().value()).toList();
    }
}
```

- [ ] **Step 2: 运行新测试并确认红灯**

Run: `.\gradlew.bat :common:test --tests "*RegionLabelCollisionLayoutTest"`

Expected: FAIL，编译错误指出 `RegionLabelCollisionLayout` 不存在。

- [ ] **Step 3: 写最小碰撞过滤实现**

创建 `RegionLabelCollisionLayout.java`：

```java
package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.region.RegionId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RegionLabelCollisionLayout {
    private RegionLabelCollisionLayout() {
    }

    public record Candidate(RegionId id, String text, int textArgb,
                            int x, int y, int width, int height) {
        public Candidate {
            Objects.requireNonNull(id, "Region id cannot be null.");
            Objects.requireNonNull(text, "Label text cannot be null.");
        }
    }

    public static List<Candidate> visibleCandidates(List<Candidate> candidates,
                                                     Optional<RegionId> selectedId, int padding) {
        Objects.requireNonNull(candidates, "Candidates cannot be null.");
        Objects.requireNonNull(selectedId, "Selected id cannot be null.");
        int safePadding = Math.max(0, padding);
        List<Candidate> accepted = new ArrayList<>();
        List<Rect> occupied = new ArrayList<>();
        for (Candidate candidate : prioritize(candidates, selectedId)) {
            Rect bounds = Rect.of(candidate, safePadding);
            if (occupied.stream().noneMatch(bounds::intersects)) {
                accepted.add(candidate);
                occupied.add(bounds);
            }
        }
        return List.copyOf(accepted);
    }

    private static List<Candidate> prioritize(List<Candidate> candidates, Optional<RegionId> selectedId) {
        List<Candidate> prioritized = new ArrayList<>(candidates.size());
        selectedId.flatMap(id -> candidates.stream().filter(candidate -> candidate.id().equals(id)).findFirst())
                .ifPresent(prioritized::add);
        for (int index = candidates.size() - 1; index >= 0; index--) {
            Candidate candidate = candidates.get(index);
            if (selectedId.isEmpty() || !candidate.id().equals(selectedId.get())) {
                prioritized.add(candidate);
            }
        }
        return prioritized;
    }

    private record Rect(int left, int top, int right, int bottom) {
        static Rect of(Candidate candidate, int padding) {
            return new Rect(candidate.x() - padding, candidate.y() - padding,
                    candidate.x() + candidate.width() + padding,
                    candidate.y() + candidate.height() + padding);
        }

        boolean intersects(Rect other) {
            return left <= other.right && right >= other.left
                    && top <= other.bottom && bottom >= other.top;
        }
    }
}
```

- [ ] **Step 4: 运行公共测试并确认绿灯**

Run: `.\gradlew.bat :common:test`

Expected: BUILD SUCCESSFUL，新增碰撞测试与现有 common 契约测试全部通过。

- [ ] **Step 5: 提交标签碰撞布局**

```powershell
git add common/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionLabelCollisionLayout.java common/src/test/java/com/suian/xaeroregionsrev/client/xaero/RegionLabelCollisionLayoutTest.java
git commit -m "fix: 避让重叠区域行内标签" -m "- 按当前选中区域和渲染层级过滤标签候选`n- 使用带安全间距的屏幕矩形检测视觉碰撞`n- 保持原标签锚点，碰撞时仅隐藏低优先级标签"
```

---

### Task 3: 双平台整帧编排与描边气泡绘制

**Files:**
- Modify: `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java:98-103,209-237`
- Modify: `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayRenderer.java:42-59`
- Create: `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionTooltipRenderer.java`
- Modify: `forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java:98-103,209-237`
- Modify: `forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayRenderer.java:47-64`
- Create: `forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionTooltipRenderer.java`

**Interfaces:**
- Consumes: Task 1 的 `RegionHoverTooltipLayout`、Task 2 的 `RegionLabelCollisionLayout`。
- Produces:
  - `XaeroMapOverlayController.createLabelCandidate(...)`
  - `XaeroMapOverlayController.renderInlineLabel(...)`
  - `XaeroMapOverlayController.isHovered(...)`
  - `RegionTooltipRenderer.render(GuiGraphics, Screen, RegionHoverTooltipLayout.Layout, int, int)`

- [ ] **Step 1: 先编译双平台基线**

Run: `.\gradlew.bat :neoforge:mc-1.21.1:compileJava :forge:mc-1.20.1:compileJava`

Expected: BUILD SUCCESSFUL。该结果记录接线前基线；平台绘制是 Minecraft GUI 薄适配，核心行为已由 Task 1/2 的失败优先契约测试覆盖。

- [ ] **Step 2: 将控制器拆成装饰、候选和绘制三个职责**

在两个平台的控制器中，将 `renderRegionDecorations` 改为不再即时调用标签方法，并用以下三个公开方法替换原私有 `renderLabel`：

```java
public static void renderRegionDecorations(GuiGraphics graphics, Region region, List<Vector2f> projected) {
    renderBoundary(graphics, region, projected);
    renderSelectedOutline(graphics, region, projected);
}

public static Optional<RegionLabelCollisionLayout.Candidate> createLabelCandidate(
        Screen screen, Region region, List<Vector2f> projected) {
    List<RegionEditorOverlay.ScreenPoint> points = projected.stream()
            .map(point -> new RegionEditorOverlay.ScreenPoint(point.x(), point.y()))
            .toList();
    var font = Minecraft.getInstance().font;
    Optional<RegionLabelDisplay.InlineLabel> inlineLabel = RegionLabelDisplay.layoutInlineLabel(
            region.label(), points, font.lineHeight, font::width);
    if (inlineLabel.isEmpty()) {
        return Optional.empty();
    }
    RegionLabelDisplay.InlineLabel label = inlineLabel.get();
    int labelWidth = font.width(label.text());
    if (label.x() + labelWidth < 0 || label.y() + font.lineHeight < 0
            || label.x() > screen.width || label.y() > screen.height) {
        return Optional.empty();
    }
    return Optional.of(new RegionLabelCollisionLayout.Candidate(
            region.id(), label.text(), region.labelColor().value(),
            label.x(), label.y(), labelWidth, font.lineHeight));
}

public static void renderInlineLabel(GuiGraphics graphics, RegionLabelCollisionLayout.Candidate label) {
    graphics.drawString(Minecraft.getInstance().font, label.text(), label.x(), label.y(), label.textArgb(), true);
}

public static boolean isHovered(List<Vector2f> projected, int mouseX, int mouseY) {
    return RegionLabelDisplay.isHovered(projected.stream()
            .map(point -> new RegionEditorOverlay.ScreenPoint(point.x(), point.y()))
            .toList(), mouseX, mouseY);
}
```

同步移除控制器中只供旧气泡使用的 `Component` 调用不得影响确认删除屏幕等其他 `Component` 使用点。

- [ ] **Step 3: 新建双平台原生风格描边气泡绘制器**

在两个平台的对应路径各创建一份以下 `RegionTooltipRenderer.java`；内容应完全相同：

```java
package com.suian.xaeroregionsrev.client.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;

final class RegionTooltipRenderer {
    private static final int Z = 400;
    private static final int ROW_GAP = 2;
    private static final int FULL_BRIGHT = 0x00F000F0;

    private RegionTooltipRenderer() {
    }

    static void render(GuiGraphics graphics, Screen screen, RegionHoverTooltipLayout.Layout layout,
                       int mouseX, int mouseY) {
        if (layout.rows().isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        List<RenderedLine> lines = renderedLines(layout);
        int width = lines.stream().mapToInt(line -> font.width(line.text())).max().orElse(0);
        int height = lines.size() * font.lineHeight + Math.max(0, lines.size() - 1) * ROW_GAP;
        Vector2ic position = DefaultTooltipPositioner.INSTANCE.positionTooltip(
                screen.width, screen.height, mouseX, mouseY, width, height);

        graphics.flush();
        TooltipRenderUtil.renderTooltipBackground(graphics, position.x(), position.y(), width, height, Z);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, Z);
        int y = position.y();
        for (RenderedLine line : lines) {
            font.drawInBatch8xOutline(line.text(), position.x(), y, line.textRgb(), line.outlineArgb(),
                    graphics.pose().last().pose(), graphics.bufferSource(), FULL_BRIGHT);
            y += font.lineHeight + ROW_GAP;
        }
        graphics.flush();
        graphics.pose().popPose();
    }

    private static List<RenderedLine> renderedLines(RegionHoverTooltipLayout.Layout layout) {
        List<RenderedLine> lines = new ArrayList<>(layout.rows().size());
        for (RegionHoverTooltipLayout.Row row : layout.rows()) {
            if (row instanceof RegionHoverTooltipLayout.RegionRow regionRow) {
                lines.add(new RenderedLine(
                        Component.literal(regionRow.candidate().label()).getVisualOrderText(),
                        regionRow.textRgb(), regionRow.outlineArgb()));
            } else if (row instanceof RegionHoverTooltipLayout.OverflowRow overflowRow) {
                int rgb = 0xFFFFFF;
                lines.add(new RenderedLine(
                        Component.translatable("tooltip.xaeroregionsrev.more_regions", overflowRow.hiddenCount())
                                .getVisualOrderText(),
                        rgb, RegionHoverTooltipLayout.outlineArgbForRgb(rgb)));
            }
        }
        return List.copyOf(lines);
    }

    private record RenderedLine(FormattedCharSequence text, int textRgb, int outlineArgb) {
    }
}
```

- [ ] **Step 4: 将双平台渲染循环改为整帧收集和统一绘制**

在两个平台的 `XaeroMapOverlayRenderer` 增加 `ArrayList`、`Optional<RegionId>` 所需 import，并用以下主体替换 `renderRegions`；Forge 原有 `register()` 与事件类型保持不变：

```java
private static void renderRegions(GuiGraphics graphics, Screen screen, List<Region> regions, String currentDimension,
                                  int mouseX, int mouseY) {
    int renderedRegions = 0;
    List<RegionLabelCollisionLayout.Candidate> labelCandidates = new ArrayList<>();
    List<RegionHoverTooltipLayout.Candidate> hoveredCandidates = new ArrayList<>();
    Optional<RegionId> selectedId = XaeroMapOverlayController.session().selectedRegionId();

    for (Region region : regions) {
        if (region.points().size() < 3 || !region.dimension().equals(currentDimension)) {
            continue;
        }
        List<Vector2f> projected = project(region.points(), screen);
        if (!XaeroMapOverlayController.isProjectedRegionVisible(projected, screen.width, screen.height)) {
            continue;
        }
        PolygonFillRenderer.fill(graphics, projected, region.color().value());
        XaeroMapOverlayController.renderRegionDecorations(graphics, region, projected);
        XaeroMapOverlayController.createLabelCandidate(screen, region, projected).ifPresent(labelCandidates::add);
        if (XaeroMapOverlayController.isHovered(projected, mouseX, mouseY)) {
            hoveredCandidates.add(new RegionHoverTooltipLayout.Candidate(
                    region.id(), region.label(), region.labelColor().value()));
        }
        renderedRegions++;
    }

    RegionLabelCollisionLayout.visibleCandidates(labelCandidates, selectedId, 2)
            .forEach(label -> XaeroMapOverlayController.renderInlineLabel(graphics, label));
    int rowHeight = Minecraft.getInstance().font.lineHeight + 2;
    RegionTooltipRenderer.render(graphics, screen, RegionHoverTooltipLayout.layout(
            hoveredCandidates, selectedId, screen.height, rowHeight), mouseX, mouseY);
    XaeroMapOverlayController.renderEditor(graphics, screen, mouseX, mouseY);
    logRender(screen, regions.size(), renderedRegions, currentDimension);
}
```

- [ ] **Step 5: 编译并运行双平台测试**

Run: `.\gradlew.bat :neoforge:mc-1.21.1:test :forge:mc-1.20.1:test`

Expected: BUILD SUCCESSFUL。若 `TooltipRenderUtil` 或缓冲提交签名存在版本映射差异，只调整对应平台的薄适配；不得把 Minecraft 类型移入 common，也不得退回多次 `renderTooltip`。

- [ ] **Step 6: 比较并提交双平台接线**

先运行：

```powershell
git diff --no-index -- neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionTooltipRenderer.java forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionTooltipRenderer.java
```

Expected: 两份 `RegionTooltipRenderer` 无差异；若存在版本 API 必需差异，差异只限 import 或单个调用签名并附有原因注释。

```powershell
git add neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayRenderer.java neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionTooltipRenderer.java forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayRenderer.java forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/RegionTooltipRenderer.java
git commit -m "fix: 修复双平台重叠区域显示" -m "- 将区域标签和悬停信息改为整帧收集后统一布局`n- 只绘制一个有容量边界的原生风格气泡`n- 在两个目标版本中使用原生字体 API 添加自适应描边"
```

---

### Task 4: 文档同步、全量验证与人工烟测

**Files:**
- Modify: `AGENTS.md` 文件树中 `common/client/xaero`、NeoForge `client`、Forge 平台说明。
- Test: 全部公共、NeoForge、Forge 测试与构建任务。

**Interfaces:**
- Consumes: Task 1-3 的完整实现。
- Produces: 更新后的架构事实源、全量自动化验证证据和人工验收清单。

- [ ] **Step 1: 更新 AGENTS.md 文件树职责**

将 common 的 xaero 描述更新为：

```text
│       │   │   └── xaero/ 凹多边形三角化、渲染样式、悬停气泡布局与行内标签碰撞过滤。
```

将 NeoForge 客户端描述更新为：

```text
│           │   ├── client/ 客户端本地配置、payload 桥接、快捷键、Xaero 覆盖层、整帧标签避让/描边气泡与编辑器屏幕。
```

Forge 的平台实现说明追加一句：对应实现包含与 NeoForge 一致的整帧标签避让和描边气泡薄适配。

- [ ] **Step 2: 运行全量单元测试**

Run: `.\gradlew.bat :common:test :neoforge:mc-1.21.1:test :forge:mc-1.20.1:test`

Expected: BUILD SUCCESSFUL，三个子项目测试全部通过。

- [ ] **Step 3: 构建全部发布产物**

Run: `.\gradlew.bat buildAll`

Expected: BUILD SUCCESSFUL，NeoForge 与 Forge jar 均成功生成且 common 类被合并。

- [ ] **Step 4: 检查补丁格式与工作树范围**

Run:

```powershell
git diff --check
git status --short
git diff --stat HEAD~4..HEAD
```

Expected: `git diff --check` 无输出；状态只包含本任务预期的 `AGENTS.md`（若尚未提交）；最近提交与文件清单不含无关文件。

- [ ] **Step 5: 提交规则文件更新**

```powershell
git add AGENTS.md
git commit -m "docs: 更新重叠区域渲染架构说明" -m "- 补充 common 气泡布局和标签碰撞职责`n- 记录两个平台整帧标签避让与描边气泡薄适配`n- 使项目文件树与本次新增实现保持一致"
```

- [ ] **Step 6: 运行 NeoForge 客户端人工烟测**

Run: `.\gradlew.bat :neoforge:mc-1.21.1:runClient`

在 Xaero 世界地图执行：

1. 创建两个标签锚点接近、区域互相重叠的多边形。
2. 将一个标签设为深色、另一个设为浅色，并设置非 `FF` alpha。
3. 悬停重合区，确认只有一个气泡；深色文字为白描边、浅色文字为黑描边，文字本身不受 alpha 影响。
4. 创建或载入超过 8 个重合区域，确认气泡最多 8 行且最后一行为正确的遗漏计数。
5. 点击循环选中上下层，确认选中区域在气泡首行，碰撞标签优先显示选中区域。
6. 移开或缩放地图，确认不再碰撞的标签恢复，编辑按钮、HUD 与上下文菜单正常。

Expected: 六项全部通过；截图保存为验收证据但不提交临时运行目录。

- [ ] **Step 7: 条件允许时运行 Forge 客户端对等烟测**

Run: `.\gradlew.bat :forge:mc-1.20.1:runClient`

重复 Step 6 的核心场景：单气泡、遗漏汇总、黑白描边、标签避让和循环选中。

Expected: 与 NeoForge 用户可见行为一致。若本地运行环境阻塞 Forge 客户端，必须如实记录阻塞原因；自动化测试与 `buildAll` 仍必须通过。

- [ ] **Step 8: 最终状态确认**

Run: `git status --short; git log -5 --oneline`

Expected: 工作树干净；最近提交依次覆盖公共气泡布局、公共标签碰撞、双平台接线和规则文档更新。

---

## 完成定义

- 重合区每帧只绘制一个区域气泡。
- 气泡在屏幕高度和 8 行硬上限内显示，遗漏计数准确。
- 区域行文字使用去 alpha 的标签 RGB，并使用对比度更高的不透明黑/白八方向描边。
- 行内标签不再互相叠字，当前选中或更高层区域获胜。
- NeoForge 1.21.1 与 Forge 1.20.1 自动化测试、`buildAll` 和补丁格式检查通过。
- `AGENTS.md` 文件树与实际实现职责一致；人工烟测结果交由用户验收后才考虑推送或创建 PR。
