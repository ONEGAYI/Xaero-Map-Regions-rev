package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.client.editor.RegionEditorOverlay;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionLabelDisplayTest {
    @Test
    void laysOutFullLabelWhenMeasuredTextFitsInsidePolygon() {
        List<RegionEditorOverlay.ScreenPoint> points = List.of(
                new RegionEditorOverlay.ScreenPoint(0, 0),
                new RegionEditorOverlay.ScreenPoint(100, 0),
                new RegionEditorOverlay.ScreenPoint(100, 24),
                new RegionEditorOverlay.ScreenPoint(0, 24)
        );

        Optional<RegionLabelDisplay.InlineLabel> label = RegionLabelDisplay.layoutInlineLabel(
                "Spawn", points, 10, RegionLabelDisplayTest::monospaceWidth);

        assertTrue(label.isPresent());
        assertEquals("Spawn", label.orElseThrow().text());
    }

    @Test
    void laysOutLabelWhenTextHeightExactlyFitsPolygonHeight() {
        List<RegionEditorOverlay.ScreenPoint> points = List.of(
                new RegionEditorOverlay.ScreenPoint(0, 0),
                new RegionEditorOverlay.ScreenPoint(100, 0),
                new RegionEditorOverlay.ScreenPoint(100, 10),
                new RegionEditorOverlay.ScreenPoint(0, 10)
        );

        Optional<RegionLabelDisplay.InlineLabel> label = RegionLabelDisplay.layoutInlineLabel(
                "Spawn", points, 10, RegionLabelDisplayTest::monospaceWidth);

        assertTrue(label.isPresent());
        assertEquals("Spawn", label.orElseThrow().text());
    }

    @Test
    void truncatesToLongestMeasuredTextThatFitsPolygonWidth() {
        List<RegionEditorOverlay.ScreenPoint> points = List.of(
                new RegionEditorOverlay.ScreenPoint(0, 0),
                new RegionEditorOverlay.ScreenPoint(60, 0),
                new RegionEditorOverlay.ScreenPoint(60, 24),
                new RegionEditorOverlay.ScreenPoint(0, 24)
        );

        Optional<RegionLabelDisplay.InlineLabel> label = RegionLabelDisplay.layoutInlineLabel(
                "Spawn Village", points, 10, RegionLabelDisplayTest::monospaceWidth);

        assertTrue(label.isPresent());
        assertEquals("Spawn\u2026", label.orElseThrow().text());
    }

    @Test
    void hidesInlineLabelWhenFirstCharacterAndEllipsisDoNotFit() {
        List<RegionEditorOverlay.ScreenPoint> points = List.of(
                new RegionEditorOverlay.ScreenPoint(0, 0),
                new RegionEditorOverlay.ScreenPoint(15, 0),
                new RegionEditorOverlay.ScreenPoint(15, 24),
                new RegionEditorOverlay.ScreenPoint(0, 24)
        );

        assertTrue(RegionLabelDisplay.layoutInlineLabel(
                "Spawn", points, 10, RegionLabelDisplayTest::monospaceWidth).isEmpty());
    }

    @Test
    void hidesInlineLabelWhenAnchorIsOutsideConcavePolygonInterior() {
        List<RegionEditorOverlay.ScreenPoint> points = List.of(
                new RegionEditorOverlay.ScreenPoint(0, 0),
                new RegionEditorOverlay.ScreenPoint(100, 0),
                new RegionEditorOverlay.ScreenPoint(100, 30),
                new RegionEditorOverlay.ScreenPoint(30, 30),
                new RegionEditorOverlay.ScreenPoint(30, 100),
                new RegionEditorOverlay.ScreenPoint(0, 100)
        );

        assertTrue(RegionLabelDisplay.layoutInlineLabel(
                "Spawn", points, 10, RegionLabelDisplayTest::monospaceWidth).isEmpty());
    }

    @Test
    void detectsHoverInsidePolygonForTooltipFallback() {
        List<RegionEditorOverlay.ScreenPoint> points = List.of(
                new RegionEditorOverlay.ScreenPoint(10, 10),
                new RegionEditorOverlay.ScreenPoint(20, 10),
                new RegionEditorOverlay.ScreenPoint(20, 20),
                new RegionEditorOverlay.ScreenPoint(10, 20)
        );

        assertTrue(RegionLabelDisplay.isHovered(points, 15, 15));
        assertFalse(RegionLabelDisplay.isHovered(points, 25, 25));
    }

    private static int monospaceWidth(String text) {
        return text.codePointCount(0, text.length()) * 10;
    }
}
