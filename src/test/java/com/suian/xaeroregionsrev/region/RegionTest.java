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
    void labelAndLabelColorAreReadableWhenProvided() {
        var region = new Region(
                new RegionId("spawn"),
                "Spawn",
                "minecraft:overworld",
                new ArgbColor(0x8000FF00),
                "Spawn Label",
                new ArgbColor(0xFFFFAA00),
                "default",
                "home",
                List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                1L,
                2L
        );

        assertEquals("Spawn Label", region.label());
        assertEquals(0xFFFFAA00, region.labelColor().value());
    }

    @Test
    void legacyConstructorDefaultsLabelToNameAndWhiteLabelColor() {
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

        assertEquals("Spawn", region.label());
        assertEquals(0xFFFFFFFF, region.labelColor().value());
    }

    @Test
    void blankLabelAndNullLabelColorAreRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new Region(
                        new RegionId("spawn"),
                        "Spawn",
                        "minecraft:overworld",
                        new ArgbColor(0x8000FF00),
                        " ",
                        new ArgbColor(0xFFFFFFFF),
                        "default",
                        "home",
                        List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                        1L,
                        2L
                )),
                () -> assertThrows(IllegalArgumentException.class, () -> new Region(
                        new RegionId("spawn"),
                        "Spawn",
                        "minecraft:overworld",
                        new ArgbColor(0x8000FF00),
                        "Spawn",
                        null,
                        "default",
                        "home",
                        List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                        1L,
                        2L
                ))
        );
    }

    @Test
    void blankRegionIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RegionId(" "));
    }

    @Test
    void nullRequiredRegionFieldsAreRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new Region(
                        null,
                        "Spawn",
                        "minecraft:overworld",
                        new ArgbColor(0x8000FF00),
                        "default",
                        "home",
                        List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                        1L,
                        2L
                )),
                () -> assertThrows(IllegalArgumentException.class, () -> new Region(
                        new RegionId("spawn"),
                        "Spawn",
                        "minecraft:overworld",
                        null,
                        "default",
                        "home",
                        List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                        1L,
                        2L
                )),
                () -> assertThrows(IllegalArgumentException.class, () -> new Region(
                        new RegionId("spawn"),
                        "Spawn",
                        "minecraft:overworld",
                        new ArgbColor(0x8000FF00),
                        " ",
                        "home",
                        List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                        1L,
                        2L
                )),
                () -> assertThrows(IllegalArgumentException.class, () -> new Region(
                        new RegionId("spawn"),
                        "Spawn",
                        "minecraft:overworld",
                        new ArgbColor(0x8000FF00),
                        "default",
                        " ",
                        List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                        1L,
                        2L
                )),
                () -> assertThrows(IllegalArgumentException.class, () -> new Region(
                        new RegionId("spawn"),
                        "Spawn",
                        "minecraft:overworld",
                        new ArgbColor(0x8000FF00),
                        "default",
                        "home",
                        null,
                        1L,
                        2L
                ))
        );
    }

    @Test
    void pointMarkerRequiresTargetPlayer() {
        assertThrows(IllegalArgumentException.class, () -> new PointMarker(
                null,
                "player",
                "home",
                "Spawn",
                0,
                64,
                0
        ));
    }
}
