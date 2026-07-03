package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.client.editor.RegionEditorOverlay;

import java.util.List;

public final class RegionLabelDisplay {
    private static final double INLINE_AREA_RATIO_THRESHOLD = 0.015D;
    private static final double INLINE_ABSOLUTE_AREA_THRESHOLD = 8_000.0D;

    private RegionLabelDisplay() {
    }

    public static boolean shouldRenderInlineLabel(List<RegionEditorOverlay.ScreenPoint> points,
                                                  int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return false;
        }
        double screenArea = (double) screenWidth * screenHeight;
        double projectedArea = polygonArea(points);
        return projectedArea >= INLINE_ABSOLUTE_AREA_THRESHOLD
                || projectedArea / screenArea >= INLINE_AREA_RATIO_THRESHOLD;
    }

    public static boolean isHovered(List<RegionEditorOverlay.ScreenPoint> points, double mouseX, double mouseY) {
        if (points.size() < 3) {
            return false;
        }
        boolean inside = false;
        for (int current = 0, previous = points.size() - 1; current < points.size(); previous = current++) {
            RegionEditorOverlay.ScreenPoint a = points.get(current);
            RegionEditorOverlay.ScreenPoint b = points.get(previous);
            boolean intersects = (a.y() > mouseY) != (b.y() > mouseY)
                    && mouseX < (b.x() - a.x()) * (mouseY - a.y()) / (b.y() - a.y()) + a.x();
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    public static String truncate(String label, int maxVisibleCharacters) {
        if (label == null || label.length() <= maxVisibleCharacters) {
            return label;
        }
        if (maxVisibleCharacters <= 0) {
            return "...";
        }
        return label.substring(0, maxVisibleCharacters).stripTrailing() + "...";
    }

    private static double polygonArea(List<RegionEditorOverlay.ScreenPoint> points) {
        if (points.size() < 3) {
            return 0.0D;
        }
        double doubleArea = 0.0D;
        for (int index = 0; index < points.size(); index++) {
            RegionEditorOverlay.ScreenPoint current = points.get(index);
            RegionEditorOverlay.ScreenPoint next = points.get((index + 1) % points.size());
            doubleArea += current.x() * next.y() - next.x() * current.y();
        }
        return Math.abs(doubleArea) / 2.0D;
    }
}
