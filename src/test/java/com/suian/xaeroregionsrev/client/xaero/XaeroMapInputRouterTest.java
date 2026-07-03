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
    void editModeActionsAreIgnoredBeforeEnteringEditMode() {
        RegionEditSession session = new RegionEditSession();
        XaeroMapInputRouter router = new XaeroMapInputRouter(session);

        assertEquals(XaeroMapInputRouter.Result.IGNORED, router.handleKey(XaeroMapInputRouter.KeyAction.UNDO_DRAFT_POINT));
        assertEquals(XaeroMapInputRouter.Result.IGNORED, router.handleKey(XaeroMapInputRouter.KeyAction.REDO_DRAFT_POINT));
        assertEquals(XaeroMapInputRouter.Result.IGNORED, router.handleKey(XaeroMapInputRouter.KeyAction.SUBMIT_DRAFT));
        assertEquals(XaeroMapInputRouter.Result.IGNORED, router.handleKey(XaeroMapInputRouter.KeyAction.CLEAR_DRAFT));
        assertEquals(XaeroMapInputRouter.Result.IGNORED, router.handleKey(XaeroMapInputRouter.KeyAction.ESCAPE));
        assertEquals(RegionEditorOverlay.Action.IGNORED, router.handleMouse(
                RegionEditorOverlay.MouseButton.MIDDLE,
                100,
                100,
                new RegionPoint(1, 1),
                List.of(),
                "minecraft:overworld"
        ));
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

    @Test
    void undoRedoAndClearDraftActionsOperateOnVerticesWithoutExitingEditMode() {
        RegionEditSession session = new RegionEditSession();
        XaeroMapInputRouter router = new XaeroMapInputRouter(session);
        router.handleKey(XaeroMapInputRouter.KeyAction.TOGGLE_EDIT_MODE);
        session.addDraftPoint(new RegionPoint(0, 0));
        session.addDraftPoint(new RegionPoint(10, 0));

        assertEquals(XaeroMapInputRouter.Result.CONSUMED, router.handleKey(XaeroMapInputRouter.KeyAction.UNDO_DRAFT_POINT));
        assertEquals(1, session.draftPoints().size());
        assertTrue(session.isEditing());

        assertEquals(XaeroMapInputRouter.Result.CONSUMED, router.handleKey(XaeroMapInputRouter.KeyAction.REDO_DRAFT_POINT));
        assertEquals(2, session.draftPoints().size());

        assertEquals(XaeroMapInputRouter.Result.CONSUMED, router.handleKey(XaeroMapInputRouter.KeyAction.CLEAR_DRAFT));
        assertTrue(session.draftPoints().isEmpty());
        assertTrue(session.isEditing());
    }
}
