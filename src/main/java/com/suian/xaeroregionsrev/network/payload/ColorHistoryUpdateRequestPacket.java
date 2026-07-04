package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.region.ArgbColor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ColorHistoryUpdateRequestPacket(ArgbColor color) implements CustomPacketPayload {
    public static final Type<ColorHistoryUpdateRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "color_history_update_request")
    );
    public static final StreamCodec<FriendlyByteBuf, ColorHistoryUpdateRequestPacket> STREAM_CODEC = PacketCodecs.of(
            ColorHistoryUpdateRequestPacket::encode,
            ColorHistoryUpdateRequestPacket::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ColorHistoryUpdateRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.color.value());
    }

    public static ColorHistoryUpdateRequestPacket decode(FriendlyByteBuf buffer) {
        return new ColorHistoryUpdateRequestPacket(new ArgbColor(buffer.readInt()));
    }
}
