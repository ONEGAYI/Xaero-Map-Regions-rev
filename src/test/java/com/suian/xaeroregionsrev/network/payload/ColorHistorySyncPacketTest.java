package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ColorHistorySyncPacketTest {
    @Test
    void exposesCustomPayloadType() {
        ColorHistorySyncPacket packet = packet();

        assertEquals(ColorHistorySyncPacket.TYPE, packet.type());
    }

    @Test
    void streamCodecRoundTripsColors() {
        ColorHistorySyncPacket packet = packet();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        ColorHistorySyncPacket.STREAM_CODEC.encode(buffer, packet);
        ColorHistorySyncPacket decoded = ColorHistorySyncPacket.STREAM_CODEC.decode(buffer);

        assertEquals(packet, decoded);
    }

    @Test
    void encodesAndDecodesColors() {
        ColorHistorySyncPacket packet = packet();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        ColorHistorySyncPacket.encode(packet, buffer);
        ColorHistorySyncPacket decoded = ColorHistorySyncPacket.decode(buffer);

        assertEquals(packet.colors(), decoded.colors());
    }

    @Test
    void decodeRejectsTooManyColors() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(ColorHistorySyncPacket.MAX_COLORS + 1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ColorHistorySyncPacket.decode(buffer)
        );

        assertEquals("Color history count must be between 0 and "
                + ColorHistorySyncPacket.MAX_COLORS + ".", exception.getMessage());
    }

    @Test
    void encodeRejectsTooManyColors() {
        ColorHistorySyncPacket packet = new ColorHistorySyncPacket(Collections.nCopies(
                ColorHistorySyncPacket.MAX_COLORS + 1,
                new ArgbColor(0xFF112233)
        ));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ColorHistorySyncPacket.encode(packet, buffer)
        );

        assertEquals("Color history count must be between 0 and "
                + ColorHistorySyncPacket.MAX_COLORS + ".", exception.getMessage());
    }

    private static ColorHistorySyncPacket packet() {
        return new ColorHistorySyncPacket(List.of(
                new ArgbColor(0xFF112233),
                new ArgbColor(0x80445566)
        ));
    }
}
