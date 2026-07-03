package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.client.editor.RegionEditSession;
import com.suian.xaeroregionsrev.client.editor.RegionEditorOverlay;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionPoint;

import java.util.List;

public final class XaeroMapInputRouter {
    private final RegionEditSession session;
    private final RegionEditorOverlay.ActionRouter overlayRouter;

    public enum KeyAction {
        TOGGLE_EDIT_MODE,
        OPEN_REGION_MANAGER,
        SUBMIT_DRAFT,
        ESCAPE
    }

    public enum Result {
        IGNORED,
        CONSUMED,
        OPEN_MANAGER,
        OPEN_CREATE_FORM
    }

    public XaeroMapInputRouter(RegionEditSession session) {
        this.session = session;
        this.overlayRouter = new RegionEditorOverlay.ActionRouter(session);
    }

    public Result handleKey(KeyAction action) {
        return switch (action) {
            case TOGGLE_EDIT_MODE -> {
                session.toggleEditing();
                yield Result.CONSUMED;
            }
            case OPEN_REGION_MANAGER -> Result.OPEN_MANAGER;
            case SUBMIT_DRAFT -> overlayRouter.handleEnter() == RegionEditorOverlay.Action.OPEN_CREATE_FORM
                    ? Result.OPEN_CREATE_FORM
                    : Result.IGNORED;
            case ESCAPE -> session.handleEscape() == RegionEditSession.EscapeResult.IGNORED
                    ? Result.IGNORED
                    : Result.CONSUMED;
        };
    }

    public RegionEditorOverlay.Action handleMouse(RegionEditorOverlay.MouseButton button, double mouseX, double mouseY,
                                                  RegionPoint worldPoint, List<Region> regions, String dimension) {
        return overlayRouter.handleMouse(button, mouseX, mouseY, worldPoint, regions, dimension);
    }

    public RegionEditorOverlay.Action handleMouse(RegionEditorOverlay.MouseButton button, double mouseX, double mouseY,
                                                  int screenWidth, int screenHeight, RegionPoint worldPoint,
                                                  List<Region> regions, String dimension) {
        return overlayRouter.handleMouse(button, mouseX, mouseY, screenWidth, screenHeight, worldPoint, regions, dimension);
    }
}
