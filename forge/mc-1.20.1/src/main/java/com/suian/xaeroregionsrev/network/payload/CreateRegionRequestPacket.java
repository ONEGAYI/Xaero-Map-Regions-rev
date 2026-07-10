package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.CreateRegionRequestData;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Objects;

/**
 * 包装 {@link CreateRegionRequestData} 的 Forge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留 SimpleChannel 所需的 encoder/decoder。
 */
public final class CreateRegionRequestPacket {
    private final CreateRegionRequestData data;

    public CreateRegionRequestPacket(
            long requestId,
            String name,
            ArgbColor fillColor,
            String label,
            ArgbColor labelColor,
            List<RegionPoint> points
    ) {
        this(new CreateRegionRequestData(requestId, name, fillColor, label, labelColor, points));
    }

    public CreateRegionRequestPacket(String name, ArgbColor fillColor, String label, ArgbColor labelColor,
                                     List<RegionPoint> points) {
        this(new CreateRegionRequestData(name, fillColor, label, labelColor, points));
    }

    public CreateRegionRequestPacket(CreateRegionRequestData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public long requestId() {
        return data.requestId();
    }

    public String name() {
        return data.name();
    }

    public ArgbColor fillColor() {
        return data.fillColor();
    }

    public String label() {
        return data.label();
    }

    public ArgbColor labelColor() {
        return data.labelColor();
    }

    public List<RegionPoint> points() {
        return data.points();
    }

    public CreateRegionRequestData data() {
        return data;
    }

    public static void encode(CreateRegionRequestPacket packet, FriendlyByteBuf buffer) {
        CreateRegionRequestData.encode(new FriendlyByteBufPacketBuffer(buffer), packet.data);
    }

    public static CreateRegionRequestPacket decode(FriendlyByteBuf buffer) {
        FriendlyByteBufPacketBuffer packetBuffer = new FriendlyByteBufPacketBuffer(buffer);
        return new CreateRegionRequestPacket(CreateRegionRequestData.decode(packetBuffer));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof CreateRegionRequestPacket that && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "CreateRegionRequestPacket" + data;
    }
}
