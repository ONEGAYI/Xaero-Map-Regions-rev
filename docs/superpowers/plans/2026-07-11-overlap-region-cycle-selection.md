# 重叠区域循环选中 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在编辑器地图上点击重叠区域时循环切换图层（顶层→底层→回顶），并在右上角显示当前选中的层数和区域名称 HUD。

**Architecture:** 核心状态机（候选堆栈 + 循环索引）放在 common 模块 `RegionEditSession`，纯 Java 可测试；平台层（neoforge + forge）只做薄转发和 HUD 渲染。HUD 文本截断计算（`SelectionHudText`）也在 common，保证双平台一致且可测试。

**Tech Stack:** Java 17（common/forge）、Java 21（neoforge）、JUnit 5、Minecraft 1.21.1 + NeoForge、Minecraft 1.20.1 + Forge

## Global Constraints

- common 模块是纯 Java 17 的 `java-library`，不依赖任何 Minecraft API；所有纯逻辑（状态机、几何命中、文本截断）必须放在 common 且可用 JUnit 5 测试。
- neoforge 与 forge 平台层的同名文件改动必须完全同步（仅 import 包名差异）。
- 绘制顺序仍由 `LinkedHashMap` 插入顺序决定，不给 `Region` 加 z-order 字段。
- `RegionSelection.selectTopmost` 的现有语义（取最后一个命中）和行为必须保持不变，已有测试 `selectsLastMatchingRegionWhenPolygonsOverlap` 继续通过。
- 测试命令：`./gradlew :common:test`（common）、`./gradlew :neoforge:mc-1.21.1:test`（neoforge）、`./gradlew :forge:mc-1.20.1:test`（forge）。
- 提交信息使用中文，采用 `类型: 简述` 格式。

---

## File Structure

| 模块 | 文件 | 责任 |
|------|------|------|
| common | `client/editor/RegionSelection.java` | 新增 `selectStack` 返回全部命中区域；`selectTopmost` 改为基于它 |
| common | `client/editor/RegionEditSession.java` | 新增候选堆栈字段、`advanceSelection` 状态机、`SelectionInfo` record、`selectionInfo()` |
| common | `client/editor/SelectionHudText.java` | **新增**，HUD 文本格式化与截断计算（纯计算） |
| common | `test/.../client/editor/RegionSelectionTest.java` | 扩展 `selectStack` 测试 |
| common | `test/.../client/editor/RegionEditSessionSelectionTest.java` | **新增**，选中状态机测试 |
| common | `test/.../client/editor/SelectionHudTextTest.java` | **新增**，HUD 文本截断测试 |
| neoforge | `client/editor/RegionEditorOverlay.java` | `ActionRouter.handleMouse` 左键分支改用 `selectStack` + `advanceSelection` |
| neoforge | `client/xaero/XaeroMapOverlayController.java` | 新增 `renderSelectionHud`，`renderEditor` 中调用 |
| forge | `client/editor/RegionEditorOverlay.java` | 同 neoforge |
| forge | `client/xaero/XaeroMapOverlayController.java` | 同 neoforge |

---

## Task 1: RegionSelection.selectStack（common 模块）

**Files:**
- Modify: `common/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionSelection.java`
- Test: `common/src/test/java/com/suian/xaeroregionsrev/client/editor/RegionSelectionTest.java`

**Interfaces:**
- Produces: `public static List<Region> selectStack(List<Region> regions, String dimension, int blockX, int blockZ)` — 返回所有命中区域，顺序与输入列表一致（靠后的 = 视觉顶层）。

- [ ] **Step 1: 在 RegionSelectionTest 新增 selectStack 测试**

在 `common/src/test/java/com/suian/xaeroregionsrev/client/editor/RegionSelectionTest.java` 中，在 `selectsLastMatchingRegionWhenPolygonsOverlap` 测试之后新增以下测试方法（复用文件末尾已有的 `region` 辅助方法）：

```java
@Test
void selectStackReturnsAllHitsInListOrder() {
    Region back = region("back", List.of(
            new RegionPoint(0, 0),
            new RegionPoint(20, 0),
            new RegionPoint(20, 20),
            new RegionPoint(0, 20)
    ));
    Region middle = region("middle", List.of(
            new RegionPoint(5, 5),
            new RegionPoint(15, 5),
            new RegionPoint(15, 15),
            new RegionPoint(5, 15)
    ));
    Region front = region("front", List.of(
            new RegionPoint(8, 8),
            new RegionPoint(12, 8),
            new RegionPoint(12, 12),
            new RegionPoint(8, 12)
    ));

    List<Region> stack = RegionSelection.selectStack(
            List.of(back, middle, front), "minecraft:overworld", 10, 10);

    assertEquals(List.of(new RegionId("back"), new RegionId("middle"), new RegionId("front")),
            stack.stream().map(Region::id).toList());
}

@Test
void selectStackReturnsEmptyWhenNoHit() {
    Region far = region("far", List.of(
            new RegionPoint(100, 100),
            new RegionPoint(120, 100),
            new RegionPoint(120, 120),
            new RegionPoint(100, 120)
    ));

    assertTrue(RegionSelection.selectStack(List.of(far), "minecraft:overworld", 10, 10).isEmpty());
}
```

