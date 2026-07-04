package com.suian.xaeroregionsrev.network.payload;

import net.minecraft.network.FriendlyByteBuf;

public record RegionEditResultPacket(
        long requestId,
        boolean success,
        boolean closeScreen,
        String message
) {
    public static final int MAX_MESSAGE_LENGTH = 512;

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
