package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.region.RegionLimits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeleteRegionRequestPacket(String idText) implements CustomPacketPayload {
    public static final Type<DeleteRegionRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "delete_region_request")
    );
    public static final StreamCodec<FriendlyByteBuf, DeleteRegionRequestPacket> STREAM_CODEC = PacketCodecs.of(
            DeleteRegionRequestPacket::encode,
            DeleteRegionRequestPacket::decode
    );

    public DeleteRegionRequestPacket(com.suian.xaeroregionsrev.region.RegionId id) {
        this(id.value());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(DeleteRegionRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.idText, RegionLimits.MAX_NAME_LENGTH);
    }

    public static DeleteRegionRequestPacket decode(FriendlyByteBuf buffer) {
        return new DeleteRegionRequestPacket(buffer.readUtf(RegionLimits.MAX_NAME_LENGTH));
    }

    public com.suian.xaeroregionsrev.region.RegionId id() {
        return new com.suian.xaeroregionsrev.region.RegionId(idText);
    }
}
