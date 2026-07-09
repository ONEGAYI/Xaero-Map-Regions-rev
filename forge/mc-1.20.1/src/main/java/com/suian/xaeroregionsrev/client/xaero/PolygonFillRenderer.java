package com.suian.xaeroregionsrev.client.xaero;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.List;

public final class PolygonFillRenderer {
    private PolygonFillRenderer() {
    }

    public static void fill(GuiGraphics graphics, List<Vector2f> points, int color) {
        List<PolygonTriangulator.Triangle> triangles = PolygonTriangulator.triangulate(points);
        if (triangles.isEmpty()) {
            return;
        }

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
            buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            for (PolygonTriangulator.Triangle triangle : triangles) {
                vertex(buffer, matrix, triangle.a(), red, green, blue, alpha);
                vertex(buffer, matrix, triangle.b(), red, green, blue, alpha);
                vertex(buffer, matrix, triangle.c(), red, green, blue, alpha);
            }
            tesselator.end();
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vector2f point,
                               float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, point.x(), point.y(), 0.0F).color(red, green, blue, alpha).endVertex();
    }
}
