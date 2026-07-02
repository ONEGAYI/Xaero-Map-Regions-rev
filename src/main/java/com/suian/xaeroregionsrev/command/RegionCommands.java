package com.suian.xaeroregionsrev.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import com.suian.xaeroregionsrev.platform.ForgePermissionAdapter;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.PointMarker;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import com.suian.xaeroregionsrev.service.RegionService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class RegionCommands {
    private static final RegionService SERVICE = new RegionService();

    private RegionCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("region")
                .then(literal("createpoly")
                        .requires(RegionCommands::canManage)
                        .then(argument("name", StringArgumentType.word())
                                .then(argument("argb", StringArgumentType.word())
                                        .then(argument("points", StringArgumentType.greedyString())
                                                .executes(context -> createPolygon(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name"),
                                                        StringArgumentType.getString(context, "argb"),
                                                        StringArgumentType.getString(context, "points")
                                                ))))))
                .then(literal("hide")
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("region", StringArgumentType.word())
                                        .executes(context -> messageOnly(context.getSource(), "hide command accepted for MVP data flow")))))
                .then(literal("visible")
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("region", StringArgumentType.word())
                                        .executes(context -> messageOnly(context.getSource(), "visible command accepted for MVP data flow")))))
                .then(literal("createpoint")
                        .requires(RegionCommands::canManage)
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("mode", StringArgumentType.word())
                                        .then(argument("iconname", StringArgumentType.word())
                                                .then(argument("label", StringArgumentType.word())
                                                        .then(argument("position", StringArgumentType.greedyString())
                                                                .executes(context -> createPoint(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "player"),
                                                                        StringArgumentType.getString(context, "mode"),
                                                                        StringArgumentType.getString(context, "iconname"),
                                                                        StringArgumentType.getString(context, "label"),
                                                                        StringArgumentType.getString(context, "position")
                                                                ))))))))
                .then(literal("delpoint")
                        .requires(RegionCommands::canManage)
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("position", StringArgumentType.greedyString())
                                        .executes(context -> messageOnly(context.getSource(), "delpoint command accepted for MVP data flow"))))));
    }

    private static boolean canManage(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return ForgePermissionAdapter.from(player).canManageRegions();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int createPolygon(CommandSourceStack source, String name, String argb, String pointsText) {
        ServerLevel level = source.getLevel();
        long now = Instant.now().toEpochMilli();
        Region region = new Region(
                new RegionId(name),
                name,
                level.dimension().location().toString(),
                new ArgbColor((int) Long.parseLong(argb.replace("0x", ""), 16)),
                "default",
                "default",
                parsePoints(pointsText),
                now,
                now
        );
        SERVICE.upsert(level, region);
        RegionNetwork.sendToAll(new RegionSyncPacket(List.copyOf(SERVICE.list(level))));
        source.sendSuccess(() -> Component.literal("Created region " + region.id().value()), true);
        return 1;
    }

    private static int messageOnly(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int createPoint(CommandSourceStack source, String playerName, String mode, String iconName, String label, String positionText) {
        int[] position = parseBlockPosition(positionText);
        UUID targetId = UUID.nameUUIDFromBytes(playerName.getBytes(StandardCharsets.UTF_8));
        PointMarker marker = new PointMarker(targetId, mode, iconName, label, position[0], position[1], position[2]);
        source.sendSuccess(() -> Component.literal("Created point marker " + marker.label() + " for " + playerName), true);
        return 1;
    }

    private static List<RegionPoint> parsePoints(String text) {
        String[] pairs = text.split(";");
        List<RegionPoint> points = new ArrayList<>(pairs.length);
        for (String pair : pairs) {
            String[] parts = pair.trim().split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Point must be formatted as x,z.");
            }
            points.add(new RegionPoint(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())));
        }
        return points;
    }

    private static int[] parseBlockPosition(String text) {
        String[] parts = text.trim().split(" ");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Position must be formatted as x y z.");
        }
        return new int[] {
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }
}
