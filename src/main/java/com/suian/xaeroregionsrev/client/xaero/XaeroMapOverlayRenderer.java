package com.suian.xaeroregionsrev.client.xaero;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.client.ClientRegionCache;
import com.suian.xaeroregionsrev.region.RegionPoint;
import com.suian.xaeroregionsrev.region.Region;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.slf4j.Logger;

import java.util.List;

public final class XaeroMapOverlayRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MapProjectionAdapter PROJECTION = new MapProjectionAdapter();
    private static String lastLoggedScreen = "";
    private static String lastLoggedDimension = "";
    private static int lastLoggedCachedRegions = -1;
    private static int lastLoggedRenderedRegions = -1;

    private XaeroMapOverlayRenderer() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(XaeroMapOverlayRenderer::onScreenRenderPost);
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
        PROJECTION.calibrate(screen, event.getMouseX(), event.getMouseY(), System.currentTimeMillis());
        renderRegions(event.getGuiGraphics(), screen, ClientRegionCache.regions(), currentDimension);
    }

    private static void renderRegions(GuiGraphics graphics, Screen screen, List<Region> regions, String currentDimension) {
        int renderedRegions = 0;
        for (Region region : regions) {
            if (region.points().size() < 3 || !region.dimension().equals(currentDimension)) {
                continue;
            }
            List<Vector2f> projected = project(region.points(), screen);
            if (!XaeroMapOverlayController.isProjectedRegionVisible(projected, screen.width, screen.height)) {
                continue;
            }
            drawPolygon(graphics, projected, region.color().value());
            XaeroMapOverlayController.renderRegionDecorations(graphics, screen, region, projected);
            renderedRegions++;
        }
        XaeroMapOverlayController.renderEditor(graphics, screen);
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

    private static void drawPolygon(GuiGraphics graphics, List<Vector2f> projected, int color) {
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        float red = ((color >>> 16) & 0xFF) / 255.0F;
        float green = ((color >>> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            for (var point : projected) {
                buffer.vertex(matrix, point.x(), point.y(), 0.0F).color(red, green, blue, alpha).endVertex();
            }
            tesselator.end();
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }
}
