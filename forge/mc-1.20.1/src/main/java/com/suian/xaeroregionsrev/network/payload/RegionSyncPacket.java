package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.RegionSyncData;
import com.suian.xaeroregionsrev.region.Region;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Objects;

/**
 * 包装 {@link RegionSyncData} 的 Forge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留 SimpleChannel 所需的 encoder/decoder。
 */
public final class RegionSyncPacket {
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
