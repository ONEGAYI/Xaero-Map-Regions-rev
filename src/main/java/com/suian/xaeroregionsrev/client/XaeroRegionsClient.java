package com.suian.xaeroregionsrev.client;

import com.suian.xaeroregionsrev.client.command.RegionClientCommands;
import com.suian.xaeroregionsrev.client.editor.RegionManagerScreen;
import com.suian.xaeroregionsrev.client.editor.RegionStyleEditScreen;
import com.suian.xaeroregionsrev.client.xaero.MapProjectionAdapter;
import com.suian.xaeroregionsrev.client.xaero.XaeroMapInputHandler;
import com.suian.xaeroregionsrev.client.xaero.XaeroMapOverlayController;
import com.suian.xaeroregionsrev.client.xaero.XaeroMapOverlayRenderer;
import com.suian.xaeroregionsrev.client.xaero.XaeroScreenDetector;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class XaeroRegionsClient {
    private static boolean registered;

    private XaeroRegionsClient() {
    }

    public static void register(IEventBus modEventBus) {
        if (registered) {
            return;
        }
        registered = true;
        modEventBus.addListener(RegionKeyMappings::register);
        NeoForge.EVENT_BUS.addListener(XaeroMapOverlayRenderer::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(XaeroMapInputHandler::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(XaeroMapInputHandler::onMouseButtonPressed);
        NeoForge.EVENT_BUS.addListener(XaeroRegionsClient::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(XaeroRegionsClient::onClientLoggingOut);
        NeoForge.EVENT_BUS.addListener(RegionClientCommands::register);
        MapProjectionAdapter.shared().syncCalibrationEnabledFromConfig();
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
        ClientColorHistoryCache.clear();
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
