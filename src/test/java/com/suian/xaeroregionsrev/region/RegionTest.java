package com.suian.xaeroregionsrev.region;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegionTest {
    @Test
    void validRegionRequiresAtLeastThreePoints() {
        var region = new Region(
                new RegionId("spawn"),
                "Spawn",
                "minecraft:overworld",
                new ArgbColor(0x8000FF00),
                "default",
                "home",
                List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                1L,
                2L
        );

        assertEquals("spawn", region.id().value());
        assertTrue(region.hasValidPolygon());
    }

    @Test
    void blankRegionIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RegionId(" "));
    }
}
