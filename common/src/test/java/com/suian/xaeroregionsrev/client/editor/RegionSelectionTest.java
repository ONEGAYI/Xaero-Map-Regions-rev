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
