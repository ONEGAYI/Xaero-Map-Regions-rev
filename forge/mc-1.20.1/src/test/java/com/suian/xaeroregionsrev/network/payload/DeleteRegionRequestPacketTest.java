package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionLimits;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteRegionRequestPacketTest {
    @Test
    void encodesAndDecodesRequest() {
        var packet = new DeleteRegionRequestPacket(new RegionId("Spawn"));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        DeleteRegionRequestPacket.encode(packet, buffer);
        var decoded = DeleteRegionRequestPacket.decode(buffer);

        assertEquals(packet, decoded);
        assertEquals("spawn", decoded.id().value());
    }

    @Test
    void decodeAllowsBlankIdForHandlerValidation() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf(" ");

        var decoded = DeleteRegionRequestPacket.decode(buffer);

        assertEquals(" ", decoded.idText());
    }

    @Test
    void decodeRejectsTooLongId() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("i".repeat(RegionLimits.MAX_NAME_LENGTH + 1));

        assertThrows(RuntimeException.class, () -> DeleteRegionRequestPacket.decode(buffer));
    }
}
