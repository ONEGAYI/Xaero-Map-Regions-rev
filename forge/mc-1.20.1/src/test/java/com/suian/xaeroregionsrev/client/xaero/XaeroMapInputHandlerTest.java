package com.suian.xaeroregionsrev.client.xaero;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XaeroMapInputHandlerTest {
    @Test
    void submitAndEscapeActionsWinWhenEditableMappingsConflict() {
        assertEquals(XaeroMapInputRouter.KeyAction.SUBMIT_DRAFT,
                XaeroMapInputHandler.resolveKeyAction(true, false, false, false, true, false, GLFW.GLFW_KEY_ENTER));
        assertEquals(XaeroMapInputRouter.KeyAction.ESCAPE,
                XaeroMapInputHandler.resolveKeyAction(true, false, false, false, false, true, GLFW.GLFW_KEY_ESCAPE));
        assertEquals(XaeroMapInputRouter.KeyAction.UNDO_DRAFT_POINT,
                XaeroMapInputHandler.resolveKeyAction(true, false, true, false, true, false, GLFW.GLFW_KEY_Z));
        assertEquals(XaeroMapInputRouter.KeyAction.REDO_DRAFT_POINT,
                XaeroMapInputHandler.resolveKeyAction(true, false, false, true, false, true, GLFW.GLFW_KEY_Y));
    }
}
