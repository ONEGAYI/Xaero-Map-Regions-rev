package com.suian.xaeroregionsrev.client.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.List;

public final class RegionEditorOverlay {
    public static final int EDIT_BUTTON_WIDTH = 72;
    public static final int EDIT_BUTTON_HEIGHT = 20;
    private static final int EDIT_BUTTON_MARGIN = 12;

    private RegionEditorOverlay() {
    }

    public enum MouseButton {
        LEFT,
        RIGHT,
        MIDDLE,
        OTHER
    }

    public enum Action {
        IGNORED,
        TOGGLED_EDITING,
        ADDED_DRAFT_POINT,
        SELECTED_REGION,
        OPEN_CONTEXT_MENU,
        OPEN_CREATE_FORM
    }

    public record ScreenPoint(float x, float y) {
    }

    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    public static Rect editButtonBounds(int screenWidth, int screenHeight) {
        return new Rect(screenWidth - EDIT_BUTTON_WIDTH - EDIT_BUTTON_MARGIN, EDIT_BUTTON_MARGIN,
                EDIT_BUTTON_WIDTH, EDIT_BUTTON_HEIGHT);
    }

    public static ScreenPoint labelAnchor(List<ScreenPoint> points) {
        if (points.isEmpty()) {
            return new ScreenPoint(0.0F, 0.0F);
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (ScreenPoint point : points) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }
        return new ScreenPoint((minX + maxX) / 2.0F, (minY + maxY) / 2.0F);
    }

    public static boolean shouldRenderDraftFill(List<ScreenPoint> points) {
        return points.size() >= 3;
    }

    public static boolean isProjectedBoundsVisible(List<ScreenPoint> points, int screenWidth, int screenHeight) {
        if (points.isEmpty()) {
            return false;
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (ScreenPoint point : points) {
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            maxX = Math.max(maxX, point.x());
            maxY = Math.max(maxY, point.y());
        }
        return maxX >= 0 && maxY >= 0 && minX <= screenWidth && minY <= screenHeight;
    }

    public static void renderButton(GuiGraphics graphics, int screenWidth, int screenHeight, boolean editing) {
        Rect bounds = editButtonBounds(screenWidth, screenHeight);
        int fill = editing ? 0xCC2F855A : 0xCC1A365D;
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), fill);
        Component label = Component.translatable(editing ? "button.xaeroregionsrev.done" : "button.xaeroregionsrev.edit");
        int textX = bounds.x() + (bounds.width() - Minecraft.getInstance().font.width(label)) / 2;
        graphics.drawString(Minecraft.getInstance().font, label, textX, bounds.y() + 6, 0xFFFFFFFF, false);
    }

    public static void renderDraft(GuiGraphics graphics, List<Vector2f> points) {
        if (points.isEmpty()) {
            return;
        }
        if (shouldRenderDraftFill(points.stream()
                .map(point -> new ScreenPoint(point.x(), point.y()))
                .toList())) {
            drawFilledPolygon(graphics, points, 0x44E2B93B);
        }
        for (Vector2f point : points) {
            int x = Math.round(point.x());
            int y = Math.round(point.y());
            graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFFE2B93B);
        }
        for (int i = 1; i < points.size(); i++) {
            drawLine(graphics, points.get(i - 1), points.get(i), 0xFFE2B93B);
        }
        if (points.size() >= 3) {
            drawLine(graphics, points.get(points.size() - 1), points.get(0), 0x99E2B93B);
        }
    }

    public static void drawLine(GuiGraphics graphics, Vector2f from, Vector2f to, int color) {
        int steps = Math.max(1, Math.round(Math.max(Math.abs(to.x() - from.x()), Math.abs(to.y() - from.y()))));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(from.x() + (to.x() - from.x()) * t);
            int y = Math.round(from.y() + (to.y() - from.y()) * t);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void drawFilledPolygon(GuiGraphics graphics, List<Vector2f> points, int color) {
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
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try {
            buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            for (Vector2f point : points) {
                buffer.vertex(matrix, point.x(), point.y(), 0.0F).color(red, green, blue, alpha).endVertex();
            }
            tesselator.end();
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    public static final class ActionRouter {
        private final RegionEditSession session;

        public ActionRouter(RegionEditSession session) {
            this.session = session;
        }

        public Action handleMouse(MouseButton button, double mouseX, double mouseY, RegionPoint worldPoint,
                                  List<Region> regions, String dimension) {
            return handleMouse(button, mouseX, mouseY, 800, 600, worldPoint, regions, dimension);
        }

        public Action handleMouse(MouseButton button, double mouseX, double mouseY, int screenWidth, int screenHeight,
                                  RegionPoint worldPoint, List<Region> regions, String dimension) {
            if (button == MouseButton.LEFT && editButtonBounds(screenWidth, screenHeight).contains(mouseX, mouseY)) {
                session.toggleEditing();
                return Action.TOGGLED_EDITING;
            }
            if (!session.isEditing()) {
                return Action.IGNORED;
            }
            if (button == MouseButton.MIDDLE && session.addDraftPoint(worldPoint)) {
                return Action.ADDED_DRAFT_POINT;
            }
            if (button == MouseButton.LEFT) {
                return RegionSelection.selectTopmost(regions, dimension, worldPoint.x(), worldPoint.z())
                        .map(region -> {
                            session.select(region.id());
                            return Action.SELECTED_REGION;
                        })
                        .orElse(Action.IGNORED);
            }
            if (button == MouseButton.RIGHT && session.selectedRegionId().isPresent()) {
                return Action.OPEN_CONTEXT_MENU;
            }
            return Action.IGNORED;
        }

        public Action handleEnter() {
            return session.canSubmitDraft() ? Action.OPEN_CREATE_FORM : Action.IGNORED;
        }
    }
}
