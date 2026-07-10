package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.ColorHistorySyncData;
import com.suian.xaeroregionsrev.region.ArgbColor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * 包装 {@link ColorHistorySyncData} 的 NeoForge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留负载类型与流编解码器。
 */
public final class ColorHistorySyncPacket implements CustomPacketPayload {
    public static final Type<ColorHistorySyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "color_history_sync")
    );
    public static final StreamCodec<FriendlyByteBuf, ColorHistorySyncPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> ColorHistorySyncPacket.encode(packet, buffer),
            ColorHistorySyncPacket::decode
    );
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
