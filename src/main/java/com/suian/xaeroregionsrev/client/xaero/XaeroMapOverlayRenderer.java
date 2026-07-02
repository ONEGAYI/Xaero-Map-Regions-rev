package com.suian.xaeroregionsrev.client.xaero;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.suian.xaeroregionsrev.client.ClientRegionCache;
import com.suian.xaeroregionsrev.region.Region;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.List;

public final class XaeroMapOverlayRenderer {
    private static final MapProjectionAdapter PROJECTION = new MapProjectionAdapter();

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
        renderRegions(event.getGuiGraphics(), screen, ClientRegionCache.regions());
    }

    private static void renderRegions(GuiGraphics graphics, Screen screen, List<Region> regions) {
        for (Region region : regions) {
            if (region.points().size() < 3) {
                continue;
            }
            drawPolygon(graphics, screen, region);
        }
    }

    private static void drawPolygon(GuiGraphics graphics, Screen screen, Region region) {
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        int color = region.color().value();
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        float red = ((color >>> 16) & 0xFF) / 255.0F;
        float green = ((color >>> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        for (var point : region.points()) {
            Vector2f projected = PROJECTION.project(screen, point);
            buffer.vertex(matrix, projected.x(), projected.y(), 0.0F).color(red, green, blue, alpha).endVertex();
        }
        tesselator.end();
    }
}
