package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.RegionEditResultData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 包装 {@link RegionEditResultData} 的 NeoForge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留负载类型与流编解码器。
 */
public final class RegionEditResultPacket implements CustomPacketPayload {
    public static final Type<RegionEditResultPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "region_edit_result")
    );
    public static final StreamCodec<FriendlyByteBuf, RegionEditResultPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> RegionEditResultPacket.encode(packet, buffer),
            RegionEditResultPacket::decode
    );
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
