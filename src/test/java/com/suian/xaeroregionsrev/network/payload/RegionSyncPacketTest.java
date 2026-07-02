package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionSyncPacketTest {
    @Test
    void encodesAndDecodesRegions() {
        var region = new Region(
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
        var packet = new RegionSyncPacket(List.of(region));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        RegionSyncPacket.encode(packet, buffer);
        var decoded = RegionSyncPacket.decode(buffer);

        assertEquals(packet.regions(), decoded.regions());
    }
}
