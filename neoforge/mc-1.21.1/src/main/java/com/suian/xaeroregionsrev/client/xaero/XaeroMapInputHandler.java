package com.suian.xaeroregionsrev.client.xaero;

import com.mojang.blaze3d.platform.InputConstants;
import com.suian.xaeroregionsrev.client.RegionKeyMappings;
import com.suian.xaeroregionsrev.client.editor.RegionEditorOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class XaeroMapInputHandler {
    private XaeroMapInputHandler() {
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!XaeroScreenDetector.isWorldMapScreen(screen)) {
            return;
        }
        if (screen.getFocused() instanceof EditBox) {
            return;
        }
        InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        XaeroMapInputRouter.KeyAction action = resolveKeyAction(
                RegionKeyMappings.TOGGLE_EDIT_MODE.get().isActiveAndMatches(key),
                RegionKeyMappings.OPEN_REGION_MANAGER.get().isActiveAndMatches(key),
                RegionKeyMappings.UNDO_DRAFT_POINT.get().isActiveAndMatches(key),
                RegionKeyMappings.REDO_DRAFT_POINT.get().isActiveAndMatches(key),
                RegionKeyMappings.SUBMIT_DRAFT.get().isActiveAndMatches(key),
                RegionKeyMappings.CLEAR_OR_EXIT_EDIT_MODE.get().isActiveAndMatches(key),
                event.getKeyCode()
        );
        XaeroMapInputRouter.Result result = XaeroMapInputRouter.Result.IGNORED;
        if (action != null) {
            result = XaeroMapOverlayController.handleKey(action, screen);
        }
        if (result != XaeroMapInputRouter.Result.IGNORED) {
            event.setCanceled(true);
        }
    }

    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!XaeroScreenDetector.isWorldMapScreen(screen)) {
            return;
        }
        if (screen.getFocused() instanceof EditBox) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        String dimension = minecraft.level.dimension().location().toString();
        boolean handled = XaeroMapOverlayController.handleMouse(
                screen,
                toMouseButton(event.getButton()),
                event.getMouseX(),
                event.getMouseY(),
                dimension
        );
        if (handled) {
            event.setCanceled(true);
        }
    }

    static XaeroMapInputRouter.KeyAction resolveKeyAction(
            boolean toggleEditModeMatches,
            boolean openManagerMatches,
            boolean undoDraftPointMatches,
            boolean redoDraftPointMatches,
            boolean submitDraftMatches,
            boolean clearOrExitMatches,
            int keyCode
    ) {
        if (undoDraftPointMatches) {
            return XaeroMapInputRouter.KeyAction.UNDO_DRAFT_POINT;
        }
        if (redoDraftPointMatches) {
            return XaeroMapInputRouter.KeyAction.REDO_DRAFT_POINT;
        }
        if (submitDraftMatches) {
            return XaeroMapInputRouter.KeyAction.SUBMIT_DRAFT;
        }
        if (clearOrExitMatches) {
            return XaeroMapInputRouter.KeyAction.ESCAPE;
        }
        if (toggleEditModeMatches) {
            return XaeroMapInputRouter.KeyAction.TOGGLE_EDIT_MODE;
        }
        if (openManagerMatches) {
            return XaeroMapInputRouter.KeyAction.OPEN_REGION_MANAGER;
        }
        return null;
    }

    private static RegionEditorOverlay.MouseButton toMouseButton(int button) {
        return switch (button) {
            case 0 -> RegionEditorOverlay.MouseButton.LEFT;
            case 1 -> RegionEditorOverlay.MouseButton.RIGHT;
            case 2 -> RegionEditorOverlay.MouseButton.MIDDLE;
            default -> RegionEditorOverlay.MouseButton.OTHER;
        };
    }
}
