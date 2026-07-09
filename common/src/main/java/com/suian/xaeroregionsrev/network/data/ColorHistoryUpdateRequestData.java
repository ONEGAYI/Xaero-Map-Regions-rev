package com.suian.xaeroregionsrev.network.data;

import com.suian.xaeroregionsrev.network.buffer.PacketBuffer;
import com.suian.xaeroregionsrev.region.ArgbColor;

public record ColorHistoryUpdateRequestData(ArgbColor color) {
    public static void encode(PacketBuffer buffer, ColorHistoryUpdateRequestData data) {
        buffer.writeInt(data.color.value());
    }

    public static ColorHistoryUpdateRequestData decode(PacketBuffer buffer) {
        return new ColorHistoryUpdateRequestData(new ArgbColor(buffer.readInt()));
    }
}
