package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.ColorHistoryUpdateRequestData;
import com.suian.xaeroregionsrev.region.ArgbColor;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;

/**
 * 包装 {@link ColorHistoryUpdateRequestData} 的 Forge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留 SimpleChannel 所需的 encoder/decoder。
 */
public final class ColorHistoryUpdateRequestPacket {
    private final ColorHistoryUpdateRequestData data;

    public ColorHistoryUpdateRequestPacket(ArgbColor color) {
        this(new ColorHistoryUpdateRequestData(color));
    }

    public ColorHistoryUpdateRequestPacket(ColorHistoryUpdateRequestData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public ArgbColor color() {
        return data.color();
    }

    public ColorHistoryUpdateRequestData data() {
        return data;
    }

    public static void encode(ColorHistoryUpdateRequestPacket packet, FriendlyByteBuf buffer) {
        ColorHistoryUpdateRequestData.encode(new FriendlyByteBufPacketBuffer(buffer), packet.data);
    }

    public static ColorHistoryUpdateRequestPacket decode(FriendlyByteBuf buffer) {
        FriendlyByteBufPacketBuffer packetBuffer = new FriendlyByteBufPacketBuffer(buffer);
        return new ColorHistoryUpdateRequestPacket(ColorHistoryUpdateRequestData.decode(packetBuffer));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ColorHistoryUpdateRequestPacket that && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ColorHistoryUpdateRequestPacket" + data;
    }
}
