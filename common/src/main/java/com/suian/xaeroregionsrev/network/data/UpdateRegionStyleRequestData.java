package com.suian.xaeroregionsrev.network.data;

import com.suian.xaeroregionsrev.network.buffer.PacketBuffer;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionLimits;

public record UpdateRegionStyleRequestData(
        long requestId,
        String idText,
        ArgbColor fillColor,
        String label,
        ArgbColor labelColor
) {
    public UpdateRegionStyleRequestData(String idText, ArgbColor fillColor, String label, ArgbColor labelColor) {
        this(0L, idText, fillColor, label, labelColor);
    }

    public UpdateRegionStyleRequestData(RegionId id, ArgbColor fillColor, String label, ArgbColor labelColor) {
        this(0L, id.value(), fillColor, label, labelColor);
    }

    public UpdateRegionStyleRequestData(long requestId, RegionId id, ArgbColor fillColor, String label,
                                        ArgbColor labelColor) {
        this(requestId, id.value(), fillColor, label, labelColor);
    }

    public static void encode(PacketBuffer buffer, UpdateRegionStyleRequestData data) {
        buffer.writeLong(data.requestId);
        buffer.writeUtf(data.idText, RegionLimits.MAX_ID_LENGTH);
        buffer.writeInt(data.fillColor.value());
        buffer.writeUtf(data.label, RegionLimits.MAX_LABEL_LENGTH);
        buffer.writeInt(data.labelColor.value());
    }

    public static UpdateRegionStyleRequestData decode(PacketBuffer buffer) {
        long requestId = buffer.readLong();
        String idText = buffer.readUtf(RegionLimits.MAX_ID_LENGTH);
        ArgbColor fillColor = new ArgbColor(buffer.readInt());
        String label = buffer.readUtf(RegionLimits.MAX_LABEL_LENGTH);
        ArgbColor labelColor = new ArgbColor(buffer.readInt());
        return new UpdateRegionStyleRequestData(requestId, idText, fillColor, label, labelColor);
    }

    public RegionId id() {
        return new RegionId(idText);
    }
}
