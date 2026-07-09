package com.suian.xaeroregionsrev.region;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionStyleUpdaterTest {
    @Test
    void updatesOnlyStyleAndUpdatedAt() {
        var original = region();
        var updated = RegionStyleUpdater.withStyle(
                original,
                new ArgbColor(0xAA445566),
                " New Label ",
                new ArgbColor(0xFFEEDDCC),
                300L
        );

        assertEquals(original.id(), updated.id());
        assertEquals(original.name(), updated.name());
        assertEquals(original.dimension(), updated.dimension());
        assertEquals(original.category(), updated.category());
        assertEquals(original.iconName(), updated.iconName());
        assertEquals(original.points(), updated.points());
        assertEquals(original.createdAt(), updated.createdAt());
        assertEquals(new ArgbColor(0xAA445566), updated.color());
        assertEquals(" New Label ", updated.label());
        assertEquals(new ArgbColor(0xFFEEDDCC), updated.labelColor());
        assertEquals(300L, updated.updatedAt());
    }

    @Test
    void rejectsNullRegion() {
        assertThrows(NullPointerException.class, () -> RegionStyleUpdater.withStyle(
                null,
                new ArgbColor(0xAA445566),
                "Label",
                new ArgbColor(0xFFEEDDCC),
                300L
        ));
    }

    private static Region region() {
        return new Region(
                new RegionId("spawn"),
                "Spawn",
                "minecraft:overworld",
                new ArgbColor(0x8800FF00),
                "Old Label",
                new ArgbColor(0xFFFFFFFF),
                "town",
                "home",
                List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                100L,
                200L
        );
    }
}
