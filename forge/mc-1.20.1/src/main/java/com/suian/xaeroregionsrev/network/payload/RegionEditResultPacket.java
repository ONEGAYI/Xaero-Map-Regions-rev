package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.RegionEditResultData;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;

/**
 * 包装 {@link RegionEditResultData} 的 Forge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留 SimpleChannel 所需的 encoder/decoder。
 */
public final class RegionEditResultPacket {
    public static final int MAX_MESSAGE_LENGTH = RegionEditResultData.MAX_MESSAGE_LENGTH;

    private final RegionEditResultData data;

    public RegionEditResultPacket(long requestId, boolean success, boolean closeScreen, String message) {
        this(new RegionEditResultData(requestId, success, closeScreen, message));
    }

    public RegionEditResultPacket(RegionEditResultData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public long requestId() {
        return data.requestId();
    }

    public boolean success() {
        return data.success();
    }

    public boolean closeScreen() {
        return data.closeScreen();
    }

    public String message() {
        return data.message();
    }

    public RegionEditResultData data() {
        return data;
    }

    public static void encode(RegionEditResultPacket packet, FriendlyByteBuf buffer) {
        RegionEditResultData.encode(new FriendlyByteBufPacketBuffer(buffer), packet.data);
    }

    public static RegionEditResultPacket decode(FriendlyByteBuf buffer) {
        FriendlyByteBufPacketBuffer packetBuffer = new FriendlyByteBufPacketBuffer(buffer);
        return new RegionEditResultPacket(RegionEditResultData.decode(packetBuffer));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof RegionEditResultPacket that && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "RegionEditResultPacket" + data;
    }
}
