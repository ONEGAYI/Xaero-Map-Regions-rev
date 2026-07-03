package com.suian.xaeroregionsrev.client.xaero;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionRenderStyleTest {
    @Test
    void boundaryColorDiffersFromFillAndIsMoreOpaque() {
        int fill = 0x6600AAFF;

        int boundary = RegionRenderStyle.boundaryColor(fill);

        assertNotEquals(fill, boundary);
        assertTrue(((boundary >>> 24) & 0xFF) > ((fill >>> 24) & 0xFF));
    }

    @Test
    void vertexDotsAreThickerThanBoundaryLines() {
        assertTrue(RegionRenderStyle.VERTEX_RADIUS * 2 > RegionRenderStyle.BOUNDARY_THICKNESS);
    }

    @Test
    void decorationsAreHiddenWhenProjectedRegionIsTooSmall() {
        RegionRenderStyle.Decoration decoration = RegionRenderStyle.decorationForProjectedBounds(120.0F, 20.0F);

        assertFalse(decoration.visible());
    }

    @Test
    void decorationsBecomeThinBeforeFullSize() {
        RegionRenderStyle.Decoration decoration = RegionRenderStyle.decorationForProjectedBounds(56.0F, 56.0F);

        assertTrue(decoration.visible());
        assertTrue(decoration.boundaryThickness() < RegionRenderStyle.BOUNDARY_THICKNESS);
        assertTrue(decoration.vertexRadius() < RegionRenderStyle.VERTEX_RADIUS);
    }

    @Test
    void decorationsUseFullSizeForLargeProjectedRegions() {
        RegionRenderStyle.Decoration decoration = RegionRenderStyle.decorationForProjectedBounds(96.0F, 96.0F);

        assertTrue(decoration.visible());
        assertEquals(RegionRenderStyle.BOUNDARY_THICKNESS, decoration.boundaryThickness());
        assertEquals(RegionRenderStyle.VERTEX_RADIUS, decoration.vertexRadius());
    }
}
