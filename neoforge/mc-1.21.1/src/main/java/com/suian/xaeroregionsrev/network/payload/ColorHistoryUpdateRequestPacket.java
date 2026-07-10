package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.ColorHistoryUpdateRequestData;
import com.suian.xaeroregionsrev.region.ArgbColor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 包装 {@link ColorHistoryUpdateRequestData} 的 NeoForge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留负载类型与流编解码器。
 */
public final class ColorHistoryUpdateRequestPacket implements CustomPacketPayload {
    public static final Type<ColorHistoryUpdateRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "color_history_update_request")
    );
    public static final StreamCodec<FriendlyByteBuf, ColorHistoryUpdateRequestPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> ColorHistoryUpdateRequestPacket.encode(packet, buffer),
            ColorHistoryUpdateRequestPacket::decode
    );

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
