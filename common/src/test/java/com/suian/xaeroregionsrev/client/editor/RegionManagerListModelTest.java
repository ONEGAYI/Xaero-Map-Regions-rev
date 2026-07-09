package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionManagerListModelTest {
    @Test
    void centerPointUsesRegionBoundsMidpoint() {
        Region region = region("spawn", "Spawn", List.of(
                new RegionPoint(0, 10),
                new RegionPoint(20, 10),
                new RegionPoint(20, 40),
                new RegionPoint(0, 40)
        ));

        assertEquals(new RegionPoint(10, 25), RegionManagerListModel.centerPoint(region));
        assertEquals("X: 10 Z: 25", RegionManagerListModel.formatCenter(region));
    }

    @Test
    void paginationClampsAndSlicesRegions() {
        List<Region> regions = List.of(
                region("a", "A", List.of(new RegionPoint(0, 0))),
                region("b", "B", List.of(new RegionPoint(1, 0))),
                region("c", "C", List.of(new RegionPoint(2, 0))),
                region("d", "D", List.of(new RegionPoint(3, 0)))
        );

        assertEquals(2, RegionManagerListModel.pageCount(regions.size(), 3));
        assertEquals(1, RegionManagerListModel.clampPage(9, regions.size(), 3));
        assertEquals(List.of(regions.get(3)), RegionManagerListModel.pageItems(regions, 1, 3));
    }

    @Test
    void rowAtMapsMouseToVisiblePageIndex() {
        assertEquals(2, RegionManagerListModel.rowAt(40, 86, 20, 50, 200, 18, 5).orElseThrow());
        assertTrue(RegionManagerListModel.rowAt(10, 86, 20, 50, 200, 18, 5).isEmpty());
        assertTrue(RegionManagerListModel.rowAt(40, 160, 20, 50, 200, 18, 5).isEmpty());
    }

    @Test
    void listWidthStaysInsideNarrowScreens() {
        assertEquals(620, RegionManagerListModel.listWidth(900, 620, 16));
        assertEquals(395, RegionManagerListModel.listWidth(427, 620, 16));
    }

    @Test
    void buttonGridWrapsWhenSingleRowWouldOverflow() {
        assertEquals(6, RegionManagerListModel.buttonColumns(900, 6, 96, 8, 8));
        assertEquals(3, RegionManagerListModel.buttonColumns(360, 6, 96, 8, 8));
        assertEquals(2, RegionManagerListModel.buttonRows(360, 6, 96, 8, 8));
    }

    @Test
    void visiblePageSizeUsesRemainingHeightAboveControls() {
        assertEquals(10, RegionManagerListModel.visiblePageSize(520, 48, 450, 18, 10));
        assertEquals(6, RegionManagerListModel.visiblePageSize(220, 48, 172, 18, 10));
    }

    private static Region region(String id, String label, List<RegionPoint> points) {
        return new Region(
                new RegionId(id),
                id,
                "minecraft:overworld",
                new ArgbColor(0x6600AAFF),
                label,
                new ArgbColor(0xFFFFFFFF),
                "default",
                "default",
                points,
                1L,
                1L
        );
    }
}
