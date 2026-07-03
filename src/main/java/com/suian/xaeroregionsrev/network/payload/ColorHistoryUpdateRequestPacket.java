package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import net.minecraft.network.FriendlyByteBuf;

public record ColorHistoryUpdateRequestPacket(ArgbColor color) {
    public static void encode(ColorHistoryUpdateRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.color.value());
    }

    public static ColorHistoryUpdateRequestPacket decode(FriendlyByteBuf buffer) {
        return new ColorHistoryUpdateRequestPacket(new ArgbColor(buffer.readInt()));
    }
}
