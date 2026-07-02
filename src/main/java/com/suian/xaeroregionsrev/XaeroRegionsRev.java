package com.suian.xaeroregionsrev;

import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.command.RegionCommands;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import com.suian.xaeroregionsrev.service.RegionService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.List;

@Mod(XaeroRegionsRev.MOD_ID)
public final class XaeroRegionsRev {
    public static final String MOD_ID = "xaeroregionsrev";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final RegionService REGION_SERVICE = new RegionService();

    public XaeroRegionsRev() {
        RegionNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Xaero Map Regions Rev loaded.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RegionCommands.register(event);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            RegionNetwork.sendToPlayer(player, new RegionSyncPacket(List.copyOf(REGION_SERVICE.list(level))));
        }
    }
}
