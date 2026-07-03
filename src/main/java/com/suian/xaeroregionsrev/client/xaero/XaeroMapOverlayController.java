package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.client.ClientRegionCache;
import com.suian.xaeroregionsrev.client.editor.RegionContextMenu;
import com.suian.xaeroregionsrev.client.editor.RegionEditSession;
import com.suian.xaeroregionsrev.client.editor.RegionEditorOverlay;
import com.suian.xaeroregionsrev.client.editor.RegionManagerScreen;
import com.suian.xaeroregionsrev.client.editor.RegionStyleEditScreen;
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
    private static final MapProjectionAdapter PROJECTION = new MapProjectionAdapter();
    private static RegionContextMenu contextMenu;

    private XaeroMapOverlayController() {
    }

    public static RegionEditSession session() {
        return SESSION;
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

    public static void render(GuiGraphics graphics, Screen screen, List<Region> regions, String dimension) {
        for (Region region : regions) {
            if (region.points().size() < 3 || !region.dimension().equals(dimension)) {
                continue;
            }
            List<Vector2f> projected = project(region.points(), screen);
            renderSelectedOutline(graphics, region, projected);
            renderLabel(graphics, screen, region, projected);
        }
        if (SESSION.isEditing()) {
            RegionEditorOverlay.renderDraft(graphics, project(SESSION.draftPoints(), screen));
        }
        RegionEditorOverlay.renderButton(graphics, screen.width, screen.height, SESSION.isEditing());
        if (contextMenu != null) {
            contextMenu.render(graphics);
        }
    }

    private static void renderSelectedOutline(GuiGraphics graphics, Region region, List<Vector2f> projected) {
        Optional<RegionId> selected = SESSION.selectedRegionId();
        if (selected.isEmpty() || !selected.get().equals(region.id())) {
            return;
        }
        for (int i = 0; i < projected.size(); i++) {
            Vector2f from = projected.get(i);
            Vector2f to = projected.get((i + 1) % projected.size());
            RegionEditorOverlay.drawLine(graphics, from, to, 0xFFFFFFFF);
        }
    }

    private static void renderLabel(GuiGraphics graphics, Screen screen, Region region, List<Vector2f> projected) {
        List<RegionEditorOverlay.ScreenPoint> points = projected.stream()
                .map(point -> new RegionEditorOverlay.ScreenPoint(point.x(), point.y()))
                .toList();
        RegionEditorOverlay.ScreenPoint anchor = RegionEditorOverlay.labelAnchor(points);
        if (anchor.x() < 0 || anchor.y() < 0 || anchor.x() > screen.width || anchor.y() > screen.height) {
            return;
        }
        int labelWidth = Minecraft.getInstance().font.width(region.label());
        graphics.drawString(
                Minecraft.getInstance().font,
                region.label(),
                Math.round(anchor.x() - labelWidth / 2.0F),
                Math.round(anchor.y() - 4.0F),
                region.labelColor().value(),
                true
        );
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
                    RegionNetwork.CHANNEL.sendToServer(new DeleteRegionRequestPacket(regionId));
                    SESSION.clearSelection();
                }
                Minecraft.getInstance().setScreen(previous);
            }, Component.translatable("screen.xaeroregionsrev.confirm_delete"),
                    Component.literal(region.label())));
            return;
        }
        Minecraft.getInstance().setScreen(RegionStyleEditScreen.edit(previous, region));
    }
}
