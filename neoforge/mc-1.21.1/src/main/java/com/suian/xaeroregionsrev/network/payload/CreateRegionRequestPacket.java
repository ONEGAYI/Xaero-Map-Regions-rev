package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.CreateRegionRequestData;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * 包装 {@link CreateRegionRequestData} 的 NeoForge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留负载类型与流编解码器。
 */
public final class CreateRegionRequestPacket implements CustomPacketPayload {
    public static final Type<CreateRegionRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "create_region_request")
    );
    public static final StreamCodec<FriendlyByteBuf, CreateRegionRequestPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> CreateRegionRequestPacket.encode(packet, buffer),
            CreateRegionRequestPacket::decode
    );

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
