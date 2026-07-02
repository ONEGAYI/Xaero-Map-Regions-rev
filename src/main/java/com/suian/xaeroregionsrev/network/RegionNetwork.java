package com.suian.xaeroregionsrev.network;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RegionNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(XaeroRegionsRev.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int packetId;

    private RegionNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                RegionSyncPacket.class,
                RegionSyncPacket::encode,
                RegionSyncPacket::decode,
                (packet, contextSupplier) -> contextSupplier.get().setPacketHandled(true)
        );
    }
}
