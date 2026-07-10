package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.network.payload.RegionEditResultPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionSubmissionStateTest {
    @Test
    void submitDisablesButtonForTenSeconds() {
        RegionSubmissionState state = new RegionSubmissionState();

        state.submit(100L, 1_000L);

        assertTrue(state.isPending());
        assertFalse(state.canSubmit(10_999L));
        assertTrue(state.canSubmit(11_000L));
    }

    @Test
    void failedResultKeepsButtonDisabledUntilTenSecondWindowEnds() {
        RegionSubmissionState state = new RegionSubmissionState();
        state.submit(100L, 1_000L);

        RegionSubmissionState.ResultAction action = state.receive(
                new RegionEditResultPacket(100L, false, false, "No permission."), 1_500L);

        assertEquals(RegionSubmissionState.ResultAction.SHOW_FAILURE, action);
        assertFalse(state.canSubmit(10_999L));
        assertEquals(RegionSubmissionState.TickAction.RESTORE_AFTER_FAILURE, state.tick(11_000L));
        assertTrue(state.canSubmit(11_000L));
    }

    @Test
    void successfulResultClosesImmediately() {
        RegionSubmissionState state = new RegionSubmissionState();
        state.submit(100L, 1_000L);

        RegionSubmissionState.ResultAction action = state.receive(
                new RegionEditResultPacket(100L, true, true, "Saved."), 1_500L);

        assertEquals(RegionSubmissionState.ResultAction.CLOSE_SCREEN, action);
        assertFalse(state.isPending());
    }

    @Test
    void timeoutRestoresButtonAndIgnoresLateResult() {
        RegionSubmissionState state = new RegionSubmissionState();
        state.submit(100L, 1_000L);

        assertEquals(RegionSubmissionState.TickAction.TIMEOUT, state.tick(11_000L));
        assertTrue(state.canSubmit(11_000L));

        RegionSubmissionState.ResultAction action = state.receive(
                new RegionEditResultPacket(100L, true, true, "Saved."), 11_500L);

        assertEquals(RegionSubmissionState.ResultAction.IGNORED, action);
    }

    @Test
    void mismatchedResultIsIgnoredWhileAnotherSubmitIsPending() {
        RegionSubmissionState state = new RegionSubmissionState();
        state.submit(100L, 1_000L);

        RegionSubmissionState.ResultAction action = state.receive(
                new RegionEditResultPacket(99L, false, false, "Old failure."), 1_500L);

        assertEquals(RegionSubmissionState.ResultAction.IGNORED, action);
        assertTrue(state.isPending());
    }
}
