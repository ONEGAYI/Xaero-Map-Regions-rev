package com.suian.xaeroregionsrev.network.payload;

import net.minecraft.network.FriendlyByteBuf;

public record RegionRefreshRequestPacket() {
    public static void encode(RegionRefreshRequestPacket packet, FriendlyByteBuf buffer) {
    }

    public static RegionRefreshRequestPacket decode(FriendlyByteBuf buffer) {
        return new RegionRefreshRequestPacket();
    }
}
