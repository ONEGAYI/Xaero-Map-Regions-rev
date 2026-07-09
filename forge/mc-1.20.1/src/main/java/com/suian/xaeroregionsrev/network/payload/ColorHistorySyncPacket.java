package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.ColorHistorySyncData;
import com.suian.xaeroregionsrev.region.ArgbColor;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Objects;

/**
 * 包装 {@link ColorHistorySyncData} 的 Forge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留 SimpleChannel 所需的 encoder/decoder。
 */
public final class ColorHistorySyncPacket {
    public static final int MAX_COLORS = ColorHistorySyncData.MAX_COLORS;

    private final ColorHistorySyncData data;

    public ColorHistorySyncPacket(List<ArgbColor> colors) {
        this(new ColorHistorySyncData(colors));
    }

    public ColorHistorySyncPacket(ColorHistorySyncData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public List<ArgbColor> colors() {
        return data.colors();
    }

    public ColorHistorySyncData data() {
        return data;
    }

    public static void encode(ColorHistorySyncPacket packet, FriendlyByteBuf buffer) {
        ColorHistorySyncData.encode(new FriendlyByteBufPacketBuffer(buffer), packet.data);
    }

    public static ColorHistorySyncPacket decode(FriendlyByteBuf buffer) {
        FriendlyByteBufPacketBuffer packetBuffer = new FriendlyByteBufPacketBuffer(buffer);
        return new ColorHistorySyncPacket(ColorHistorySyncData.decode(packetBuffer));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ColorHistorySyncPacket that && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ColorHistorySyncPacket" + data;
    }
}
