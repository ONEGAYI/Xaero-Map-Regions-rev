package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionLimits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateRegionStyleRequestPacket(
        long requestId,
        String idText,
        ArgbColor fillColor,
        String label,
        ArgbColor labelColor
) implements CustomPacketPayload {
    public static final Type<UpdateRegionStyleRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "update_region_style_request")
    );
    public static final StreamCodec<FriendlyByteBuf, UpdateRegionStyleRequestPacket> STREAM_CODEC = PacketCodecs.of(
            UpdateRegionStyleRequestPacket::encode,
            UpdateRegionStyleRequestPacket::decode
    );

    public UpdateRegionStyleRequestPacket(String idText, ArgbColor fillColor, String label, ArgbColor labelColor) {
        this(0L, idText, fillColor, label, labelColor);
    }

    public UpdateRegionStyleRequestPacket(RegionId id, ArgbColor fillColor, String label, ArgbColor labelColor) {
        this(0L, id.value(), fillColor, label, labelColor);
    }

    public UpdateRegionStyleRequestPacket(long requestId, RegionId id, ArgbColor fillColor, String label,
                                          ArgbColor labelColor) {
        this(requestId, id.value(), fillColor, label, labelColor);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(UpdateRegionStyleRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.requestId);
        buffer.writeUtf(packet.idText, RegionLimits.MAX_ID_LENGTH);
        buffer.writeInt(packet.fillColor.value());
        buffer.writeUtf(packet.label, RegionLimits.MAX_LABEL_LENGTH);
        buffer.writeInt(packet.labelColor.value());
    }

    public static UpdateRegionStyleRequestPacket decode(FriendlyByteBuf buffer) {
        long requestId = buffer.readLong();
        String idText = buffer.readUtf(RegionLimits.MAX_ID_LENGTH);
        ArgbColor fillColor = new ArgbColor(buffer.readInt());
        String label = buffer.readUtf(RegionLimits.MAX_LABEL_LENGTH);
        ArgbColor labelColor = new ArgbColor(buffer.readInt());
        return new UpdateRegionStyleRequestPacket(requestId, idText, fillColor, label, labelColor);
    }

    public RegionId id() {
        return new RegionId(idText);
    }
}
