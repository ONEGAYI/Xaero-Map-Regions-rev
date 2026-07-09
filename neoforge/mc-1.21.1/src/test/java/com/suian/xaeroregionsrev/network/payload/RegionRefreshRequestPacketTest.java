package com.suian.xaeroregionsrev.network.payload;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionRefreshRequestPacketTest {
    @Test
    void exposesCustomPayloadType() {
        var packet = new RegionRefreshRequestPacket();

        assertEquals(RegionRefreshRequestPacket.TYPE, packet.type());
    }

    @Test
    void streamCodecRoundTripsRefreshRequest() {
        var packet = new RegionRefreshRequestPacket();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        RegionRefreshRequestPacket.STREAM_CODEC.encode(buffer, packet);
        var decoded = RegionRefreshRequestPacket.STREAM_CODEC.decode(buffer);

        assertEquals(packet, decoded);
    }

    @Test
    void encodesAndDecodesEmptyRefreshRequest() {
        var packet = new RegionRefreshRequestPacket();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        RegionRefreshRequestPacket.encode(packet, buffer);
        var decoded = RegionRefreshRequestPacket.decode(buffer);

        assertEquals(packet, decoded);
        assertEquals(0, buffer.readableBytes());
    }
}
