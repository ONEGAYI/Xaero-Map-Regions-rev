package com.suian.xaeroregionsrev.region;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegionNbtCodecTest {
    @Test
    void roundTripsRegionThroughNbt() {
        var original = new Region(
                new RegionId("spawn"),
                "Spawn",
                "minecraft:overworld",
                new ArgbColor(0x8800FF00),
                "town",
                "home",
                List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                100L,
                200L
        );

        var tag = RegionNbtCodec.writeRegion(original);
        var decoded = RegionNbtCodec.readRegion(tag);

        assertEquals(original, decoded);
    }
}
