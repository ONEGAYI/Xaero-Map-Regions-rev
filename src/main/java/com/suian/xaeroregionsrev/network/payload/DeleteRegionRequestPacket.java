package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionLimits;
import net.minecraft.network.FriendlyByteBuf;

public record DeleteRegionRequestPacket(RegionId id) {
    public static void encode(DeleteRegionRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.id.value());
    }

    public static DeleteRegionRequestPacket decode(FriendlyByteBuf buffer) {
        return new DeleteRegionRequestPacket(new RegionId(buffer.readUtf(RegionLimits.MAX_NAME_LENGTH)));
    }
}
