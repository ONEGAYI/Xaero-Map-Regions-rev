package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.client.editor.RegionEditSession;
import com.suian.xaeroregionsrev.client.editor.RegionEditorOverlay;
import com.suian.xaeroregionsrev.region.RegionPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XaeroMapInputRouterTest {
    @Test
    void toggleAndOpenManagerActionsAreMapScopedByCaller() {
        RegionEditSession session = new RegionEditSession();
        XaeroMapInputRouter router = new XaeroMapInputRouter(session);

        assertEquals(XaeroMapInputRouter.Result.CONSUMED, router.handleKey(XaeroMapInputRouter.KeyAction.TOGGLE_EDIT_MODE));
        assertTrue(session.isEditing());

        assertEquals(XaeroMapInputRouter.Result.OPEN_MANAGER, router.handleKey(XaeroMapInputRouter.KeyAction.OPEN_REGION_MANAGER));
    }

    @Test
    void enterAndMouseActionsDelegateToOverlayRouter() {
        RegionEditSession session = new RegionEditSession();
        XaeroMapInputRouter router = new XaeroMapInputRouter(session);
        router.handleKey(XaeroMapInputRouter.KeyAction.TOGGLE_EDIT_MODE);

        assertEquals(RegionEditorOverlay.Action.ADDED_DRAFT_POINT, router.handleMouse(
                RegionEditorOverlay.MouseButton.MIDDLE,
                100,
                100,
                new RegionPoint(1, 1),
                List.of(),
                "minecraft:overworld"
        ));
        assertEquals(XaeroMapInputRouter.Result.IGNORED, router.handleKey(XaeroMapInputRouter.KeyAction.SUBMIT_DRAFT));

        session.addDraftPoint(new RegionPoint(10, 1));
        session.addDraftPoint(new RegionPoint(10, 10));
        assertEquals(XaeroMapInputRouter.Result.OPEN_CREATE_FORM, router.handleKey(XaeroMapInputRouter.KeyAction.SUBMIT_DRAFT));
    }
}
