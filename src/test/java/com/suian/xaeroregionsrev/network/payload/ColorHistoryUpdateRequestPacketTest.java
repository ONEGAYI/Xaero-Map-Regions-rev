package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorHistoryUpdateRequestPacketTest {
    @Test
    void exposesCustomPayloadType() {
        ColorHistoryUpdateRequestPacket packet = new ColorHistoryUpdateRequestPacket(new ArgbColor(0x80445566));

        assertEquals(ColorHistoryUpdateRequestPacket.TYPE, packet.type());
    }

    @Test
    void streamCodecRoundTripsSelectedColor() {
        ColorHistoryUpdateRequestPacket packet = new ColorHistoryUpdateRequestPacket(new ArgbColor(0x80445566));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        ColorHistoryUpdateRequestPacket.STREAM_CODEC.encode(buffer, packet);
        ColorHistoryUpdateRequestPacket decoded = ColorHistoryUpdateRequestPacket.STREAM_CODEC.decode(buffer);

        assertEquals(packet, decoded);
    }

    @Test
    void encodesAndDecodesSelectedColor() {
        ColorHistoryUpdateRequestPacket packet = new ColorHistoryUpdateRequestPacket(new ArgbColor(0x80445566));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        ColorHistoryUpdateRequestPacket.encode(packet, buffer);
        ColorHistoryUpdateRequestPacket decoded = ColorHistoryUpdateRequestPacket.decode(buffer);

        assertEquals(packet, decoded);
    }
}
