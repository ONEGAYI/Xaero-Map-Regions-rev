package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.UpdateRegionStyleRequestData;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.RegionId;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 包装 {@link UpdateRegionStyleRequestData} 的 NeoForge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留负载类型与流编解码器。
 */
public final class UpdateRegionStyleRequestPacket implements CustomPacketPayload {
    public static final Type<UpdateRegionStyleRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "update_region_style_request")
    );
    public static final StreamCodec<FriendlyByteBuf, UpdateRegionStyleRequestPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> UpdateRegionStyleRequestPacket.encode(packet, buffer),
            UpdateRegionStyleRequestPacket::decode
    );

    private final UpdateRegionStyleRequestData data;

    public UpdateRegionStyleRequestPacket(
            long requestId,
            String idText,
            ArgbColor fillColor,
            String label,
            ArgbColor labelColor
    ) {
        this(new UpdateRegionStyleRequestData(requestId, idText, fillColor, label, labelColor));
    }

    public UpdateRegionStyleRequestPacket(String idText, ArgbColor fillColor, String label, ArgbColor labelColor) {
        this(new UpdateRegionStyleRequestData(idText, fillColor, label, labelColor));
    }

    public UpdateRegionStyleRequestPacket(RegionId id, ArgbColor fillColor, String label, ArgbColor labelColor) {
        this(new UpdateRegionStyleRequestData(id, fillColor, label, labelColor));
    }

    public UpdateRegionStyleRequestPacket(long requestId, RegionId id, ArgbColor fillColor, String label,
                                          ArgbColor labelColor) {
        this(new UpdateRegionStyleRequestData(requestId, id, fillColor, label, labelColor));
    }

    public UpdateRegionStyleRequestPacket(UpdateRegionStyleRequestData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public long requestId() {
        return data.requestId();
    }

    public String idText() {
        return data.idText();
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

    public RegionId id() {
        return data.id();
    }

    public UpdateRegionStyleRequestData data() {
        return data;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(UpdateRegionStyleRequestPacket packet, FriendlyByteBuf buffer) {
        UpdateRegionStyleRequestData.encode(new FriendlyByteBufPacketBuffer(buffer), packet.data);
    }

    public static UpdateRegionStyleRequestPacket decode(FriendlyByteBuf buffer) {
        FriendlyByteBufPacketBuffer packetBuffer = new FriendlyByteBufPacketBuffer(buffer);
        return new UpdateRegionStyleRequestPacket(UpdateRegionStyleRequestData.decode(packetBuffer));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof UpdateRegionStyleRequestPacket that && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "UpdateRegionStyleRequestPacket" + data;
    }
}
