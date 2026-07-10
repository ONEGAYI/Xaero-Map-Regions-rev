package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorHistoryUpdateRequestPacketTest {
    @Test
    void encodesAndDecodesSelectedColor() {
        ColorHistoryUpdateRequestPacket packet = new ColorHistoryUpdateRequestPacket(new ArgbColor(0x80445566));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        ColorHistoryUpdateRequestPacket.encode(packet, buffer);
        ColorHistoryUpdateRequestPacket decoded = ColorHistoryUpdateRequestPacket.decode(buffer);

        assertEquals(packet, decoded);
    }
}
