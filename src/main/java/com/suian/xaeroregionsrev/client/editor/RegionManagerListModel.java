package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionPoint;

import java.util.List;
import java.util.OptionalInt;

public final class RegionManagerListModel {
    private RegionManagerListModel() {
    }

    public static RegionPoint centerPoint(Region region) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (RegionPoint point : region.points()) {
            minX = Math.min(minX, point.x());
            minZ = Math.min(minZ, point.z());
            maxX = Math.max(maxX, point.x());
            maxZ = Math.max(maxZ, point.z());
        }
        if (region.points().isEmpty()) {
            return new RegionPoint(0, 0);
        }
        return new RegionPoint(Math.floorDiv(minX + maxX, 2), Math.floorDiv(minZ + maxZ, 2));
    }

    public static String formatCenter(Region region) {
        RegionPoint center = centerPoint(region);
        return "X: " + center.x() + " Z: " + center.z();
    }

    public static int pageCount(int itemCount, int pageSize) {
        if (itemCount <= 0) {
            return 1;
        }
        return Math.max(1, (itemCount + pageSize - 1) / pageSize);
    }

    public static int clampPage(int page, int itemCount, int pageSize) {
        return Math.max(0, Math.min(page, pageCount(itemCount, pageSize) - 1));
    }

    public static <T> List<T> pageItems(List<T> items, int page, int pageSize) {
        int clampedPage = clampPage(page, items.size(), pageSize);
        int from = Math.min(items.size(), clampedPage * pageSize);
        int to = Math.min(items.size(), from + pageSize);
        return items.subList(from, to);
    }

    public static OptionalInt rowAt(double mouseX, double mouseY, int left, int top, int width, int rowHeight,
                                    int visibleRows) {
        if (mouseX < left || mouseX >= left + width || mouseY < top) {
            return OptionalInt.empty();
        }
        int row = (int) ((mouseY - top) / rowHeight);
        if (row < 0 || row >= visibleRows) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(row);
    }

    public static int listWidth(int screenWidth, int maxWidth, int horizontalMargin) {
        return Math.max(0, Math.min(maxWidth, screenWidth - horizontalMargin * 2));
    }

    public static int buttonColumns(int screenWidth, int buttonCount, int buttonWidth, int gap,
                                    int horizontalMargin) {
        int availableWidth = Math.max(buttonWidth, screenWidth - horizontalMargin * 2);
        int columns = Math.max(1, (availableWidth + gap) / (buttonWidth + gap));
        return Math.max(1, Math.min(buttonCount, columns));
    }

    public static int buttonRows(int screenWidth, int buttonCount, int buttonWidth, int gap, int horizontalMargin) {
        int columns = buttonColumns(screenWidth, buttonCount, buttonWidth, gap, horizontalMargin);
        return Math.max(1, (buttonCount + columns - 1) / columns);
    }

    public static int visiblePageSize(int screenHeight, int listTop, int bottomContentTop, int rowHeight,
                                      int maxPageSize) {
        int availableHeight = Math.max(rowHeight, bottomContentTop - listTop);
        return Math.max(1, Math.min(maxPageSize, availableHeight / rowHeight));
    }
}
