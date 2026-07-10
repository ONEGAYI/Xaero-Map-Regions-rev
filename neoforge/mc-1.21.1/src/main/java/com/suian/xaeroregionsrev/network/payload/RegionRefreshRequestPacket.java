package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.RegionRefreshData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 包装 {@link RegionRefreshData} 的 NeoForge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留负载类型与流编解码器。
 */
public final class RegionRefreshRequestPacket implements CustomPacketPayload {
    public static final Type<RegionRefreshRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "region_refresh_request")
    );
    public static final StreamCodec<FriendlyByteBuf, RegionRefreshRequestPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> RegionRefreshRequestPacket.encode(packet, buffer),
            RegionRefreshRequestPacket::decode
    );

    private final RegionRefreshData data;

    public RegionRefreshRequestPacket() {
        this(new RegionRefreshData());
    }

    public RegionRefreshRequestPacket(RegionRefreshData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public RegionRefreshData data() {
        return data;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(RegionRefreshRequestPacket packet, FriendlyByteBuf buffer) {
        RegionRefreshData.encode(new FriendlyByteBufPacketBuffer(buffer), packet.data);
    }

    public static RegionRefreshRequestPacket decode(FriendlyByteBuf buffer) {
        FriendlyByteBufPacketBuffer packetBuffer = new FriendlyByteBufPacketBuffer(buffer);
        return new RegionRefreshRequestPacket(RegionRefreshData.decode(packetBuffer));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof RegionRefreshRequestPacket;
    }

    @Override
    public int hashCode() {
        return RegionRefreshRequestPacket.class.hashCode();
    }

    @Override
    public String toString() {
        return "RegionRefreshRequestPacket" + data;
    }
}
