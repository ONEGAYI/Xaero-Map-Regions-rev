package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionSelectionTest {
    @Test
    void selectsLastMatchingRegionWhenPolygonsOverlap() {
        Region back = region("back", List.of(
                new RegionPoint(0, 0),
                new RegionPoint(20, 0),
                new RegionPoint(20, 20),
                new RegionPoint(0, 20)
        ));
        Region front = region("front", List.of(
                new RegionPoint(5, 5),
                new RegionPoint(15, 5),
                new RegionPoint(15, 15),
                new RegionPoint(5, 15)
        ));

        assertEquals(new RegionId("front"),
                RegionSelection.selectTopmost(List.of(back, front), "minecraft:overworld", 10, 10).orElseThrow().id());
    }

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

    @Test
    void ignoresRegionsFromOtherDimensions() {
        Region nether = new Region(
                new RegionId("nether"),
                "nether",
                "minecraft:the_nether",
                new ArgbColor(0x6600FF00),
                "nether",
                new ArgbColor(0xFFFFFFFF),
                "default",
                "default",
                List.of(
                        new RegionPoint(0, 0),
                        new RegionPoint(20, 0),
                        new RegionPoint(20, 20),
                        new RegionPoint(0, 20)
                ),
                1L,
                1L
        );

        assertTrue(RegionSelection.selectTopmost(List.of(nether), "minecraft:overworld", 10, 10).isEmpty());
    }

    private static Region region(String id, List<RegionPoint> points) {
        return new Region(
                new RegionId(id),
                id,
                "minecraft:overworld",
                new ArgbColor(0x6600FF00),
                id,
                new ArgbColor(0xFFFFFFFF),
                "default",
                "default",
                points,
                1L,
                1L
        );
    }
}
