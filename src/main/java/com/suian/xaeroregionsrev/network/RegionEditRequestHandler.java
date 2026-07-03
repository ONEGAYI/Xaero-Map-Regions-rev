package com.suian.xaeroregionsrev.network;

import com.suian.xaeroregionsrev.network.payload.CreateRegionRequestPacket;
import com.suian.xaeroregionsrev.network.payload.DeleteRegionRequestPacket;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class RegionEditRequestHandler {
    private static final RegionService SERVICE = new RegionService();

    private RegionEditRequestHandler() {
    }

    public static void handleCreate(CreateRegionRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (!canManage(sender)) {
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
                return;
            }
            ServerLevel level = sender.serverLevel();
            if (!SERVICE.delete(level, packet.id())) {
                sendError(sender, "Region " + packet.id().value() + " was not found.");
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
            if (SERVICE.updateStyle(level, packet.id(), request.fillColor(), request.label(), request.labelColor(),
                    Instant.now().toEpochMilli()).isEmpty()) {
                sendError(sender, "Region " + packet.id().value() + " was not found.");
                return;
            }
            broadcastSnapshot(sender);
        });
        context.setPacketHandled(true);
    }

    private static boolean canManage(ServerPlayer sender) {
        return sender != null && ForgePermissionAdapter.from(sender).canManageRegions();
    }

    private static void sendError(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    private static void broadcastSnapshot(ServerPlayer sender) {
        MinecraftServer server = sender.getServer();
        if (server != null) {
            RegionNetwork.sendToAll(new RegionSyncPacket(allRegions(server)));
        }
    }

    private static List<Region> allRegions(MinecraftServer server) {
        List<Region> regions = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            regions.addAll(SERVICE.list(level));
        }
        return List.copyOf(regions);
    }
}
