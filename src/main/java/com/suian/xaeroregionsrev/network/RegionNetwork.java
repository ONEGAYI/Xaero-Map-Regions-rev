package com.suian.xaeroregionsrev.network;

import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.network.payload.ColorHistorySyncPacket;
import com.suian.xaeroregionsrev.network.payload.ColorHistoryUpdateRequestPacket;
import com.suian.xaeroregionsrev.network.payload.CreateRegionRequestPacket;
import com.suian.xaeroregionsrev.network.payload.DeleteRegionRequestPacket;
import com.suian.xaeroregionsrev.network.payload.RegionEditResultPacket;
import com.suian.xaeroregionsrev.network.payload.RegionRefreshRequestPacket;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import com.suian.xaeroregionsrev.network.payload.UpdateRegionStyleRequestPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

public final class RegionNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL_VERSION = "6";

    private RegionNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(RegionSyncPacket.TYPE, RegionSyncPacket.STREAM_CODEC,
                ClientboundPayloadDispatch::handleRegionSync);
        registrar.playToClient(ColorHistorySyncPacket.TYPE, ColorHistorySyncPacket.STREAM_CODEC,
                ClientboundPayloadDispatch::handleColorHistorySync);
        registrar.playToClient(RegionEditResultPacket.TYPE, RegionEditResultPacket.STREAM_CODEC,
                ClientboundPayloadDispatch::handleRegionEditResult);

        registrar.playToServer(CreateRegionRequestPacket.TYPE, CreateRegionRequestPacket.STREAM_CODEC,
                RegionEditRequestHandler::handleCreate);
        registrar.playToServer(DeleteRegionRequestPacket.TYPE, DeleteRegionRequestPacket.STREAM_CODEC,
                RegionEditRequestHandler::handleDelete);
        registrar.playToServer(UpdateRegionStyleRequestPacket.TYPE, UpdateRegionStyleRequestPacket.STREAM_CODEC,
                RegionEditRequestHandler::handleUpdateStyle);
        registrar.playToServer(RegionRefreshRequestPacket.TYPE, RegionRefreshRequestPacket.STREAM_CODEC,
                RegionEditRequestHandler::handleRefresh);
        registrar.playToServer(ColorHistoryUpdateRequestPacket.TYPE, ColorHistoryUpdateRequestPacket.STREAM_CODEC,
                RegionEditRequestHandler::handleRememberColor);
    }

    public static void sendToPlayer(ServerPlayer player, RegionSyncPacket packet) {
        LOGGER.info("Sending {} region(s) to player {}.", packet.regions().size(), player.getGameProfile().getName());
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendEditResultToPlayer(ServerPlayer player, RegionEditResultPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToAll(RegionSyncPacket packet) {
        LOGGER.info("Broadcasting {} region(s) to all players.", packet.regions().size());
        PacketDistributor.sendToAllPlayers(packet);
    }

    public static void sendColorHistoryToPlayer(ServerPlayer player, ColorHistorySyncPacket packet) {
        LOGGER.info("Sending {} color history item(s) to player {}.",
                packet.colors().size(), player.getGameProfile().getName());
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendColorHistoryToAll(ColorHistorySyncPacket packet) {
        LOGGER.info("Broadcasting {} color history item(s) to all players.", packet.colors().size());
        PacketDistributor.sendToAllPlayers(packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
