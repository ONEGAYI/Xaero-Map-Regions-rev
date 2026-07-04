package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RegionEditResultPacket(
        long requestId,
        boolean success,
        boolean closeScreen,
        String message
) implements CustomPacketPayload {
    public static final Type<RegionEditResultPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "region_edit_result")
    );
    public static final StreamCodec<FriendlyByteBuf, RegionEditResultPacket> STREAM_CODEC = PacketCodecs.of(
            RegionEditResultPacket::encode,
            RegionEditResultPacket::decode
    );
    public static final int MAX_MESSAGE_LENGTH = 512;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(RegionEditResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.requestId);
        buffer.writeBoolean(packet.success);
        buffer.writeBoolean(packet.closeScreen);
        buffer.writeUtf(packet.message, MAX_MESSAGE_LENGTH);
    }

    public static RegionEditResultPacket decode(FriendlyByteBuf buffer) {
        long requestId = buffer.readLong();
        boolean success = buffer.readBoolean();
        boolean closeScreen = buffer.readBoolean();
        String message = buffer.readUtf(MAX_MESSAGE_LENGTH);
        return new RegionEditResultPacket(requestId, success, closeScreen, message);
    }
}
