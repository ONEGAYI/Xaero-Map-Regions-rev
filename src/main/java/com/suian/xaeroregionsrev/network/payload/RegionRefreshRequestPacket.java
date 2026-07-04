package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RegionRefreshRequestPacket() implements CustomPacketPayload {
    public static final Type<RegionRefreshRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "region_refresh_request")
    );
    public static final StreamCodec<FriendlyByteBuf, RegionRefreshRequestPacket> STREAM_CODEC = PacketCodecs.of(
            RegionRefreshRequestPacket::encode,
            RegionRefreshRequestPacket::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(RegionRefreshRequestPacket packet, FriendlyByteBuf buffer) {
    }

    public static RegionRefreshRequestPacket decode(FriendlyByteBuf buffer) {
        return new RegionRefreshRequestPacket();
    }
}
