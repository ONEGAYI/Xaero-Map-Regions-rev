package com.suian.xaeroregionsrev.client.xaero;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XaeroScreenDetectorTest {
    @Test
    void detectsXaeroGuiMapClassName() {
        assertTrue(XaeroScreenDetector.isWorldMapScreenClassName("xaero.map.gui.GuiMap"));
    }

    @Test
    void rejectsNonXaeroMapClassName() {
        assertFalse(XaeroScreenDetector.isWorldMapScreenClassName("net.minecraft.client.gui.screens.TitleScreen"));
    }
}
