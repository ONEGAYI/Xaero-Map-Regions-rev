package com.suian.xaeroregionsrev.network;

import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.client.ClientColorHistoryCache;
import com.suian.xaeroregionsrev.client.ClientRegionCache;
import com.suian.xaeroregionsrev.network.payload.ColorHistorySyncPacket;
import com.suian.xaeroregionsrev.network.payload.ColorHistoryUpdateRequestPacket;
import com.suian.xaeroregionsrev.network.payload.CreateRegionRequestPacket;
import com.suian.xaeroregionsrev.network.payload.DeleteRegionRequestPacket;
import com.suian.xaeroregionsrev.network.payload.RegionRefreshRequestPacket;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import com.suian.xaeroregionsrev.network.payload.UpdateRegionStyleRequestPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

public final class RegionNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL_VERSION = "5";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(XaeroRegionsRev.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int packetId;
    private static boolean registered;

    private RegionNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(RegionSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RegionSyncPacket::encode)
                .decoder(RegionSyncPacket::decode)
                .consumerMainThread((packet, contextSupplier) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRegionCache.replaceAll(packet.regions()));
                    contextSupplier.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(ColorHistorySyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ColorHistorySyncPacket::encode)
                .decoder(ColorHistorySyncPacket::decode)
                .consumerMainThread((packet, contextSupplier) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientColorHistoryCache.replaceAll(packet.colors()));
                    contextSupplier.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(CreateRegionRequestPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CreateRegionRequestPacket::encode)
                .decoder(CreateRegionRequestPacket::decode)
                .consumerMainThread(RegionEditRequestHandler::handleCreate)
                .add();

        CHANNEL.messageBuilder(DeleteRegionRequestPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteRegionRequestPacket::encode)
                .decoder(DeleteRegionRequestPacket::decode)
                .consumerMainThread(RegionEditRequestHandler::handleDelete)
                .add();

        CHANNEL.messageBuilder(UpdateRegionStyleRequestPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateRegionStyleRequestPacket::encode)
                .decoder(UpdateRegionStyleRequestPacket::decode)
                .consumerMainThread(RegionEditRequestHandler::handleUpdateStyle)
                .add();

        CHANNEL.messageBuilder(RegionRefreshRequestPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RegionRefreshRequestPacket::encode)
                .decoder(RegionRefreshRequestPacket::decode)
                .consumerMainThread(RegionEditRequestHandler::handleRefresh)
                .add();

        CHANNEL.messageBuilder(ColorHistoryUpdateRequestPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ColorHistoryUpdateRequestPacket::encode)
                .decoder(ColorHistoryUpdateRequestPacket::decode)
                .consumerMainThread(RegionEditRequestHandler::handleRememberColor)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, RegionSyncPacket packet) {
        LOGGER.info("Sending {} region(s) to player {}.", packet.regions().size(), player.getGameProfile().getName());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(RegionSyncPacket packet) {
        LOGGER.info("Broadcasting {} region(s) to all players.", packet.regions().size());
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendColorHistoryToPlayer(ServerPlayer player, ColorHistorySyncPacket packet) {
        LOGGER.info("Sending {} color history item(s) to player {}.",
                packet.colors().size(), player.getGameProfile().getName());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendColorHistoryToAll(ColorHistorySyncPacket packet) {
        LOGGER.info("Broadcasting {} color history item(s) to all players.", packet.colors().size());
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
