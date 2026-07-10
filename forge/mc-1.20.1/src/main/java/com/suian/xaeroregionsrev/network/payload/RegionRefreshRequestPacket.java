package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.RegionRefreshData;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;

/**
 * 包装 {@link RegionRefreshData} 的 Forge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留 SimpleChannel 所需的 encoder/decoder。
 */
public final class RegionRefreshRequestPacket {
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
