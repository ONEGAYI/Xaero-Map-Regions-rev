package com.suian.xaeroregionsrev.network.data;

import com.suian.xaeroregionsrev.network.buffer.PacketBuffer;

public record RegionEditResultData(
        long requestId,
        boolean success,
        boolean closeScreen,
        String message
) {
    public static final int MAX_MESSAGE_LENGTH = 512;

    public static void encode(PacketBuffer buffer, RegionEditResultData data) {
        buffer.writeLong(data.requestId);
        buffer.writeBoolean(data.success);
        buffer.writeBoolean(data.closeScreen);
        buffer.writeUtf(data.message, MAX_MESSAGE_LENGTH);
    }

    public static RegionEditResultData decode(PacketBuffer buffer) {
        long requestId = buffer.readLong();
        boolean success = buffer.readBoolean();
        boolean closeScreen = buffer.readBoolean();
        String message = buffer.readUtf(MAX_MESSAGE_LENGTH);
        return new RegionEditResultData(requestId, success, closeScreen, message);
    }
}
