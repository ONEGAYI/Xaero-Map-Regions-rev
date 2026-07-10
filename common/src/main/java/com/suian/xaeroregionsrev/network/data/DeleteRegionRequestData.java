package com.suian.xaeroregionsrev.network.data;

import com.suian.xaeroregionsrev.network.buffer.PacketBuffer;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionLimits;

public record DeleteRegionRequestData(String idText) {
    public DeleteRegionRequestData(RegionId id) {
        this(id.value());
    }

    public static void encode(PacketBuffer buffer, DeleteRegionRequestData data) {
        buffer.writeUtf(data.idText, RegionLimits.MAX_ID_LENGTH);
    }

    public static DeleteRegionRequestData decode(PacketBuffer buffer) {
        return new DeleteRegionRequestData(buffer.readUtf(RegionLimits.MAX_ID_LENGTH));
    }

    public RegionId id() {
        return new RegionId(idText);
    }
}
