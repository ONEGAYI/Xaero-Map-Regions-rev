package com.suian.xaeroregionsrev.client;

import com.suian.xaeroregionsrev.client.xaero.XaeroMapInputHandler;
import com.suian.xaeroregionsrev.client.xaero.XaeroMapOverlayRenderer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class XaeroRegionsClient {
    private static boolean registered;

    private XaeroRegionsClient() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RegionKeyMappings::register);
        MinecraftForge.EVENT_BUS.addListener(XaeroMapOverlayRenderer::onScreenRenderPost);
        MinecraftForge.EVENT_BUS.addListener(XaeroMapInputHandler::onKeyPressed);
        MinecraftForge.EVENT_BUS.addListener(XaeroMapInputHandler::onMouseButtonPressed);
    }
}
