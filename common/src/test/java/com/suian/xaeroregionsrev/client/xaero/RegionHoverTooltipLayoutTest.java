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
