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
        assertTrue(inactive.contains(inactive.x() + 1, inactive.y() + 1));
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
        assertEquals(RegionContextMenu.Command.EDIT_FILL_COLOR, RegionContextMenu.commandFor(1));
        assertEquals(RegionContextMenu.Command.EDIT_LABEL_TEXT, RegionContextMenu.commandFor(2));
        assertEquals(RegionContextMenu.Command.EDIT_LABEL_COLOR, RegionContextMenu.commandFor(3));
        assertThrows(IllegalArgumentException.class, () -> RegionContextMenu.commandFor(4));
    }
}