需要在文件顶部添加 `import java.util.List;`（如果已有则跳过）。

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:test --tests "*RegionSelectionTest*"`
Expected: FAIL — `selectStack` 方法不存在，编译错误。

- [ ] **Step 3: 实现 selectStack 并重构 selectTopmost**

将 `common/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionSelection.java` 完整替换为：

```java
package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.PolygonMath;
import com.suian.xaeroregionsrev.region.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RegionSelection {
    private RegionSelection() {
    }

    public static Optional<Region> selectTopmost(List<Region> regions, String dimension, int blockX, int blockZ) {
        List<Region> stack = selectStack(regions, dimension, blockX, blockZ);
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.get(stack.size() - 1));
    }

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
}
```

- [ ] **Step 4: 运行测试确认全部通过**

Run: `./gradlew :common:test --tests "*RegionSelectionTest*"`
Expected: PASS — 包括原有的 `selectsLastMatchingRegionWhenPolygonsOverlap` 和 `ignoresRegionsFromOtherDimensions`。

- [ ] **Step 5: 运行 common 全量测试确认无回归**

Run: `./gradlew :common:test`
Expected: PASS — `selectTopmost` 重构未破坏其它已有测试。

- [ ] **Step 6: 提交**

```bash
git add common/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionSelection.java common/src/test/java/com/suian/xaeroregionsrev/client/editor/RegionSelectionTest.java
git commit -m "feat: RegionSelection 新增 selectStack 返回全部命中区域

- 新增 selectStack 方法，返回点击位置命中的所有区域列表，
  顺序与渲染顺序一致（靠后的为视觉顶层）
