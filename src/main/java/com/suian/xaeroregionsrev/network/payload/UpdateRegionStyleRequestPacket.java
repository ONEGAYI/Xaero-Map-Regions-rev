package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionLimits;
import net.minecraft.network.FriendlyByteBuf;

public record UpdateRegionStyleRequestPacket(
        String idText,
        ArgbColor fillColor,
        String label,
        ArgbColor labelColor
) {
    public UpdateRegionStyleRequestPacket(RegionId id, ArgbColor fillColor, String label, ArgbColor labelColor) {
        this(id.value(), fillColor, label, labelColor);
    }

    public static void encode(UpdateRegionStyleRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.idText);
        buffer.writeInt(packet.fillColor.value());
        buffer.writeUtf(packet.label);
        buffer.writeInt(packet.labelColor.value());
    }

    public static UpdateRegionStyleRequestPacket decode(FriendlyByteBuf buffer) {
        String idText = buffer.readUtf(RegionLimits.MAX_NAME_LENGTH);
        ArgbColor fillColor = new ArgbColor(buffer.readInt());
        String label = buffer.readUtf(RegionLimits.MAX_LABEL_LENGTH);
        ArgbColor labelColor = new ArgbColor(buffer.readInt());
        return new UpdateRegionStyleRequestPacket(idText, fillColor, label, labelColor);
    }

    public RegionId id() {
        return new RegionId(idText);
    }
}
