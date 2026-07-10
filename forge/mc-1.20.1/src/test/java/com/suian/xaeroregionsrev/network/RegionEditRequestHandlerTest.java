package com.suian.xaeroregionsrev.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionEditRequestHandlerTest {
    @Test
    void editErrorsUseActionBarOverlay() {
        assertTrue(RegionEditRequestHandler.editErrorsUseActionBar());
    }

    @Test
    void refreshRequestsAreRateLimitedPerPlayer() {
        UUID player = UUID.randomUUID();

        assertTrue(RegionEditRequestHandler.canRefreshNow(player, 1_000_000_000L));
        assertFalse(RegionEditRequestHandler.canRefreshNow(player, 2_000_000_000L));
        assertTrue(RegionEditRequestHandler.canRefreshNow(player, 3_000_000_000L));
    }
}
