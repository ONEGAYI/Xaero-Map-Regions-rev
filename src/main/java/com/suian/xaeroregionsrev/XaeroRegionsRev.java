package com.suian.xaeroregionsrev;

import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.command.RegionCommands;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.ColorHistorySyncPacket;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.service.RegionService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(XaeroRegionsRev.MOD_ID)
public final class XaeroRegionsRev {
    public static final String MOD_ID = "xaeroregionsrev";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final RegionService REGION_SERVICE = new RegionService();

    public XaeroRegionsRev(IEventBus modEventBus) {
        modEventBus.addListener(RegionNetwork::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.suian.xaeroregionsrev.client.XaeroRegionsClient.register(modEventBus);
        }
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Xaero Map Regions Rev loaded.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RegionCommands.register(event);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.getServer();
            if (server == null) {
                LOGGER.warn("Skipped region sync for {} because no server was available.", player.getGameProfile().getName());
                return;
            }
            RegionNetwork.sendToPlayer(player, new RegionSyncPacket(allRegions(server)));
            RegionNetwork.sendColorHistoryToPlayer(player,
                    new ColorHistorySyncPacket(REGION_SERVICE.colorHistory(server)));
        }
    }

    private static List<Region> allRegions(MinecraftServer server) {
        List<Region> regions = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            regions.addAll(REGION_SERVICE.list(level));
        }
        return List.copyOf(regions);
    }
}
