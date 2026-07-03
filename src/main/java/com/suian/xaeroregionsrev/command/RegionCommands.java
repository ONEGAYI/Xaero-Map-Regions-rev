package com.suian.xaeroregionsrev.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import com.suian.xaeroregionsrev.platform.ForgePermissionAdapter;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionColorParser;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import com.suian.xaeroregionsrev.region.RegionRequestValidator;
import com.suian.xaeroregionsrev.service.RegionService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
                        .requires(RegionCommands::canManage)
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("region", StringArgumentType.word())
                                        .executes(context -> messageOnly(context.getSource(), "hide command accepted for MVP data flow")))))
                .then(literal("visible")
                        .requires(RegionCommands::canManage)
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

    private static int createPolygon(CommandSourceStack source, String name, String argb, String pointsText) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        if (server == null) {
            throw commandError("Cannot create region without a running server.");
        }
        ServerLevel level = source.getLevel();
        long now = Instant.now().toEpochMilli();
        Region region;
        try {
            RegionRequestValidator.ValidatedRegionCreateRequest request = RegionRequestValidator.validateCreate(
                    name,
                    RegionColorParser.parse(argb),
                    name,
                    new ArgbColor(0xFFFFFFFF),
                    parsePoints(pointsText)
            );
            RegionId id = new RegionId(request.name());
            if (SERVICE.find(level, id).isPresent()) {
                throw new IllegalArgumentException("Region " + id.value() + " already exists.");
            }
            region = new Region(
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
        } catch (IllegalArgumentException exception) {
            throw commandError(exception.getMessage());
        }
        RegionNetwork.sendToAll(new RegionSyncPacket(SERVICE.snapshot(server)));
        source.sendSuccess(() -> Component.literal("Created region " + region.id().value()), true);
        return 1;
    }

    private static int messageOnly(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int createPoint(CommandSourceStack source, String playerName, String mode, String iconName, String label, String positionText) throws CommandSyntaxException {
        parseBlockPosition(positionText);
        return messageOnly(source, "createpoint is not implemented in the MVP data flow");
    }

    private static List<RegionPoint> parsePoints(String text) throws CommandSyntaxException {
        String[] pairs = text.split(";", -1);
        List<RegionPoint> points = new ArrayList<>(pairs.length);
        for (String pair : pairs) {
            String trimmedPair = pair.trim();
            if (trimmedPair.isEmpty()) {
                throw commandError("Region points cannot contain empty entries.");
            }
            String[] parts = trimmedPair.split(",", -1);
            if (parts.length != 2) {
                throw commandError("Point must be formatted as x,z.");
            }
            if (parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                throw commandError("Point coordinates cannot be blank.");
            }
            try {
                points.add(new RegionPoint(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())));
            } catch (NumberFormatException exception) {
                throw commandError("Point coordinates must be valid integers.");
            }
        }
        if (points.size() < 3) {
            throw commandError("Region polygon must contain at least three points.");
        }
        return points;
    }

    private static int[] parseBlockPosition(String text) throws CommandSyntaxException {
        String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            throw commandError("Position must be formatted as x y z.");
        }
        String[] parts = trimmedText.split("\\s+");
        if (parts.length != 3) {
            throw commandError("Position must be formatted as x y z.");
        }
        try {
            return new int[] {
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            };
        } catch (NumberFormatException exception) {
            throw commandError("Position coordinates must be valid integers.");
        }
    }

    private static CommandSyntaxException commandError(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }
}
