package com.suian.xaeroregionsrev.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XaeroRegionsClientTest {
    @Test
    void keepsSessionWhenOpeningOwnChildScreenFromWorldMap() {
        assertFalse(XaeroRegionsClient.shouldResetSessionOnScreenChange(true, false, true));
    }

    @Test
    void resetsSessionWhenLeavingWorldMapToUnrelatedScreenOrClosing() {
        assertTrue(XaeroRegionsClient.shouldResetSessionOnScreenChange(true, false, false));
        assertFalse(XaeroRegionsClient.shouldResetSessionOnScreenChange(false, false, false));
    }
}
