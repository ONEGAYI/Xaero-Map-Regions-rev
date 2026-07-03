package com.suian.xaeroregionsrev.network;

import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.network.payload.CreateRegionRequestPacket;
import com.suian.xaeroregionsrev.network.payload.DeleteRegionRequestPacket;
import com.suian.xaeroregionsrev.network.payload.RegionRefreshRequestPacket;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import com.suian.xaeroregionsrev.network.payload.UpdateRegionStyleRequestPacket;
import com.suian.xaeroregionsrev.platform.ForgePermissionAdapter;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionRequestValidator;
import com.suian.xaeroregionsrev.service.RegionService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class RegionEditRequestHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RegionService SERVICE = new RegionService();
    private static final long REFRESH_COOLDOWN_NANOS = 2_000_000_000L;
    private static final Map<UUID, Long> LAST_REFRESH_BY_PLAYER = new HashMap<>();

    private RegionEditRequestHandler() {
    }

    public static void handleCreate(CreateRegionRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (!canManage(sender)) {
                sendPermissionError(sender);
                return;
            }
            ServerLevel level = sender.serverLevel();
            RegionRequestValidator.ValidatedRegionCreateRequest request;
            try {
                request = RegionRequestValidator.validateCreate(
                        packet.name(),
                        packet.fillColor(),
                        packet.label(),
                        packet.labelColor(),
                        packet.points()
                );
            } catch (IllegalArgumentException exception) {
                sendError(sender, exception.getMessage());
                return;
            }

            RegionId id = new RegionId(request.name());
            if (SERVICE.find(level, id).isPresent()) {
                sendError(sender, "Region " + id.value() + " already exists.");
                return;
            }

            long now = Instant.now().toEpochMilli();
            Region region = new Region(
                    id,
                    request.name(),
                    level.dimension().location().toString(),
                    request.fillColor(),
                    request.label(),
                    request.labelColor(),
                    "default",
                    "default",
                    request.points(),
                    now,
                    now
            );
            SERVICE.upsert(level, region);
            broadcastSnapshot(sender);
        });
        context.setPacketHandled(true);
    }

    public static void handleDelete(DeleteRegionRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (!canManage(sender)) {
                sendPermissionError(sender);
                return;
            }
            ServerLevel level = sender.serverLevel();
            RegionId id = parseRegionId(sender, packet.idText());
            if (id == null) {
                return;
            }
            if (!SERVICE.delete(level, id)) {
                sendError(sender, "Region " + id.value() + " was not found.");
                return;
            }
            broadcastSnapshot(sender);
        });
        context.setPacketHandled(true);
    }

    public static void handleUpdateStyle(
            UpdateRegionStyleRequestPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (!canManage(sender)) {
                sendPermissionError(sender);
                return;
            }
            RegionRequestValidator.ValidatedRegionStyleRequest request;
            try {
                request = RegionRequestValidator.validateStyle(packet.fillColor(), packet.label(), packet.labelColor());
            } catch (IllegalArgumentException exception) {
                sendError(sender, exception.getMessage());
                return;
            }

            ServerLevel level = sender.serverLevel();
            RegionId id = parseRegionId(sender, packet.idText());
            if (id == null) {
                return;
            }
            if (SERVICE.updateStyle(level, id, request.fillColor(), request.label(), request.labelColor(),
                    Instant.now().toEpochMilli()).isEmpty()) {
                sendError(sender, "Region " + id.value() + " was not found.");
                return;
            }
            broadcastSnapshot(sender);
        });
        context.setPacketHandled(true);
    }

    public static void handleRefresh(RegionRefreshRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (!canRefreshNow(sender.getUUID(), System.nanoTime())) {
                sendError(sender, "Region refresh is cooling down.");
                return;
            }
            MinecraftServer server = sender.getServer();
            if (server != null) {
                RegionNetwork.sendToPlayer(sender, new RegionSyncPacket(SERVICE.snapshot(server)));
            }
        });
        context.setPacketHandled(true);
    }

    private static boolean canManage(ServerPlayer sender) {
        return sender != null && ForgePermissionAdapter.from(sender).canManageRegions();
    }

    private static void sendError(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private static void sendPermissionError(ServerPlayer player) {
        sendError(player, "You must be an operator in creative mode to manage regions.");
    }

    private static RegionId parseRegionId(ServerPlayer player, String idText) {
        try {
            return new RegionId(idText);
        } catch (IllegalArgumentException exception) {
            sendError(player, exception.getMessage());
            return null;
        }
    }

    private static void broadcastSnapshot(ServerPlayer sender) {
        MinecraftServer server = sender.getServer();
        if (server != null) {
            RegionNetwork.sendToAll(new RegionSyncPacket(SERVICE.snapshot(server)));
        } else {
            LOGGER.warn("Skipped region snapshot broadcast because player {} has no server.",
                    sender.getGameProfile().getName());
        }
    }

    static boolean canRefreshNow(UUID playerId, long nowNanos) {
        Long lastRefreshAt = LAST_REFRESH_BY_PLAYER.get(playerId);
        if (lastRefreshAt != null && nowNanos >= lastRefreshAt
                && nowNanos - lastRefreshAt < REFRESH_COOLDOWN_NANOS) {
            return false;
        }
        LAST_REFRESH_BY_PLAYER.put(playerId, nowNanos);
        return true;
    }
}
