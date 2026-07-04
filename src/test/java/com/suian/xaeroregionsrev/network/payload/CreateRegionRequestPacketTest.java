package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.RegionLimits;
import com.suian.xaeroregionsrev.region.RegionPoint;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateRegionRequestPacketTest {
    @Test
    void exposesCustomPayloadType() {
        var packet = packet();

        assertEquals(CreateRegionRequestPacket.TYPE, packet.type());
    }

    @Test
    void streamCodecRoundTripsRequest() {
        var packet = packet();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        CreateRegionRequestPacket.STREAM_CODEC.encode(buffer, packet);
        var decoded = CreateRegionRequestPacket.STREAM_CODEC.decode(buffer);

        assertEquals(packet, decoded);
    }

    @Test
    void encodesAndDecodesRequest() {
        var packet = packet();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        CreateRegionRequestPacket.encode(packet, buffer);
        var decoded = CreateRegionRequestPacket.decode(buffer);

        assertEquals(packet, decoded);
        assertEquals(42L, decoded.requestId());
    }

    @Test
    void decodeRejectsTooLongNameAndLabel() {
        assertThrows(RuntimeException.class, () -> {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeLong(1L);
            buffer.writeUtf("n".repeat(RegionLimits.MAX_NAME_LENGTH + 1));
            buffer.writeInt(0x8800FF00);
            buffer.writeUtf("Label");
            buffer.writeInt(0xFFFFFFFF);
            buffer.writeVarInt(3);
            writePoints(buffer);

            CreateRegionRequestPacket.decode(buffer);
        });

        assertThrows(RuntimeException.class, () -> {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeLong(1L);
            buffer.writeUtf("Name");
            buffer.writeInt(0x8800FF00);
            buffer.writeUtf("l".repeat(RegionLimits.MAX_LABEL_LENGTH + 1));
            buffer.writeInt(0xFFFFFFFF);
            buffer.writeVarInt(3);
            writePoints(buffer);

            CreateRegionRequestPacket.decode(buffer);
        });
    }

    @Test
    void decodeRejectsNegativeAndTooManyPoints() {
        assertThrows(IllegalArgumentException.class, () -> {
            var buffer = createHeader();
            buffer.writeVarInt(-1);

            CreateRegionRequestPacket.decode(buffer);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            var buffer = createHeader();
            buffer.writeVarInt(RegionLimits.MAX_POINTS_PER_REQUEST + 1);

            CreateRegionRequestPacket.decode(buffer);
        });
    }

    private static FriendlyByteBuf createHeader() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeLong(1L);
        buffer.writeUtf("Spawn");
        buffer.writeInt(0x8800FF00);
        buffer.writeUtf("Spawn Label");
        buffer.writeInt(0xFFFFFFFF);
        return buffer;
    }

    private static List<RegionPoint> points() {
        return List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16));
    }

    private static CreateRegionRequestPacket packet() {
        return new CreateRegionRequestPacket(
                42L,
                "Spawn",
                new ArgbColor(0x8800FF00),
                "Spawn Label",
                new ArgbColor(0xFFFFFFFF),
                points()
        );
    }

    private static void writePoints(FriendlyByteBuf buffer) {
        for (RegionPoint point : points()) {
            buffer.writeInt(point.x());
            buffer.writeInt(point.z());
        }
    }
}
