package com.suian.xaeroregionsrev.client.xaero;

import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.client.ClientRegionCache;
import com.suian.xaeroregionsrev.region.RegionPoint;
import com.suian.xaeroregionsrev.region.Region;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.joml.Vector2f;
import org.slf4j.Logger;

import java.util.List;

public final class XaeroMapOverlayRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MapProjectionAdapter PROJECTION = MapProjectionAdapter.shared();
    private static String lastLoggedScreen = "";
    private static String lastLoggedDimension = "";
    private static int lastLoggedCachedRegions = -1;
    private static int lastLoggedRenderedRegions = -1;

    private XaeroMapOverlayRenderer() {
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!XaeroScreenDetector.isWorldMapScreen(screen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        String currentDimension = minecraft.level.dimension().location().toString();
        PROJECTION.calibrate(screen, event.getMouseX(), event.getMouseY(), System.nanoTime());
        renderRegions(event.getGuiGraphics(), screen, ClientRegionCache.regions(), currentDimension,
                event.getMouseX(), event.getMouseY());
    }

    private static void renderRegions(GuiGraphics graphics, Screen screen, List<Region> regions, String currentDimension,
                                      int mouseX, int mouseY) {
        int renderedRegions = 0;
        for (Region region : regions) {
            if (region.points().size() < 3 || !region.dimension().equals(currentDimension)) {
                continue;
            }
            List<Vector2f> projected = project(region.points(), screen);
            if (!XaeroMapOverlayController.isProjectedRegionVisible(projected, screen.width, screen.height)) {
                continue;
            }
            PolygonFillRenderer.fill(graphics, projected, region.color().value());
            XaeroMapOverlayController.renderRegionDecorations(graphics, screen, region, projected, mouseX, mouseY);
            renderedRegions++;
        }
        XaeroMapOverlayController.renderEditor(graphics, screen, mouseX, mouseY);
        logRender(screen, regions.size(), renderedRegions, currentDimension);
    }

    private static void logRender(Screen screen, int cachedRegions, int renderedRegions, String currentDimension) {
        String screenClass = screen.getClass().getName();
        if (screenClass.equals(lastLoggedScreen)
                && currentDimension.equals(lastLoggedDimension)
                && cachedRegions == lastLoggedCachedRegions
                && renderedRegions == lastLoggedRenderedRegions) {
            return;
        }
        lastLoggedScreen = screenClass;
        lastLoggedDimension = currentDimension;
        lastLoggedCachedRegions = cachedRegions;
        lastLoggedRenderedRegions = renderedRegions;
        LOGGER.info("Xaero map overlay: screen={}, cachedRegions={}, renderedRegions={}, dimension={}.",
                screenClass, cachedRegions, renderedRegions, currentDimension);
    }

    private static List<Vector2f> project(List<RegionPoint> points, Screen screen) {
        return points.stream()
                .map(point -> PROJECTION.project(screen, point))
                .toList();
    }

}
