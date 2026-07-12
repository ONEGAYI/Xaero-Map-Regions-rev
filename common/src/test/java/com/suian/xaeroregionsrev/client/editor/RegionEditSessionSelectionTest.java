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
