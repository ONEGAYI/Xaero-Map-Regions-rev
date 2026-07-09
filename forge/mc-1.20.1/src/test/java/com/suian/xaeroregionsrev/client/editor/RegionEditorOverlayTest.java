package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.RegionPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegionEditorOverlayTest {
    @Test
    void labelAnchorUsesPolygonBoundsCenter() {
        RegionEditorOverlay.ScreenPoint anchor = RegionEditorOverlay.labelAnchor(List.of(
                new RegionEditorOverlay.ScreenPoint(10.0F, 20.0F),
                new RegionEditorOverlay.ScreenPoint(30.0F, 20.0F),
                new RegionEditorOverlay.ScreenPoint(20.0F, 60.0F)
        ));

        assertEquals(20.0F, anchor.x());
        assertEquals(40.0F, anchor.y());
    }

    @Test
    void editButtonHitboxIsStableWhenTextChanges() {
        RegionEditorOverlay.Rect inactive = RegionEditorOverlay.editButtonBounds(800, 600);
        RegionEditorOverlay.Rect active = RegionEditorOverlay.editButtonBounds(800, 600);

        assertEquals(inactive, active);
        assertEquals(22, inactive.width());
        assertEquals(22, inactive.height());
        assertTrue(inactive.contains(inactive.x() + 1, inactive.y() + 1));
    }

    @Test
    void toolbarActionsAppearOnlyInEditingModeAndMapToStableHitboxes() {
        RegionEditorOverlay.Rect addPoint = RegionEditorOverlay.toolbarActionBounds(
                800, 600, RegionEditorOverlay.ToolbarAction.ADD_DRAFT_POINT_HINT);
        RegionEditorOverlay.Rect manager = RegionEditorOverlay.toolbarActionBounds(
                800, 600, RegionEditorOverlay.ToolbarAction.OPEN_REGION_MANAGER);
        RegionEditorOverlay.Rect undo = RegionEditorOverlay.toolbarActionBounds(
                800, 600, RegionEditorOverlay.ToolbarAction.UNDO_DRAFT_POINT);
        RegionEditorOverlay.Rect redo = RegionEditorOverlay.toolbarActionBounds(
                800, 600, RegionEditorOverlay.ToolbarAction.REDO_DRAFT_POINT);
        RegionEditorOverlay.Rect submit = RegionEditorOverlay.toolbarActionBounds(
                800, 600, RegionEditorOverlay.ToolbarAction.SUBMIT_DRAFT);
        RegionEditorOverlay.Rect clear = RegionEditorOverlay.toolbarActionBounds(
                800, 600, RegionEditorOverlay.ToolbarAction.CLEAR_DRAFT);

        assertEquals(22, addPoint.width());
        assertEquals(22, addPoint.height());
        assertTrue(addPoint.x() < manager.x());
        assertTrue(manager.x() < submit.x());
        assertTrue(manager.x() < undo.x());
        assertTrue(undo.x() < redo.x());
        assertTrue(redo.x() < submit.x());
        assertTrue(submit.x() < clear.x());
        assertTrue(clear.x() < RegionEditorOverlay.editButtonBounds(800, 600).x());

        assertEquals(RegionEditorOverlay.ToolbarAction.ADD_DRAFT_POINT_HINT,
                RegionEditorOverlay.toolbarActionAt(addPoint.x() + 1, addPoint.y() + 1, 800, 600, true).orElseThrow());
        assertEquals(RegionEditorOverlay.ToolbarAction.OPEN_REGION_MANAGER,
                RegionEditorOverlay.toolbarActionAt(manager.x() + 1, manager.y() + 1, 800, 600, true).orElseThrow());
        assertEquals(RegionEditorOverlay.ToolbarAction.UNDO_DRAFT_POINT,
                RegionEditorOverlay.toolbarActionAt(undo.x() + 1, undo.y() + 1, 800, 600, true).orElseThrow());
        assertEquals(RegionEditorOverlay.ToolbarAction.REDO_DRAFT_POINT,
                RegionEditorOverlay.toolbarActionAt(redo.x() + 1, redo.y() + 1, 800, 600, true).orElseThrow());
        assertEquals(RegionEditorOverlay.ToolbarAction.SUBMIT_DRAFT,
                RegionEditorOverlay.toolbarActionAt(submit.x() + 1, submit.y() + 1, 800, 600, true).orElseThrow());
        assertEquals(RegionEditorOverlay.ToolbarAction.CLEAR_DRAFT,
                RegionEditorOverlay.toolbarActionAt(clear.x() + 1, clear.y() + 1, 800, 600, true).orElseThrow());
        assertTrue(RegionEditorOverlay.toolbarActionAt(manager.x() + 1, manager.y() + 1, 800, 600, false).isEmpty());
    }

    @Test
    void draftPreviewFillStartsAtThreePoints() {
        assertFalse(RegionEditorOverlay.shouldRenderDraftFill(List.of(
                new RegionEditorOverlay.ScreenPoint(0, 0),
                new RegionEditorOverlay.ScreenPoint(10, 0)
        )));
        assertTrue(RegionEditorOverlay.shouldRenderDraftFill(List.of(
                new RegionEditorOverlay.ScreenPoint(0, 0),
                new RegionEditorOverlay.ScreenPoint(10, 0),
                new RegionEditorOverlay.ScreenPoint(10, 10)
        )));
    }

    @Test
    void projectedBoundsVisibilityUsesScreenIntersection() {
        assertTrue(RegionEditorOverlay.isProjectedBoundsVisible(List.of(
                new RegionEditorOverlay.ScreenPoint(-10, 10),
                new RegionEditorOverlay.ScreenPoint(10, 10),
                new RegionEditorOverlay.ScreenPoint(10, 20)
        ), 100, 100));
        assertFalse(RegionEditorOverlay.isProjectedBoundsVisible(List.of(
                new RegionEditorOverlay.ScreenPoint(-30, 10),
                new RegionEditorOverlay.ScreenPoint(-10, 10),
                new RegionEditorOverlay.ScreenPoint(-10, 20)
        ), 100, 100));
    }

    @Test
    void routerHandlesEditButtonMiddleClickAndEnter() {
        RegionEditSession session = new RegionEditSession();
        RegionEditorOverlay.ActionRouter router = new RegionEditorOverlay.ActionRouter(session);

        assertEquals(RegionEditorOverlay.Action.TOGGLED_EDITING,
                router.handleMouse(RegionEditorOverlay.MouseButton.LEFT, 780, 18, new RegionPoint(0, 0), List.of(), "minecraft:overworld"));
        assertTrue(session.isEditing());

        assertEquals(RegionEditorOverlay.Action.ADDED_DRAFT_POINT,
                router.handleMouse(RegionEditorOverlay.MouseButton.MIDDLE, 200, 200, new RegionPoint(0, 0), List.of(), "minecraft:overworld"));
        assertEquals(RegionEditorOverlay.Action.IGNORED, router.handleEnter());

        session.addDraftPoint(new RegionPoint(10, 0));
        session.addDraftPoint(new RegionPoint(10, 10));
        assertEquals(RegionEditorOverlay.Action.OPEN_CREATE_FORM, router.handleEnter());
    }

    @Test
    void contextMenuRoutesActionsToCommands() {
        assertEquals(RegionContextMenu.Command.DELETE, RegionContextMenu.commandFor(0));
        assertEquals(RegionContextMenu.Command.EDIT, RegionContextMenu.commandFor(1));
        assertThrows(IllegalArgumentException.class, () -> RegionContextMenu.commandFor(2));
    }
}
