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
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionSyncPacketTest {
    @Test
    void encodesAndDecodesRegions() {
        var region = new Region(
                new RegionId("spawn"),
                "Spawn",
                "minecraft:overworld",
                new ArgbColor(0x8800FF00),
                "Spawn Label",
                new ArgbColor(0xFFFFAA00),
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
        assertEquals("Spawn Label", decoded.regions().get(0).label());
        assertEquals(0xFFFFAA00, decoded.regions().get(0).labelColor().value());
    }

    @Test
    void decodeRejectsNegativeRegionCount() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(-1);

        var exception = assertThrows(IllegalArgumentException.class, () -> RegionSyncPacket.decode(buffer));

        assertEquals("Region count must be between 0 and 4096.", exception.getMessage());
    }

    @Test
    void decodeRejectsTooManyRegions() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(4097);

        var exception = assertThrows(IllegalArgumentException.class, () -> RegionSyncPacket.decode(buffer));

        assertEquals("Region count must be between 0 and 4096.", exception.getMessage());
    }

    @Test
    void decodeRejectsNegativePointCount() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(1);
        writeRegionHeader(buffer);
        buffer.writeVarInt(-1);

        var exception = assertThrows(IllegalArgumentException.class, () -> RegionSyncPacket.decode(buffer));

        assertEquals("Region point count must be between 0 and 1024.", exception.getMessage());
    }

    @Test
    void decodeRejectsTooManyPoints() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(1);
        writeRegionHeader(buffer);
        buffer.writeVarInt(1025);

        var exception = assertThrows(IllegalArgumentException.class, () -> RegionSyncPacket.decode(buffer));

        assertEquals("Region point count must be between 0 and 1024.", exception.getMessage());
    }

    @Test
    void decodeRejectsTooFewPoints() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(1);
        writeRegionHeader(buffer);
        buffer.writeVarInt(2);
        writePoint(buffer, 0, 0);
        writePoint(buffer, 16, 0);

        var exception = assertThrows(IllegalArgumentException.class, () -> RegionSyncPacket.decode(buffer));

        assertEquals("Region points must form a valid polygon.", exception.getMessage());
    }

    private static void writeRegionHeader(FriendlyByteBuf buffer) {
        buffer.writeUtf("spawn");
        buffer.writeUtf("Spawn");
        buffer.writeUtf("minecraft:overworld");
        buffer.writeInt(0x8800FF00);
        buffer.writeUtf("Spawn Label");
        buffer.writeInt(0xFFFFAA00);
        buffer.writeUtf("town");
        buffer.writeUtf("home");
        buffer.writeLong(100L);
        buffer.writeLong(200L);
    }

    private static void writePoint(FriendlyByteBuf buffer, int x, int z) {
        buffer.writeInt(x);
        buffer.writeInt(z);
    }
}
