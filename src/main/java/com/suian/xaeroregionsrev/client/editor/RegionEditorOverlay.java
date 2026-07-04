package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.client.xaero.PolygonFillRenderer;
import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;

import java.util.List;
import java.util.Optional;

public final class RegionEditorOverlay {
    public static final int EDIT_BUTTON_WIDTH = 22;
    public static final int EDIT_BUTTON_HEIGHT = 22;
    private static final int EDIT_BUTTON_MARGIN = 12;
    private static final int ICON_BUTTON_GAP = 6;
    private static final int ICON_SIZE = 16;
    private static final int ICON_PADDING = 3;
    private static final int ICON_TEXTURE_WIDTH = 128;
    private static final int ICON_TEXTURE_HEIGHT = 16;
    private static final int EDIT_ICON_INDEX = 0;
    private static final int EXIT_EDIT_ICON_INDEX = 1;
    private static final ResourceLocation ICONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            XaeroRegionsRev.MOD_ID, "textures/gui/region_editor_icons.png");

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

    public enum ToolbarAction {
        ADD_DRAFT_POINT_HINT("tooltip.xaeroregionsrev.add_draft_point", 2),
        OPEN_REGION_MANAGER("tooltip.xaeroregionsrev.open_region_manager", 3),
        UNDO_DRAFT_POINT("tooltip.xaeroregionsrev.undo_draft_point", 4),
        REDO_DRAFT_POINT("tooltip.xaeroregionsrev.redo_draft_point", 5),
        SUBMIT_DRAFT("tooltip.xaeroregionsrev.submit_draft", 6),
        CLEAR_DRAFT("tooltip.xaeroregionsrev.clear_draft", 7);

        private final String tooltipKey;
        private final int iconIndex;

        ToolbarAction(String tooltipKey, int iconIndex) {
            this.tooltipKey = tooltipKey;
            this.iconIndex = iconIndex;
        }

        public String tooltipKey() {
            return tooltipKey;
        }

        public int iconIndex() {
            return iconIndex;
        }
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

    public static Rect toolbarActionBounds(int screenWidth, int screenHeight, ToolbarAction action) {
        ToolbarAction[] actions = ToolbarAction.values();
        Rect editButton = editButtonBounds(screenWidth, screenHeight);
        int totalWidth = actions.length * EDIT_BUTTON_WIDTH + (actions.length - 1) * ICON_BUTTON_GAP;
        int startX = editButton.x() - ICON_BUTTON_GAP - totalWidth;
        int index = action.ordinal();
        return new Rect(startX + index * (EDIT_BUTTON_WIDTH + ICON_BUTTON_GAP), editButton.y(),
                EDIT_BUTTON_WIDTH, EDIT_BUTTON_HEIGHT);
    }

    public static Optional<ToolbarAction> toolbarActionAt(double mouseX, double mouseY, int screenWidth,
                                                          int screenHeight, boolean editing) {
        if (!editing) {
            return Optional.empty();
        }
        for (ToolbarAction action : ToolbarAction.values()) {
            if (toolbarActionBounds(screenWidth, screenHeight, action).contains(mouseX, mouseY)) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
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
        renderButton(graphics, screenWidth, screenHeight, editing, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public static void renderButton(GuiGraphics graphics, int screenWidth, int screenHeight, boolean editing,
                                    int mouseX, int mouseY) {
        Rect bounds = editButtonBounds(screenWidth, screenHeight);
        drawIconButton(graphics, bounds, editing);
        drawSpriteIcon(graphics, bounds, editing ? EXIT_EDIT_ICON_INDEX : EDIT_ICON_INDEX);
        if (bounds.contains(mouseX, mouseY)) {
            graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.translatable(editing
                            ? "tooltip.xaeroregionsrev.exit_edit_region"
                            : "tooltip.xaeroregionsrev.edit_region"), mouseX, mouseY);
        }
    }

    public static void renderToolbar(GuiGraphics graphics, int screenWidth, int screenHeight, boolean editing,
                                     int mouseX, int mouseY) {
        if (!editing) {
            return;
        }
        for (ToolbarAction action : ToolbarAction.values()) {
            Rect bounds = toolbarActionBounds(screenWidth, screenHeight, action);
            drawIconButton(graphics, bounds, false);
            drawSpriteIcon(graphics, bounds, action.iconIndex());
            if (bounds.contains(mouseX, mouseY)) {
                graphics.renderTooltip(Minecraft.getInstance().font,
                        Component.translatable(action.tooltipKey()), mouseX, mouseY);
            }
        }
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
        drawLine(graphics, from, to, color, 1);
    }

    public static void drawLine(GuiGraphics graphics, Vector2f from, Vector2f to, int color, int thickness) {
        int steps = Math.max(1, Math.round(Math.max(Math.abs(to.x() - from.x()), Math.abs(to.y() - from.y()))));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(from.x() + (to.x() - from.x()) * t);
            int y = Math.round(from.y() + (to.y() - from.y()) * t);
            drawCenteredSquare(graphics, x, y, Math.max(1, thickness), color);
        }
    }

    public static void drawFilledCircle(GuiGraphics graphics, Vector2f center, int radius, int color) {
        int centerX = Math.round(center.x());
        int centerY = Math.round(center.y());
        int safeRadius = Math.max(1, radius);
        int radiusSquared = safeRadius * safeRadius;
        for (int y = -safeRadius; y <= safeRadius; y++) {
            for (int x = -safeRadius; x <= safeRadius; x++) {
                if (x * x + y * y <= radiusSquared) {
                    graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }

    private static void drawCenteredSquare(GuiGraphics graphics, int centerX, int centerY, int size, int color) {
        int before = size / 2;
        int after = size - before;
        graphics.fill(centerX - before, centerY - before, centerX + after, centerY + after, color);
    }

    private static void drawFilledPolygon(GuiGraphics graphics, List<Vector2f> points, int color) {
        PolygonFillRenderer.fill(graphics, points, color);
    }

    private static void drawIconButton(GuiGraphics graphics, Rect bounds, boolean active) {
        int fill = active ? 0xCC2F855A : 0xCC1F2937;
        int border = active ? 0xFFE2B93B : 0xFF9CA3AF;
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xAA000000);
        graphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.x() + bounds.width() - 1,
                bounds.y() + bounds.height() - 1, fill);
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + 1, border);
        graphics.fill(bounds.x(), bounds.y() + bounds.height() - 1, bounds.x() + bounds.width(),
                bounds.y() + bounds.height(), 0xFF111827);
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + 1, bounds.y() + bounds.height(), border);
        graphics.fill(bounds.x() + bounds.width() - 1, bounds.y(), bounds.x() + bounds.width(),
                bounds.y() + bounds.height(), 0xFF111827);
    }

    private static void drawSpriteIcon(GuiGraphics graphics, Rect bounds, int iconIndex) {
        graphics.blit(ICONS_TEXTURE, bounds.x() + ICON_PADDING, bounds.y() + ICON_PADDING,
                iconIndex * ICON_SIZE, 0, ICON_SIZE, ICON_SIZE, ICON_TEXTURE_WIDTH, ICON_TEXTURE_HEIGHT);
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
