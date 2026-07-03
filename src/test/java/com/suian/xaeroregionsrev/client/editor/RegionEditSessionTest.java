package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegionEditSessionTest {
    @Test
    void toggleEditModeEntersAndExitsCleanly() {
        RegionEditSession session = new RegionEditSession();

        session.toggleEditing();
        assertTrue(session.isEditing());

        session.addDraftPoint(new RegionPoint(1, 1));
        session.select(new RegionId("spawn"));
        session.toggleEditing();

        assertFalse(session.isEditing());
        assertTrue(session.draftPoints().isEmpty());
        assertTrue(session.selectedRegionId().isEmpty());
    }

    @Test
    void middleClickAddsPointsOnlyInEditMode() {
        RegionEditSession session = new RegionEditSession();

        assertFalse(session.addDraftPoint(new RegionPoint(1, 1)));
        assertTrue(session.draftPoints().isEmpty());

        session.toggleEditing();
        assertTrue(session.addDraftPoint(new RegionPoint(1, 1)));
        assertEquals(1, session.draftPoints().size());
    }

    @Test
    void draftPointsAreLimitedBeforeSubmit() {
        RegionEditSession session = new RegionEditSession();
        session.toggleEditing();

        for (int index = 0; index < 256; index++) {
            assertTrue(session.addDraftPoint(new RegionPoint(index, index)));
        }

        assertFalse(session.addDraftPoint(new RegionPoint(257, 257)));
        assertEquals(256, session.draftPoints().size());
    }

    @Test
    void threeOrMorePointsCanBeSubmitted() {
        RegionEditSession session = new RegionEditSession();
        session.toggleEditing();

        session.addDraftPoint(new RegionPoint(0, 0));
        session.addDraftPoint(new RegionPoint(10, 0));
        assertFalse(session.canSubmitDraft());

        session.addDraftPoint(new RegionPoint(10, 10));
        assertTrue(session.canSubmitDraft());
    }

    @Test
    void escapeClearsDraftBeforeExitingEditMode() {
        RegionEditSession session = new RegionEditSession();
        session.toggleEditing();
        session.addDraftPoint(new RegionPoint(0, 0));

        assertEquals(RegionEditSession.EscapeResult.CLEARED_DRAFT, session.handleEscape());
        assertTrue(session.isEditing());
        assertTrue(session.draftPoints().isEmpty());

        assertEquals(RegionEditSession.EscapeResult.EXITED_EDIT_MODE, session.handleEscape());
        assertFalse(session.isEditing());
    }

    @Test
    void resetClearsEditingDraftAndSelection() {
        RegionEditSession session = new RegionEditSession();
        session.toggleEditing();
        session.addDraftPoint(new RegionPoint(0, 0));
        session.select(new RegionId("spawn"));

        session.reset();

        assertFalse(session.isEditing());
        assertTrue(session.draftPoints().isEmpty());
        assertTrue(session.selectedRegionId().isEmpty());
    }
}
