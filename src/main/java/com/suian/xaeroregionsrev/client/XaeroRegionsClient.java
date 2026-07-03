package com.suian.xaeroregionsrev.client;

import com.suian.xaeroregionsrev.client.xaero.XaeroMapInputHandler;
import com.suian.xaeroregionsrev.client.xaero.XaeroMapOverlayController;
import com.suian.xaeroregionsrev.client.xaero.XaeroMapOverlayRenderer;
import com.suian.xaeroregionsrev.client.xaero.XaeroScreenDetector;
import net.minecraftforge.client.event.ScreenEvent;
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
        MinecraftForge.EVENT_BUS.addListener(XaeroRegionsClient::onScreenOpening);
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (XaeroScreenDetector.isWorldMapScreen(event.getCurrentScreen())
                && !XaeroScreenDetector.isWorldMapScreen(event.getNewScreen())) {
            XaeroMapOverlayController.reset();
        }
    }
}
