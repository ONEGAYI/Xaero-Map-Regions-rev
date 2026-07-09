package com.suian.xaeroregionsrev.region;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionProfileTest {
    @Test
    void opCreativeCanManageRegions() {
        assertTrue(new PermissionProfile(true, true).canManageRegions());
    }

    @Test
    void opSurvivalCannotManageRegions() {
        assertFalse(new PermissionProfile(true, false).canManageRegions());
    }

    @Test
    void creativeNonOpCannotManageRegions() {
        assertFalse(new PermissionProfile(false, true).canManageRegions());
    }
}
