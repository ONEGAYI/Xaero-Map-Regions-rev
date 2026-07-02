package com.suian.xaeroregionsrev.client.xaero;

import net.minecraft.client.gui.screens.Screen;

import java.util.Locale;

public final class XaeroScreenDetector {
    private XaeroScreenDetector() {
    }

    public static boolean isWorldMapScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        String className = screen.getClass().getName().toLowerCase(Locale.ROOT);
        return className.contains("xaero") && className.contains("world") && className.contains("map");
    }
}
