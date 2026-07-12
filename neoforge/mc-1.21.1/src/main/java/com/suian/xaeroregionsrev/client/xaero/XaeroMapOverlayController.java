package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.client.ClientRegionCache;
import com.suian.xaeroregionsrev.client.editor.RegionContextMenu;
import com.suian.xaeroregionsrev.client.editor.RegionEditSession;
import com.suian.xaeroregionsrev.client.editor.RegionEditorOverlay;
import com.suian.xaeroregionsrev.client.editor.RegionManagerScreen;
import com.suian.xaeroregionsrev.client.editor.RegionStyleEditScreen;
import com.suian.xaeroregionsrev.client.editor.SelectionHudText;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.DeleteRegionRequestPacket;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class XaeroMapOverlayController {
    private static final RegionEditSession SESSION = new RegionEditSession();
    private static final XaeroMapInputRouter ROUTER = new XaeroMapInputRouter(SESSION);
    private static final MapProjectionAdapter PROJECTION = MapProjectionAdapter.shared();
    private static RegionContextMenu contextMenu;

    private XaeroMapOverlayController() {
    }

    public static RegionEditSession session() {
        return SESSION;
    }

    public static void reset() {
        SESSION.reset();
        contextMenu = null;
    }

    public static XaeroMapInputRouter.Result handleKey(XaeroMapInputRouter.KeyAction action, Screen screen) {
        XaeroMapInputRouter.Result result = ROUTER.handleKey(action);
        if (result == XaeroMapInputRouter.Result.OPEN_MANAGER) {
            openManager(screen);
        } else if (result == XaeroMapInputRouter.Result.OPEN_CREATE_FORM) {
            openCreateForm(screen);
        }
        return result;
    }

    public static boolean handleMouse(Screen screen, RegionEditorOverlay.MouseButton button,
                                      double mouseX, double mouseY, String dimension) {
        if (contextMenu != null && button == RegionEditorOverlay.MouseButton.LEFT) {
            RegionContextMenu.Command command = contextMenu.commandAt(mouseX, mouseY);
            if (command != null) {
                handleContextCommand(screen, contextMenu.regionId(), command);
                contextMenu = null;
                return true;
            }
            contextMenu = null;
            return true;
        }

        if (button == RegionEditorOverlay.MouseButton.LEFT) {
            if (RegionEditorOverlay.editButtonBounds(screen.width, screen.height).contains(mouseX, mouseY)) {
                handleKey(XaeroMapInputRouter.KeyAction.TOGGLE_EDIT_MODE, screen);
                return true;
            }
            Optional<RegionEditorOverlay.ToolbarAction> toolbarAction = RegionEditorOverlay.toolbarActionAt(
                    mouseX, mouseY, screen.width, screen.height, SESSION.isEditing());
            if (toolbarAction.isPresent()) {
                handleToolbarAction(toolbarAction.get(), screen);
                return true;
            }
        }

        RegionPoint worldPoint = PROJECTION.unproject(screen, mouseX, mouseY);
        RegionEditorOverlay.Action action = ROUTER.handleMouse(
                button,
                mouseX,
                mouseY,
                screen.width,
                screen.height,
                worldPoint,
                ClientRegionCache.regions(),
                dimension
        );
        if (action == RegionEditorOverlay.Action.OPEN_CONTEXT_MENU) {
            SESSION.selectedRegionId().ifPresent(id -> contextMenu = new RegionContextMenu(id, (int) mouseX, (int) mouseY));
            return true;
        }
        return action != RegionEditorOverlay.Action.IGNORED;
    }

    public static void renderRegionDecorations(GuiGraphics graphics, Region region, List<Vector2f> projected) {
        renderBoundary(graphics, region, projected);
        renderSelectedOutline(graphics, region, projected);
    }

    public static Optional<RegionLabelCollisionLayout.Candidate> createLabelCandidate(
            Screen screen, Region region, List<Vector2f> projected) {
        List<RegionEditorOverlay.ScreenPoint> points = projected.stream()
                .map(point -> new RegionEditorOverlay.ScreenPoint(point.x(), point.y()))
                .toList();
        var font = Minecraft.getInstance().font;
        Optional<RegionLabelDisplay.InlineLabel> inlineLabel = RegionLabelDisplay.layoutInlineLabel(
                region.label(), points, font.lineHeight, font::width);
        if (inlineLabel.isEmpty()) {
            return Optional.empty();
        }
        RegionLabelDisplay.InlineLabel label = inlineLabel.get();
        int labelWidth = font.width(label.text());
        if (label.x() + labelWidth < 0 || label.y() + font.lineHeight < 0
                || label.x() > screen.width || label.y() > screen.height) {
            return Optional.empty();
        }
        return Optional.of(new RegionLabelCollisionLayout.Candidate(
                region.id(), label.text(), region.labelColor().value(),
                label.x(), label.y(), labelWidth, font.lineHeight));
    }

    public static void renderInlineLabel(GuiGraphics graphics, RegionLabelCollisionLayout.Candidate label) {
        graphics.drawString(Minecraft.getInstance().font, label.text(), label.x(), label.y(), label.textArgb(), true);
    }

    public static boolean isHovered(List<Vector2f> projected, int mouseX, int mouseY) {
        return RegionLabelDisplay.isHovered(projected.stream()
                .map(point -> new RegionEditorOverlay.ScreenPoint(point.x(), point.y()))
                .toList(), mouseX, mouseY);
    }

    private static void renderBoundary(GuiGraphics graphics, Region region, List<Vector2f> projected) {
        RegionRenderStyle.Decoration decoration = RegionRenderStyle.decorationForProjectedBounds(
                projectedWidth(projected), projectedHeight(projected));
        if (!decoration.visible()) {
            return;
        }
        int boundaryColor = RegionRenderStyle.boundaryColor(region.color().value());
        for (int i = 0; i < projected.size(); i++) {
            Vector2f from = projected.get(i);
            Vector2f to = projected.get((i + 1) % projected.size());
            RegionEditorOverlay.drawLine(graphics, from, to, boundaryColor, decoration.boundaryThickness());
        }
        for (Vector2f point : projected) {
            RegionEditorOverlay.drawFilledCircle(graphics, point, decoration.vertexRadius(), boundaryColor);
        }
    }

    public static void renderEditor(GuiGraphics graphics, Screen screen, int mouseX, int mouseY, String currentDimension) {
        if (SESSION.isEditing()) {
            RegionEditorOverlay.renderDraft(graphics, project(SESSION.draftPoints(), screen));
        }
        RegionEditorOverlay.renderToolbar(graphics, screen.width, screen.height, SESSION.isEditing(), mouseX, mouseY);
        RegionEditorOverlay.renderButton(graphics, screen.width, screen.height, SESSION.isEditing(), mouseX, mouseY);
        renderSelectionHud(graphics, screen, mouseX, mouseY, currentDimension);
        if (contextMenu != null) {
            contextMenu.render(graphics);
        }
    }

    private static void renderSelectionHud(GuiGraphics graphics, Screen screen, int mouseX, int mouseY, String currentDimension) {
        Optional<RegionEditSession.SelectionInfo> info = SESSION.selectionInfo();
        if (info.isEmpty()) {
            return;
        }
        String label = ClientRegionCache.regions().stream()
                .filter(r -> r.id().equals(info.get().id()) && r.dimension().equals(currentDimension))
                .map(Region::label)
                .findFirst()
                .orElse(info.get().id().value());

        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        SelectionHudText text = SelectionHudText.of(
                label, info.get().index(), info.get().total(), font::width, 160);

        int textWidth = font.width(text.displayText());
        int padding = 6;
        int hudWidth = textWidth + padding * 2;
        int hudHeight = font.lineHeight + 4 * 2;
        int rightEdge = screen.width - RegionEditorOverlay.EDIT_BUTTON_MARGIN;
        int hudX = rightEdge - hudWidth;
        int hudY = RegionEditorOverlay.EDIT_BUTTON_MARGIN + RegionEditorOverlay.EDIT_BUTTON_HEIGHT + 6;

        graphics.fill(hudX, hudY, hudX + hudWidth, hudY + hudHeight, 0xAA111111);
        graphics.drawString(font, text.displayText(), hudX + padding, hudY + 4, 0xFFFFFFFF, false);

        RegionEditorOverlay.Rect hudBounds = new RegionEditorOverlay.Rect(hudX, hudY, hudWidth, hudHeight);
        if (text.truncated() && hudBounds.contains(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal(text.fullText()), mouseX, mouseY);
        }
    }

    private static void renderSelectedOutline(GuiGraphics graphics, Region region, List<Vector2f> projected) {
        Optional<RegionId> selected = SESSION.selectedRegionId();
        if (selected.isEmpty() || !selected.get().equals(region.id())) {
            return;
        }
        RegionRenderStyle.Decoration decoration = RegionRenderStyle.decorationForProjectedBounds(
                projectedWidth(projected), projectedHeight(projected));
        if (!decoration.visible()) {
            return;
        }
        for (int i = 0; i < projected.size(); i++) {
            Vector2f from = projected.get(i);
            Vector2f to = projected.get((i + 1) % projected.size());
            RegionEditorOverlay.drawLine(graphics, from, to, 0xFFFFFFFF, decoration.boundaryThickness());
        }
    }

    private static float projectedWidth(List<Vector2f> projected) {
        if (projected.isEmpty()) {
            return 0.0F;
        }
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (Vector2f point : projected) {
            minX = Math.min(minX, point.x());
            maxX = Math.max(maxX, point.x());
        }
        return maxX - minX;
    }

    private static float projectedHeight(List<Vector2f> projected) {
        if (projected.isEmpty()) {
            return 0.0F;
        }
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (Vector2f point : projected) {
            minY = Math.min(minY, point.y());
            maxY = Math.max(maxY, point.y());
        }
        return maxY - minY;
    }

    public static boolean isProjectedRegionVisible(List<Vector2f> projected, int screenWidth, int screenHeight) {
        return RegionEditorOverlay.isProjectedBoundsVisible(projected.stream()
                .map(point -> new RegionEditorOverlay.ScreenPoint(point.x(), point.y()))
                .toList(), screenWidth, screenHeight);
    }

    private static List<Vector2f> project(List<RegionPoint> points, Screen screen) {
        List<Vector2f> projected = new ArrayList<>(points.size());
        for (RegionPoint point : points) {
            projected.add(PROJECTION.project(screen, point));
        }
        return projected;
    }

    private static void openManager(Screen previous) {
        Minecraft.getInstance().setScreen(new RegionManagerScreen(previous));
    }

    private static void openCreateForm(Screen previous) {
        Minecraft.getInstance().setScreen(RegionStyleEditScreen.create(previous, SESSION.draftPoints(), SESSION::clearDraft));
    }

    private static void handleToolbarAction(RegionEditorOverlay.ToolbarAction action, Screen screen) {
        switch (action) {
            case ADD_DRAFT_POINT_HINT -> {
            }
            case OPEN_REGION_MANAGER -> handleKey(XaeroMapInputRouter.KeyAction.OPEN_REGION_MANAGER, screen);
            case UNDO_DRAFT_POINT -> handleKey(XaeroMapInputRouter.KeyAction.UNDO_DRAFT_POINT, screen);
            case REDO_DRAFT_POINT -> handleKey(XaeroMapInputRouter.KeyAction.REDO_DRAFT_POINT, screen);
            case SUBMIT_DRAFT -> handleKey(XaeroMapInputRouter.KeyAction.SUBMIT_DRAFT, screen);
            case CLEAR_DRAFT -> handleKey(XaeroMapInputRouter.KeyAction.CLEAR_DRAFT, screen);
        }
    }

    private static void handleContextCommand(Screen previous, RegionId regionId, RegionContextMenu.Command command) {
        Region region = ClientRegionCache.regions().stream()
                .filter(candidate -> candidate.id().equals(regionId))
                .findFirst()
                .orElse(null);
        if (region == null) {
            return;
        }
        if (command == RegionContextMenu.Command.DELETE) {
            Minecraft.getInstance().setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) {
                    RegionNetwork.sendToServer(new DeleteRegionRequestPacket(regionId));
                    SESSION.clearSelection();
                }
                Minecraft.getInstance().setScreen(previous);
            }, Component.translatable("screen.xaeroregionsrev.confirm_delete"),
                    Component.literal(region.label())));
            return;
        }
        Minecraft.getInstance().setScreen(RegionStyleEditScreen.edit(previous, region, command));
    }
}
