package com.suian.xaeroregionsrev.client.xaero;

import com.mojang.blaze3d.platform.InputConstants;
import com.suian.xaeroregionsrev.client.RegionKeyMappings;
import com.suian.xaeroregionsrev.client.editor.RegionEditorOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

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
        XaeroMapInputRouter.Result result = XaeroMapInputRouter.Result.IGNORED;
        if (RegionKeyMappings.TOGGLE_EDIT_MODE.get().isActiveAndMatches(key)) {
            result = XaeroMapOverlayController.handleKey(XaeroMapInputRouter.KeyAction.TOGGLE_EDIT_MODE, screen);
        } else if (RegionKeyMappings.OPEN_REGION_MANAGER.get().isActiveAndMatches(key)) {
            result = XaeroMapOverlayController.handleKey(XaeroMapInputRouter.KeyAction.OPEN_REGION_MANAGER, screen);
        } else if (event.getKeyCode() == GLFW.GLFW_KEY_ENTER || event.getKeyCode() == GLFW.GLFW_KEY_KP_ENTER) {
            result = XaeroMapOverlayController.handleKey(XaeroMapInputRouter.KeyAction.SUBMIT_DRAFT, screen);
        } else if (event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE) {
            result = XaeroMapOverlayController.handleKey(XaeroMapInputRouter.KeyAction.ESCAPE, screen);
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

    private static RegionEditorOverlay.MouseButton toMouseButton(int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> RegionEditorOverlay.MouseButton.LEFT;
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> RegionEditorOverlay.MouseButton.RIGHT;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> RegionEditorOverlay.MouseButton.MIDDLE;
            default -> RegionEditorOverlay.MouseButton.OTHER;
        };
    }
}
