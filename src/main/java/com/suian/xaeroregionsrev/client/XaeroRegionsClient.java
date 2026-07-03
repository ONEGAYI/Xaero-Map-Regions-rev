package com.suian.xaeroregionsrev.client;

import com.suian.xaeroregionsrev.client.editor.RegionManagerScreen;
import com.suian.xaeroregionsrev.client.editor.RegionStyleEditScreen;
import com.suian.xaeroregionsrev.client.xaero.XaeroMapInputHandler;
import com.suian.xaeroregionsrev.client.xaero.XaeroMapOverlayController;
import com.suian.xaeroregionsrev.client.xaero.XaeroMapOverlayRenderer;
import com.suian.xaeroregionsrev.client.xaero.XaeroScreenDetector;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
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
        MinecraftForge.EVENT_BUS.addListener(XaeroRegionsClient::onClientLoggingOut);
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        Screen currentScreen = event.getCurrentScreen();
        Screen newScreen = event.getNewScreen();
        if (shouldResetSessionOnScreenChange(
                XaeroScreenDetector.isWorldMapScreen(currentScreen),
                XaeroScreenDetector.isWorldMapScreen(newScreen),
                isRegionChildScreen(newScreen)
        )) {
            XaeroMapOverlayController.reset();
        }
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        XaeroMapOverlayController.reset();
        ClientRegionCache.clear();
    }

    static boolean shouldResetSessionOnScreenChange(boolean currentIsWorldMap, boolean newIsWorldMap,
                                                   boolean newIsRegionChildScreen) {
        return currentIsWorldMap && !newIsWorldMap && !newIsRegionChildScreen;
    }

    private static boolean isRegionChildScreen(Screen screen) {
        return screen instanceof RegionManagerScreen
                || screen instanceof RegionStyleEditScreen
                || screen instanceof ConfirmScreen;
    }
}
