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
import com.suian.xaeroregionsrev.platform.MinecraftPermissionAdapter;
import com.suian.xaeroregionsrev.platform.NeoForgeServerContext;
import com.suian.xaeroregionsrev.platform.RegionSavedDataStore;
import com.suian.xaeroregionsrev.region.ColorPaletteLimits;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionRequestValidator;
import com.suian.xaeroregionsrev.service.RegionService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RegionEditRequestHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RegionService SERVICE = new RegionService();
    private static final long REFRESH_COOLDOWN_NANOS = 2_000_000_000L;
    private static final Map<UUID, Long> LAST_REFRESH_BY_PLAYER = new HashMap<>();
    private static final String PERMISSION_ERROR = "You must be an operator in creative mode to manage regions.";

    private RegionEditRequestHandler() {
    }

    public static void handleCreate(CreateRegionRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer sender = serverPlayer(context);
            if (!canManage(sender)) {
                sendEditFailure(sender, packet.requestId(), PERMISSION_ERROR);
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
                sendEditFailure(sender, packet.requestId(), exception.getMessage());
                return;
            }

            RegionId id = new RegionId(request.name());
            RegionSavedDataStore store = RegionSavedDataStore.of(level);
            if (SERVICE.find(store, id).isPresent()) {
                sendEditFailure(sender, packet.requestId(), "Region " + id.value() + " already exists.");
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
            SERVICE.upsert(store, region);
            broadcastSnapshot(sender);
            sendEditSuccess(sender, packet.requestId());
        });
    }

    public static void handleDelete(DeleteRegionRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer sender = serverPlayer(context);
            if (!canManage(sender)) {
                sendPermissionError(sender);
                return;
            }
            ServerLevel level = sender.serverLevel();
            RegionId id = parseRegionId(sender, packet.idText());
            if (id == null) {
                return;
            }
            if (!SERVICE.delete(RegionSavedDataStore.of(level), id)) {
                sendError(sender, "Region " + id.value() + " was not found.");
                return;
            }
            broadcastSnapshot(sender);
        });
    }

    public static void handleUpdateStyle(
            UpdateRegionStyleRequestPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            ServerPlayer sender = serverPlayer(context);
            if (!canManage(sender)) {
                sendEditFailure(sender, packet.requestId(), PERMISSION_ERROR);
                return;
            }
            RegionRequestValidator.ValidatedRegionStyleRequest request;
            try {
                request = RegionRequestValidator.validateStyle(packet.fillColor(), packet.label(), packet.labelColor());
            } catch (IllegalArgumentException exception) {
                sendEditFailure(sender, packet.requestId(), exception.getMessage());
                return;
            }

            ServerLevel level = sender.serverLevel();
            RegionId id;
            try {
                id = new RegionId(packet.idText());
            } catch (IllegalArgumentException exception) {
                sendEditFailure(sender, packet.requestId(), exception.getMessage());
                return;
            }
            if (SERVICE.updateStyle(RegionSavedDataStore.of(level), id, request.fillColor(), request.label(), request.labelColor(),
                    Instant.now().toEpochMilli()).isEmpty()) {
                sendEditFailure(sender, packet.requestId(), "Region " + id.value() + " was not found.");
                return;
            }
            broadcastSnapshot(sender);
            sendEditSuccess(sender, packet.requestId());
        });
    }

    public static void handleRefresh(RegionRefreshRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer sender = serverPlayer(context);
            if (sender == null) {
                return;
            }
            if (!canRefreshNow(sender.getUUID(), System.nanoTime())) {
                sendError(sender, "Region refresh is cooling down.");
                return;
            }
            MinecraftServer server = sender.getServer();
            if (server != null) {
                NeoForgeServerContext serverContext = new NeoForgeServerContext(server);
                RegionNetwork.sendToPlayer(sender, new RegionSyncPacket(SERVICE.snapshot(serverContext)));
                RegionNetwork.sendColorHistoryToPlayer(sender, new ColorHistorySyncPacket(SERVICE.colorHistory(serverContext)));
            }
        });
    }

    public static void handleRememberColor(
            ColorHistoryUpdateRequestPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            ServerPlayer sender = serverPlayer(context);
            if (!canManage(sender)) {
                sendPermissionError(sender);
                return;
            }
            MinecraftServer server = sender.getServer();
            if (server != null) {
                RegionNetwork.sendColorHistoryToAll(new ColorHistorySyncPacket(
                        SERVICE.rememberColor(new NeoForgeServerContext(server), packet.color(), ColorPaletteLimits.MAX_COLORS)
                ));
            }
        });
    }

    private static ServerPlayer serverPlayer(IPayloadContext context) {
        Player player = context.player();
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private static boolean canManage(ServerPlayer sender) {
        return sender != null && MinecraftPermissionAdapter.from(sender).canManageRegions();
    }

    static boolean editErrorsUseActionBar() {
        return true;
    }

    private static void sendError(ServerPlayer player, String message) {
        if (player != null) {
            player.displayClientMessage(Component.literal(message), editErrorsUseActionBar());
        }
    }

    private static void sendPermissionError(ServerPlayer player) {
        sendError(player, PERMISSION_ERROR);
    }

    private static void sendEditSuccess(ServerPlayer player, long requestId) {
        sendEditResult(player, requestId, true, true, "Region saved.");
    }

    private static void sendEditFailure(ServerPlayer player, long requestId, String message) {
        sendEditResult(player, requestId, false, false, message);
    }

    private static void sendEditResult(ServerPlayer player, long requestId, boolean success, boolean closeScreen,
                                       String message) {
        if (player != null) {
            RegionNetwork.sendEditResultToPlayer(player,
                    new RegionEditResultPacket(requestId, success, closeScreen, message));
        }
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
            RegionNetwork.sendToAll(new RegionSyncPacket(SERVICE.snapshot(new NeoForgeServerContext(server))));
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
