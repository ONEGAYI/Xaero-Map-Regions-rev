package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.RegionLimits;
import net.minecraft.network.FriendlyByteBuf;

public record DeleteRegionRequestPacket(String idText) {
    public DeleteRegionRequestPacket(com.suian.xaeroregionsrev.region.RegionId id) {
        this(id.value());
    }

    public static void encode(DeleteRegionRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.idText);
    }

    public static DeleteRegionRequestPacket decode(FriendlyByteBuf buffer) {
        return new DeleteRegionRequestPacket(buffer.readUtf(RegionLimits.MAX_NAME_LENGTH));
    }

    public com.suian.xaeroregionsrev.region.RegionId id() {
        return new com.suian.xaeroregionsrev.region.RegionId(idText);
    }
}