- selectTopmost 重构为基于 selectStack 取最后一个，保持原语义不变
- 为后续重叠区域循环切换功能提供基础"
```

---

## Task 2: RegionEditSession 选中状态机（common 模块）

**Files:**
- Modify: `common/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditSession.java`
- Test: `common/src/test/java/com/suian/xaeroregionsrev/client/editor/RegionEditSessionSelectionTest.java`（新增）

**Interfaces:**
- Consumes: `RegionSelection.selectStack`（Task 1 产出），`Region` 的 `id()`/`points()`/`dimension()`
- Produces:
  - `public boolean advanceSelection(List<Region> hitStack, int clickX, int clickZ)` — 推进选中状态机，返回是否命中区域
  - `public record SelectionInfo(RegionId id, int index, int total)` — 选中信息（index 从 1 开始）
  - `public Optional<SelectionInfo> selectionInfo()` — 查询当前选中信息

- [ ] **Step 1: 新建 RegionEditSessionSelectionTest 并编写第一批失败测试**

创建 `common/src/test/java/com/suian/xaeroregionsrev/client/editor/RegionEditSessionSelectionTest.java`：

```java
package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegionEditSessionSelectionTest {

    @Test
    void firstClickSingleHitSelectsIt() {
        RegionEditSession session = new RegionEditSession();
        Region r = region("solo", 0, 0, 20, 20);

        boolean result = session.advanceSelection(List.of(r), 10, 10);

        assertTrue(result);
        Optional<RegionEditSession.SelectionInfo> info = session.selectionInfo();
        assertTrue(info.isPresent());
        assertEquals(new RegionId("solo"), info.get().id());
        assertEquals(1, info.get().index());
        assertEquals(1, info.get().total());
    }

    @Test
    void firstClickMultipleHitsSelectsTopmost() {
        RegionEditSession session = new RegionEditSession();
        Region back = region("back", 0, 0, 20, 20);
        Region front = region("front", 5, 5, 15, 15);

        session.advanceSelection(List.of(back, front), 10, 10);

        assertEquals(new RegionId("front"), session.selectionInfo().orElseThrow().id());
        assertEquals(1, session.selectionInfo().orElseThrow().index());
        assertEquals(2, session.selectionInfo().orElseThrow().total());
    }

    @Test
    void sameStackAdvancesIndex() {
        RegionEditSession session = new RegionEditSession();
        Region back = region("back", 0, 0, 20, 20);
        Region front = region("front", 5, 5, 15, 15);
        List<Region> stack = List.of(back, front);

        // 第一次点击 → 顶层 front (index=1)
        session.advanceSelection(stack, 10, 10);
        assertEquals(new RegionId("front"), session.selectionInfo().orElseThrow().id());
        assertEquals(1, session.selectionInfo().orElseThrow().index());

        // 第二次点击同一组 → 底层 back (index=2)
        session.advanceSelection(stack, 12, 12);
        assertEquals(new RegionId("back"), session.selectionInfo().orElseThrow().id());
        assertEquals(2, session.selectionInfo().orElseThrow().index());

        // 第三次点击 → 循环回顶层 front (index=1)
        session.advanceSelection(stack, 8, 8);
        assertEquals(new RegionId("front"), session.selectionInfo().orElseThrow().id());
        assertEquals(1, session.selectionInfo().orElseThrow().index());
    }

    @Test
    void differentStackResetsToTopmost() {
        RegionEditSession session = new RegionEditSession();
        Region back = region("back", 0, 0, 20, 20);
        Region front = region("front", 5, 5, 15, 15);

        // 先在 [back, front] 组中推进到底层
        session.advanceSelection(List.of(back, front), 10, 10);
        session.advanceSelection(List.of(back, front), 10, 10);
        assertEquals(new RegionId("back"), session.selectionInfo().orElseThrow().id());

        // 切换到新的区域组 → 重新从顶层开始
        Region other = region("other", 100, 100, 120, 120);
        session.advanceSelection(List.of(other), 110, 110);
        assertEquals(new RegionId("other"), session.selectionInfo().orElseThrow().id());
        assertEquals(1, session.selectionInfo().orElseThrow().index());
    }

    @Test
    void emptyHitStackClearsSelection() {
        RegionEditSession session = new RegionEditSession();
        session.advanceSelection(List.of(region("r", 0, 0, 20, 20)), 10, 10);
        assertTrue(session.selectionInfo().isPresent());

        boolean result = session.advanceSelection(List.of(), 100, 100);

        assertFalse(result);
        assertTrue(session.selectionInfo().isEmpty());
        assertTrue(session.selectedRegionId().isEmpty());
    }

    @Test
    void selectByIdResetsCandidateStackToSingle() {
        RegionEditSession session = new RegionEditSession();
        Region back = region("back", 0, 0, 20, 20);
        Region front = region("front", 5, 5, 15, 15);

        // 先在重叠组中建立候选栈
        session.advanceSelection(List.of(back, front), 10, 10);
        assertEquals(2, session.selectionInfo().orElseThrow().total());

        // 直接 select → 候选栈变单元素
        session.select(new RegionId("external"));
        assertEquals(1, session.selectionInfo().orElseThrow().total());
        assertEquals(new RegionId("external"), session.selectedRegionId().orElseThrow());

        // 下次地图点击（即使是同一组区域）→ 视为新候选组，从顶层开始
        session.advanceSelection(List.of(back, front), 10, 10);
        assertEquals(2, session.selectionInfo().orElseThrow().total());
        assertEquals(new RegionId("front"), session.selectionInfo().orElseThrow().id());
        assertEquals(1, session.selectionInfo().orElseThrow().index());
    }

    @Test
    void clearSelectionClearsCandidateStack() {
        RegionEditSession session = new RegionEditSession();
        session.advanceSelection(List.of(region("back", 0, 0, 20, 20), region("front", 5, 5, 15, 15)), 10, 10);

        session.clearSelection();

        assertTrue(session.selectionInfo().isEmpty());
        assertTrue(session.selectedRegionId().isEmpty());
    }

    @Test
    void threeLayerCycleGoesTopToBottomAndBackToTop() {
        RegionEditSession session = new RegionEditSession();
        Region bottom = region("bottom", 0, 0, 30, 30);
        Region middle = region("middle", 0, 0, 30, 30);
        Region top = region("top", 0, 0, 30, 30);
        List<Region> stack = List.of(bottom, middle, top);

        session.advanceSelection(stack, 10, 10);
        RegionEditSession.SelectionInfo i1 = session.selectionInfo().orElseThrow();
        assertEquals(new RegionId("top"), i1.id());
        assertEquals(1, i1.index());

        session.advanceSelection(stack, 10, 10);
        RegionEditSession.SelectionInfo i2 = session.selectionInfo().orElseThrow();
        assertEquals(new RegionId("middle"), i2.id());
        assertEquals(2, i2.index());

        session.advanceSelection(stack, 10, 10);
        RegionEditSession.SelectionInfo i3 = session.selectionInfo().orElseThrow();
        assertEquals(new RegionId("bottom"), i3.id());
        assertEquals(3, i3.index());

        // 第 4 次必须回绕到顶层
        session.advanceSelection(stack, 10, 10);
        RegionEditSession.SelectionInfo i4 = session.selectionInfo().orElseThrow();
        assertEquals(new RegionId("top"), i4.id());
        assertEquals(1, i4.index());
    }

    @Test
    void sameIdSetDifferentOrderStillAdvances() {
        RegionEditSession session = new RegionEditSession();
        Region a = region("a", 0, 0, 30, 30);
        Region b = region("b", 0, 0, 30, 30);

        // 第一次：顺序 [a, b]（b 是顶层），顶层→底层反转后 candidateStack=[b, a]
        session.advanceSelection(List.of(a, b), 10, 10);
        RegionEditSession.SelectionInfo i1 = session.selectionInfo().orElseThrow();
        assertEquals(new RegionId("b"), i1.id());
        assertEquals(1, i1.index());

        // 第二次：服务端同步打乱顺序为 [b, a]，但 id 集合相同 → 推进（非重置）
        // 推进基于旧快照 candidateStack=[b,a]，index 0→1 → "a"
        session.advanceSelection(List.of(b, a), 10, 10);
        RegionEditSession.SelectionInfo i2 = session.selectionInfo().orElseThrow();
        assertEquals(2, i2.index());
        assertEquals(new RegionId("a"), i2.id());
    }

    @Test
    void selectionInfoEmptyOnFreshSession() {
        RegionEditSession session = new RegionEditSession();
        assertTrue(session.selectionInfo().isEmpty());
    }

    @Test
    void setEditingFalseClearsCandidateStack() {
        RegionEditSession session = new RegionEditSession();
        Region a = region("a", 0, 0, 20, 20);
        Region b = region("b", 5, 5, 15, 15);

        session.setEditing(true);
        session.advanceSelection(List.of(a, b), 10, 10);
        session.advanceSelection(List.of(a, b), 10, 10);
        assertEquals(2, session.selectionInfo().orElseThrow().index());

        session.setEditing(false);

        assertTrue(session.selectionInfo().isEmpty());
        assertTrue(session.selectedRegionId().isEmpty());

        // 重新进入编辑后，同一组应从顶层重新开始（证明候选栈彻底清理）
        session.setEditing(true);
        session.advanceSelection(List.of(a, b), 10, 10);
        RegionEditSession.SelectionInfo info = session.selectionInfo().orElseThrow();
        assertEquals(1, info.index());
        assertEquals(new RegionId("b"), info.id());
    }

    private static Region region(String id, int x1, int z1, int x2, int z2) {
        return new Region(
                new RegionId(id),
                id,
                "minecraft:overworld",
                new ArgbColor(0x6600FF00),
                id,
                new ArgbColor(0xFFFFFFFF),
                "default",
                "default",
                List.of(
                        new RegionPoint(x1, z1),
                        new RegionPoint(x2, z1),
                        new RegionPoint(x2, z2),
                        new RegionPoint(x1, z2)
                ),
                1L,
                1L
        );
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:test --tests "*RegionEditSessionSelectionTest*"`
Expected: FAIL — `advanceSelection`、`selectionInfo`、`SelectionInfo` 不存在，编译错误。

- [ ] **Step 3: 实现 RegionEditSession 状态机**

将 `common/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditSession.java` 的选中相关部分扩展。完整替换文件内容：

```java
package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.RegionLimits;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public final class RegionEditSession {
    private boolean editing;
    private final List<RegionPoint> draftPoints = new ArrayList<>();
    private final List<RegionPoint> redoPoints = new ArrayList<>();
    private RegionId selectedRegionId;
    private List<RegionId> candidateStack = List.of();
    private int selectionIndex = -1;

    public enum EscapeResult {
        CLEARED_DRAFT,
        EXITED_EDIT_MODE,
        IGNORED
    }

    public enum HistoryResult {
        CHANGED,
        IGNORED
    }

    public record SelectionInfo(RegionId id, int index, int total) {
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        if (this.editing == editing) {
            return;
        }
        this.editing = editing;
        if (!editing) {
            clearDraft();
            clearSelection();
        }
    }

    public void toggleEditing() {
        setEditing(!editing);
    }

    public boolean addDraftPoint(RegionPoint point) {
        if (!editing) {
            return false;
        }
        if (draftPoints.size() >= RegionLimits.MAX_POINTS_PER_REQUEST) {
            return false;
        }
        draftPoints.add(point);
        redoPoints.clear();
        return true;
    }

    public List<RegionPoint> draftPoints() {
        return List.copyOf(draftPoints);
    }

    public void clearDraft() {
        draftPoints.clear();
        redoPoints.clear();
    }

    public HistoryResult undoDraftPoint() {
        if (!editing || draftPoints.isEmpty()) {
            return HistoryResult.IGNORED;
        }
        redoPoints.add(draftPoints.remove(draftPoints.size() - 1));
        return HistoryResult.CHANGED;
    }

    public HistoryResult redoDraftPoint() {
        if (!editing || redoPoints.isEmpty() || draftPoints.size() >= RegionLimits.MAX_POINTS_PER_REQUEST) {
            return HistoryResult.IGNORED;
        }
        draftPoints.add(redoPoints.remove(redoPoints.size() - 1));
        return HistoryResult.CHANGED;
    }

    public boolean canSubmitDraft() {
        return editing && draftPoints.size() >= 3;
    }

    public EscapeResult handleEscape() {
        if (!draftPoints.isEmpty()) {
            clearDraft();
            return EscapeResult.CLEARED_DRAFT;
        }
        if (editing) {
            setEditing(false);
            return EscapeResult.EXITED_EDIT_MODE;
        }
        return EscapeResult.IGNORED;
    }

    public void select(RegionId regionId) {
        selectedRegionId = regionId;
        candidateStack = List.of(regionId);
        selectionIndex = 0;
    }

    public boolean advanceSelection(List<Region> hitStack, int clickX, int clickZ) {
        if (hitStack.isEmpty()) {
            clearSelection();
            return false;
        }
        // 反转为顶层→底层顺序（hitStack 靠后的为顶层）
        List<RegionId> reversedStack = new ArrayList<>();
        for (int i = hitStack.size() - 1; i >= 0; i--) {
            reversedStack.add(hitStack.get(i).id());
        }

        if (selectionIndex >= 0 && new HashSet<>(reversedStack).equals(new HashSet<>(candidateStack))) {
            // 同一组重叠区域 → 推进索引（基于已有 candidateStack 快照，不受新顺序影响）
            selectionIndex = (selectionIndex + 1) % candidateStack.size();
            selectedRegionId = candidateStack.get(selectionIndex);
        } else {
            // 新的候选组 → 从顶层开始
            candidateStack = Collections.unmodifiableList(reversedStack);
            selectionIndex = 0;
            selectedRegionId = candidateStack.get(0);
        }
        return true;
    }

    public void clearSelection() {
        selectedRegionId = null;
        candidateStack = List.of();
        selectionIndex = -1;
    }

    public Optional<RegionId> selectedRegionId() {
        return Optional.ofNullable(selectedRegionId);
    }

    public Optional<SelectionInfo> selectionInfo() {
        if (selectedRegionId == null || selectionIndex < 0) {
            return Optional.empty();
        }
        return Optional.of(new SelectionInfo(selectedRegionId, selectionIndex + 1, candidateStack.size()));
    }

    public void reset() {
        editing = false;
        clearDraft();
        clearSelection();
    }
}
```

- [ ] **Step 4: 运行测试确认全部通过**

Run: `./gradlew :common:test --tests "*RegionEditSessionSelectionTest*"`
Expected: PASS — 全部 11 个测试方法通过。

- [ ] **Step 5: 运行 common 全量测试确认无回归**

Run: `./gradlew :common:test`
Expected: PASS — 包括 RegionSelectionTest、RegionEditSessionSelectionTest 和所有已有测试。`RegionEditSessionTest` 中已有的 `select` 调用因不断言候选栈，新语义下仍通过。

- [ ] **Step 6: 提交**

```bash
git add common/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditSession.java common/src/test/java/com/suian/xaeroregionsrev/client/editor/RegionEditSessionSelectionTest.java
git commit -m "feat: RegionEditSession 新增重叠区域循环选中状态机

- 新增候选堆栈（candidateStack）与循环索引（selectionIndex），
  记录重叠区域的层级信息
- advanceSelection 状态机：同一组重叠区域连续点击循环切换
  图层（顶层→底层→回顶），不同候选组重置为顶层
- 同组判定基于 id 集合（HashSet）比较，对顺序不敏感，
  推进基于上次 candidateStack 快照保证体验稳定
- 新增 SelectionInfo record 与 selectionInfo() 查询方法，
  供 HUD 渲染层读取当前层数与总数
- select(RegionId) 现仅用于程序化指定选中，重置候选栈为单元素
- clearSelection/setEditing(false)/reset 同步清理新增字段"
```

---

## Task 3: SelectionHudText HUD 文本截断（common 模块）

**Files:**
- Create: `common/src/main/java/com/suian/xaeroregionsrev/client/editor/SelectionHudText.java`
- Test: `common/src/test/java/com/suian/xaeroregionsrev/client/editor/SelectionHudTextTest.java`（新增）

**Interfaces:**
- Produces: `public static SelectionHudText of(String label, int index, int total, ToIntFunction<String> textWidth, int maxWidth)` — 返回 `record SelectionHudText(String displayText, String fullText, boolean truncated)`

- [ ] **Step 1: 新建 SelectionHudTextTest 并编写失败测试**

创建 `common/src/test/java/com/suian/xaeroregionsrev/client/editor/SelectionHudTextTest.java`：

```java
package com.suian.xaeroregionsrev.client.editor;

import org.junit.jupiter.api.Test;

import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

class SelectionHudTextTest {

    // 简单的等宽模拟：每个字符宽度 = 6 像素
    private static final ToIntFunction<String> CHAR_WIDTH_6 = s -> s.length() * 6;

    @Test
    void singleLayerShowsLabelOnly() {
        SelectionHudText text = SelectionHudText.of("MyRegion", 1, 1, CHAR_WIDTH_6, 160);

        assertEquals("MyRegion", text.fullText());
        assertEquals("MyRegion", text.displayText());
        assertFalse(text.truncated());
    }

    @Test
    void multipleLayersShowsIndexPrefix() {
        SelectionHudText text = SelectionHudText.of("Castle", 2, 5, CHAR_WIDTH_6, 160);

        assertEquals("2/5  Castle", text.fullText());
        assertEquals("2/5  Castle", text.displayText());
        assertFalse(text.truncated());
    }

    @Test
    void truncatesLabelWhenExceedingMaxWidth() {
        // prefix "2/5  " = 5 chars = 30px, 省略号 "..." = 3 chars = 18px
        // maxWidth 60px → label 可用宽度 = 60 - 30 - 18 = 12px = 2 chars
        String longLabel = "AVeryLongRegionName";
        SelectionHudText text = SelectionHudText.of(longLabel, 2, 5, CHAR_WIDTH_6, 60);

        assertTrue(text.truncated());
        assertTrue(text.displayText().startsWith("2/5  "));
        assertTrue(text.displayText().endsWith("..."));
        assertEquals("2/5  AVeryLongRegionName", text.fullText());
    }

    @Test
    void singleLayerTruncatesLabelWhenExceedingMaxWidth() {
        // 单层无前缀，maxWidth 30px = 5 chars, "..." = 18px → label 2 chars
        SelectionHudText text = SelectionHudText.of("ABCDEFGH", 1, 1, CHAR_WIDTH_6, 30);

        assertTrue(text.truncated());
        assertTrue(text.displayText().endsWith("..."));
        assertEquals("ABCDEFGH", text.fullText());
    }

    @Test
    void notTruncatedWhenExactlyAtMaxWidth() {
        // "2/5  AB" = 7 chars = 42px, maxWidth 42 → 刚好不截断
        SelectionHudText text = SelectionHudText.of("AB", 2, 5, CHAR_WIDTH_6, 42);

        assertFalse(text.truncated());
        assertEquals("2/5  AB", text.displayText());
    }

    @Test
    void totalZeroOrNegativeShowsLabelOnlyNoPrefix() {
        SelectionHudText t0 = SelectionHudText.of("Name", 1, 0, CHAR_WIDTH_6, 160);
        assertEquals("Name", t0.fullText());
        assertEquals("Name", t0.displayText());
        assertFalse(t0.truncated());

        SelectionHudText tNeg = SelectionHudText.of("Name", 1, -1, CHAR_WIDTH_6, 160);
        assertEquals("Name", tNeg.fullText());
        assertFalse(tNeg.truncated());
    }

    @Test
    void emptyLabelProducesEmptyDisplay() {
        SelectionHudText single = SelectionHudText.of("", 1, 1, CHAR_WIDTH_6, 160);
        assertEquals("", single.fullText());
        assertEquals("", single.displayText());
        assertFalse(single.truncated());

        SelectionHudText multi = SelectionHudText.of("", 2, 5, CHAR_WIDTH_6, 160);
        assertEquals("2/5  ", multi.fullText());
        assertFalse(multi.truncated());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :common:test --tests "*SelectionHudTextTest*"`
Expected: FAIL — `SelectionHudText` 类不存在，编译错误。

- [ ] **Step 3: 实现 SelectionHudText**

创建 `common/src/main/java/com/suian/xaeroregionsrev/client/editor/SelectionHudText.java`：

```java
package com.suian.xaeroregionsrev.client.editor;

import java.util.function.ToIntFunction;

/**
 * 选中 HUD 的显示文本计算。纯计算逻辑，不依赖 Minecraft API，可单元测试。
 */
public record SelectionHudText(String displayText, String fullText, boolean truncated) {

    private static final String ELLIPSIS = "...";
    private static final String LAYER_SEPARATOR = "  ";

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
                                       ToIntFunction<String> textWidth, int maxWidth) {
        String prefix = total >= 2 ? index + "/" + total + LAYER_SEPARATOR : "";
        String fullText = prefix + label;

        if (textWidth.applyAsInt(fullText) <= maxWidth) {
            return new SelectionHudText(fullText, fullText, false);
        }

        // 截断 label 部分，保留 prefix 完整
        int ellipsisWidth = textWidth.applyAsInt(ELLIPSIS);
        String truncatedLabel = truncateToWidth(label, maxWidth - textWidth.applyAsInt(prefix) - ellipsisWidth, textWidth);
        String displayText = prefix + truncatedLabel + ELLIPSIS;
        return new SelectionHudText(displayText, fullText, true);
    }

    private static String truncateToWidth(String text, int availableWidth, ToIntFunction<String> textWidth) {
        if (availableWidth <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            if (textWidth.applyAsInt(sb.toString()) > availableWidth) {
                sb.deleteCharAt(sb.length() - 1);
                break;
            }
        }
        return sb.toString();
    }
}
```

- [ ] **Step 4: 运行测试确认全部通过**

Run: `./gradlew :common:test --tests "*SelectionHudTextTest*"`
Expected: PASS — 全部 7 个测试方法通过。

- [ ] **Step 5: 提交**

```bash
git add common/src/main/java/com/suian/xaeroregionsrev/client/editor/SelectionHudText.java common/src/test/java/com/suian/xaeroregionsrev/client/editor/SelectionHudTextTest.java
git commit -m "feat: 新增 SelectionHudText HUD 文本截断计算

- 纯计算 record，不依赖 Minecraft API，可单元测试
- total >= 2 时生成 \"index/total  label\" 格式，单层只显示 label
- 超出最大宽度时截断 label 部分，保留层数前缀完整，末尾加省略号
- 供平台层 HUD 渲染层调用，保证 neoforge 与 forge 双平台一致"
```

---

## Task 4: 平台层左键选中改造（neoforge + forge 同步）

**Files:**
- Modify: `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java:285-292`
- Modify: `forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java:285-292`

**Interfaces:**
- Consumes: `RegionSelection.selectStack`（Task 1）、`RegionEditSession.advanceSelection`（Task 2）

**说明：** neoforge 和 forge 的 `ActionRouter.handleMouse` 代码完全一致，以下改动两个文件都需执行。本任务为机械转发改造——核心状态机逻辑已由 Task 1+2 的 common 测试覆盖，平台层无新行为可测。如果存在 `ActionRouterTest` 则需同步更新左键分支断言，否则依赖 common 层覆盖即可。

- [ ] **Step 1: 修改 neoforge ActionRouter 左键分支**

在 `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java` 第 285-292 行，将：

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

替换为：

```java
            if (button == MouseButton.LEFT) {
                List<Region> stack = RegionSelection.selectStack(regions, dimension, worldPoint.x(), worldPoint.z());
                boolean selected = session.advanceSelection(stack, worldPoint.x(), worldPoint.z());
                return selected ? Action.SELECTED_REGION : Action.IGNORED;
            }
```

- [ ] **Step 2: 对 forge 执行相同改动**

在 `forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java` 第 285-292 行执行与 Step 1 完全相同的替换。

- [ ] **Step 3: 运行 neoforge 测试确认编译通过**

Run: `./gradlew :neoforge:mc-1.21.1:test`
Expected: PASS — 编译通过，已有测试无回归。

- [ ] **Step 4: 运行 forge 测试确认编译通过**

Run: `./gradlew :forge:mc-1.20.1:test`
Expected: PASS — 编译通过，已有测试无回归。

- [ ] **Step 5: 提交**

```bash
git add neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java
git commit -m "feat: ActionRouter 左键分支改用 selectStack + advanceSelection

- neoforge 与 forge 同步修改 ActionRouter.handleMouse 左键分支
- 原逻辑调用 selectTopmost 只选顶层区域，改为调用 selectStack
  获取全部命中区域后委托 RegionEditSession.advanceSelection 推进
  循环选中状态机
- 右键上下文菜单逻辑不变，仍依赖 selectedRegionId()"
```

---

## Task 5: 平台层 HUD 渲染（neoforge + forge 同步）

**Files:**
- Modify: `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java`（将 `EDIT_BUTTON_MARGIN` 改为 public）
- Modify: `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java`
- Modify: `forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java`（同 neoforge）
- Modify: `forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java`（同 neoforge）

**Interfaces:**
- Consumes: `RegionEditSession.selectionInfo()`（Task 2）、`SelectionHudText.of(...)`（Task 3）、`ClientRegionCache.regions()`、`Region.label()`、`RegionEditorOverlay.EDIT_BUTTON_MARGIN`、`RegionEditorOverlay.EDIT_BUTTON_HEIGHT`、`RegionEditorOverlay.Rect`

**说明：** 两个平台的 `XaeroMapOverlayController` 和 `RegionEditorOverlay` 结构一致，以下改动两个文件都需执行。注意 neoforge 的 `ResourceLocation.fromNamespaceAndPath` 与 forge 的 `new ResourceLocation` 差异不在本 Task 改动范围内。

**已有 import（无需添加）：** `Region`、`Component`、`Minecraft`、`ClientRegionCache`、`Optional`、`Screen`、`GuiGraphics`、`RegionEditSession`、`RegionEditorOverlay`。
**需新增 import：** `SelectionHudText`。
**Font 引用方式：** 现有代码全篇用 `Minecraft.getInstance().font` 内联访问，不声明 `Font` 变量、不 import `Font` 类。`renderSelectionHud` 中同样用全限定名 `net.minecraft.client.gui.Font font = ...`，**不需要添加 Font 的 import**。

- [ ] **Step 1: 将 EDIT_BUTTON_MARGIN 改为 public（neoforge + forge 同步）**

`renderSelectionHud` 需要从外部类访问 `EDIT_BUTTON_MARGIN`，但它当前是 `private`。在 `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java` 和 `forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java` 的第 19 行，将：

```java
    private static final int EDIT_BUTTON_MARGIN = 12;
```

替换为：

```java
    public static final int EDIT_BUTTON_MARGIN = 12;
```

与同文件已为 public 的 `EDIT_BUTTON_WIDTH`、`EDIT_BUTTON_HEIGHT` 风格一致。

- [ ] **Step 2: 修改 neoforge renderEditor 方法，新增 renderSelectionHud 调用**

在 `neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java` 的 `renderEditor` 方法（第 121-130 行），将：

```java
    public static void renderEditor(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
        if (SESSION.isEditing()) {
            RegionEditorOverlay.renderDraft(graphics, project(SESSION.draftPoints(), screen));
        }
        RegionEditorOverlay.renderToolbar(graphics, screen.width, screen.height, SESSION.isEditing(), mouseX, mouseY);
        RegionEditorOverlay.renderButton(graphics, screen.width, screen.height, SESSION.isEditing(), mouseX, mouseY);
        if (contextMenu != null) {
            contextMenu.render(graphics);
        }
    }
```

替换为：

```java
    public static void renderEditor(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
        if (SESSION.isEditing()) {
            RegionEditorOverlay.renderDraft(graphics, project(SESSION.draftPoints(), screen));
        }
        RegionEditorOverlay.renderToolbar(graphics, screen.width, screen.height, SESSION.isEditing(), mouseX, mouseY);
        RegionEditorOverlay.renderButton(graphics, screen.width, screen.height, SESSION.isEditing(), mouseX, mouseY);
        renderSelectionHud(graphics, screen, mouseX, mouseY);
        if (contextMenu != null) {
            contextMenu.render(graphics);
        }
    }
```

- [ ] **Step 3: 在 neoforge XaeroMapOverlayController 新增 renderSelectionHud 方法**

在 `neoforge/.../XaeroMapOverlayController.java` 中，紧接 `renderEditor` 方法之后（第 130 行 `}` 之后），新增以下方法。**不要添加 `net.minecraft.client.gui.Font` 的 import**——代码中使用全限定名，与说明一致。

```java
    private static void renderSelectionHud(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
        Optional<RegionEditSession.SelectionInfo> info = SESSION.selectionInfo();
        if (info.isEmpty()) {
            return;
        }
        String label = ClientRegionCache.regions().stream()
                .filter(r -> r.id().equals(info.get().id()))
                .map(Region::label)
                .findFirst()
                .orElse(info.get().id().value());

        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
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

同时在文件顶部 import 区添加：

```java
import com.suian.xaeroregionsrev.client.editor.SelectionHudText;
```

- [ ] **Step 4: 对 forge 执行相同的 renderEditor 改动**

在 `forge/mc-1.20.1/.../XaeroMapOverlayController.java` 中执行与 Step 2 完全相同的 `renderEditor` 替换。

- [ ] **Step 5: 对 forge 新增相同的 renderSelectionHud 方法**

在 `forge/mc-1.20.1/.../XaeroMapOverlayController.java` 中执行与 Step 3 完全相同的方法新增和 import 添加。

- [ ] **Step 6: 运行 neoforge 测试确认编译通过**

Run: `./gradlew :neoforge:mc-1.21.1:test`
Expected: PASS — 编译通过。

- [ ] **Step 7: 运行 forge 测试确认编译通过**

Run: `./gradlew :forge:mc-1.20.1:test`
Expected: PASS — 编译通过。

- [ ] **Step 8: 提交**

```bash
git add neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java neoforge/mc-1.21.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/editor/RegionEditorOverlay.java forge/mc-1.20.1/src/main/java/com/suian/xaeroregionsrev/client/xaero/XaeroMapOverlayController.java
git commit -m "feat: 右上角新增选中区域 HUD 渲染

- neoforge 与 forge 同步新增 renderSelectionHud 方法
- EDIT_BUTTON_MARGIN 改为 public 供跨类访问（与 WIDTH/HEIGHT 一致）
- 在 renderEditor 中调用，位于编辑按钮渲染之后
- 有选中区域时显示：重叠≥2显示「当前/总数 区域名」，
  单层只显示名称；名称过长截断+悬停 tooltip 显示全名
- HUD 右对齐到编辑按钮右边缘，位于按钮正下方
- label 从 ClientRegionCache 实时查询，区域重命名后即时更新"
```

---

## Task 6: 全量验证与烟测

**Files:** 无文件改动，纯验证。

- [ ] **Step 1: 运行三模块全量测试**

Run: `./gradlew :common:test :neoforge:mc-1.21.1:test :forge:mc-1.20.1:test`
Expected: PASS — 所有测试通过，无编译错误。

- [ ] **Step 2: 运行 buildAll 确认打包无误**

Run: `./gradlew buildAll`
Expected: BUILD SUCCESSFUL — 两个平台 jar 产物正常。

- [ ] **Step 3: 提交检查空白与补丁格式**

Run: `git diff --check`
Expected: 无输出（无空白错误）。

- [ ] **Step 4:（可选）客户端烟测**

如条件允许，分别运行 `./gradlew :neoforge:mc-1.21.1:runClient` 和 `./gradlew :forge:mc-1.20.1:runClient`，进入世界地图编辑模式，创建两个重叠区域，验证：
1. 第一次点击选中顶层区域，HUD 显示 `1/2  名称`
2. 再次点击同一重叠区，切换到底层，HUD 显示 `2/2  名称`
3. 第三次点击循环回顶层
4. 点击区域外，HUD 消失
5. 名称过长时截断显示，鼠标悬停 HUD 显示完整名称
