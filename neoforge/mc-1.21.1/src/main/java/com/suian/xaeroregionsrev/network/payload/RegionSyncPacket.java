package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.RegionSyncData;
import com.suian.xaeroregionsrev.region.Region;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * 包装 {@link RegionSyncData} 的 NeoForge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留负载类型与流编解码器。
 */
public final class RegionSyncPacket implements CustomPacketPayload {
    public static final Type<RegionSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "region_sync")
    );
    public static final StreamCodec<FriendlyByteBuf, RegionSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> RegionSyncPacket.encode(packet, buffer),
            RegionSyncPacket::decode
    );
    public static final int MAX_REGIONS = RegionSyncData.MAX_REGIONS;
    public static final int MAX_POINTS_PER_REGION = RegionSyncData.MAX_POINTS_PER_REGION;
    public static final int MAX_TOTAL_POINTS = RegionSyncData.MAX_TOTAL_POINTS;

    private final RegionSyncData data;

    public RegionSyncPacket(List<Region> regions) {
        this(new RegionSyncData(regions));
    }

    public RegionSyncPacket(RegionSyncData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public List<Region> regions() {
        return data.regions();
    }

    public RegionSyncData data() {
        return data;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(RegionSyncPacket packet, FriendlyByteBuf buffer) {
        RegionSyncData.encode(new FriendlyByteBufPacketBuffer(buffer), packet.data);
    }

    public static RegionSyncPacket decode(FriendlyByteBuf buffer) {
        FriendlyByteBufPacketBuffer packetBuffer = new FriendlyByteBufPacketBuffer(buffer);
        return new RegionSyncPacket(RegionSyncData.decode(packetBuffer));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof RegionSyncPacket that && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "RegionSyncPacket" + data;
    }
}
