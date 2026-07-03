package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.client.editor.RegionEditorOverlay;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionLabelDisplayTest {
    @Test
    void showsLabelWhenProjectedAreaPassesScreenRatioThreshold() {
        List<RegionEditorOverlay.ScreenPoint> points = List.of(
                new RegionEditorOverlay.ScreenPoint(0, 0),
                new RegionEditorOverlay.ScreenPoint(200, 0),
                new RegionEditorOverlay.ScreenPoint(200, 200),
                new RegionEditorOverlay.ScreenPoint(0, 200)
        );

        assertTrue(RegionLabelDisplay.shouldRenderInlineLabel(points, 800, 600));
    }

    @Test
    void hidesLabelForTinyRegionUnlessHovered() {
        List<RegionEditorOverlay.ScreenPoint> points = List.of(
                new RegionEditorOverlay.ScreenPoint(10, 10),
                new RegionEditorOverlay.ScreenPoint(20, 10),
                new RegionEditorOverlay.ScreenPoint(20, 20),
                new RegionEditorOverlay.ScreenPoint(10, 20)
        );

        assertFalse(RegionLabelDisplay.shouldRenderInlineLabel(points, 800, 600));
        assertTrue(RegionLabelDisplay.isHovered(points, 15, 15));
    }

    @Test
    void truncatesLongLabels() {
        assertEquals("Old Growth Birch...", RegionLabelDisplay.truncate("Old Growth Birch Forest", 16));
        assertEquals("Spawn", RegionLabelDisplay.truncate("Spawn", 16));
    }
}
