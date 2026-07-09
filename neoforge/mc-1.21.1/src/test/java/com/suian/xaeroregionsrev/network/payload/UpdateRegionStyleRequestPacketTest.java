package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionLimits;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateRegionStyleRequestPacketTest {
    @Test
    void exposesCustomPayloadType() {
        var packet = packet();

        assertEquals(UpdateRegionStyleRequestPacket.TYPE, packet.type());
    }

    @Test
    void streamCodecRoundTripsRequest() {
        var packet = packet();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        UpdateRegionStyleRequestPacket.STREAM_CODEC.encode(buffer, packet);
        var decoded = UpdateRegionStyleRequestPacket.STREAM_CODEC.decode(buffer);

        assertEquals(packet, decoded);
    }

    @Test
    void encodesAndDecodesRequest() {
        var packet = packet();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        UpdateRegionStyleRequestPacket.encode(packet, buffer);
        var decoded = UpdateRegionStyleRequestPacket.decode(buffer);

        assertEquals(packet, decoded);
        assertEquals(77L, decoded.requestId());
        assertEquals("spawn", decoded.id().value());
    }

    @Test
    void decodeAllowsBlankIdForHandlerValidation() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeLong(1L);
        buffer.writeUtf(" ");
        buffer.writeInt(0x8800FF00);
        buffer.writeUtf("Label");
        buffer.writeInt(0xFFFFFFFF);

        var decoded = UpdateRegionStyleRequestPacket.decode(buffer);

        assertEquals(" ", decoded.idText());
    }

    @Test
    void decodeRejectsTooLongIdAndLabel() {
        assertThrows(RuntimeException.class, () -> {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeLong(1L);
            buffer.writeUtf("i".repeat(RegionLimits.MAX_NAME_LENGTH + 1));
            buffer.writeInt(0x8800FF00);
            buffer.writeUtf("Label");
            buffer.writeInt(0xFFFFFFFF);

            UpdateRegionStyleRequestPacket.decode(buffer);
        });

        assertThrows(RuntimeException.class, () -> {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeLong(1L);
            buffer.writeUtf("spawn");
            buffer.writeInt(0x8800FF00);
            buffer.writeUtf("l".repeat(RegionLimits.MAX_LABEL_LENGTH + 1));
            buffer.writeInt(0xFFFFFFFF);

            UpdateRegionStyleRequestPacket.decode(buffer);
        });
    }

    private static UpdateRegionStyleRequestPacket packet() {
        return new UpdateRegionStyleRequestPacket(
                77L,
                new RegionId("Spawn"),
                new ArgbColor(0x8800FF00),
                "Spawn Label",
                new ArgbColor(0xFFFFFFFF)
        );
    }
}
