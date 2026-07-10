package com.suian.xaeroregionsrev.network.payload;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionEditResultPacketTest {
    @Test
    void exposesCustomPayloadType() {
        var packet = new RegionEditResultPacket(1L, true, true, "Saved.");

        assertEquals(RegionEditResultPacket.TYPE, packet.type());
    }

    @Test
    void streamCodecRoundTripsResult() {
        var packet = new RegionEditResultPacket(12L, true, true, "Region saved.");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        RegionEditResultPacket.STREAM_CODEC.encode(buffer, packet);
        var decoded = RegionEditResultPacket.STREAM_CODEC.decode(buffer);

        assertEquals(packet, decoded);
    }

    @Test
    void encodesAndDecodesSuccessfulResult() {
        var packet = new RegionEditResultPacket(12L, true, true, "Region saved.");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        RegionEditResultPacket.encode(packet, buffer);
        var decoded = RegionEditResultPacket.decode(buffer);

        assertEquals(packet, decoded);
    }

    @Test
    void encodesAndDecodesFailedResult() {
        var packet = new RegionEditResultPacket(13L, false, false,
                "You must be an operator in creative mode to manage regions.");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        RegionEditResultPacket.encode(packet, buffer);
        var decoded = RegionEditResultPacket.decode(buffer);

        assertEquals(packet, decoded);
    }

    @Test
    void decodeRejectsOverLimitMessage() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeLong(1L);
        buffer.writeBoolean(false);
        buffer.writeBoolean(false);
        buffer.writeUtf("m".repeat(RegionEditResultPacket.MAX_MESSAGE_LENGTH + 1));

        assertThrows(RuntimeException.class, () -> RegionEditResultPacket.decode(buffer));
    }
}
