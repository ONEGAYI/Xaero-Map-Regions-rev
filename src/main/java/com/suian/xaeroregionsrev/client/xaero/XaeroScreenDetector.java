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
        return isWorldMapScreenClassName(screen.getClass().getName());
    }

    static boolean isWorldMapScreenClassName(String className) {
        if (className == null) {
            return false;
        }
        String normalizedClassName = className.toLowerCase(Locale.ROOT);
        return normalizedClassName.equals("xaero.map.gui.guimap")
                || normalizedClassName.contains("xaero") && normalizedClassName.contains("world") && normalizedClassName.contains("map");
    }
}
