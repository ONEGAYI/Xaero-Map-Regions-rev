package com.suian.xaeroregionsrev.region;

import java.util.List;

public record Region(
        RegionId id,
        String name,
        String dimension,
        ArgbColor color,
        String label,
        ArgbColor labelColor,
        String category,
        String iconName,
        List<RegionPoint> points,
        long createdAt,
        long updatedAt
) {
    public Region(
            RegionId id,
            String name,
            String dimension,
            ArgbColor color,
            String category,
            String iconName,
            List<RegionPoint> points,
            long createdAt,
            long updatedAt
    ) {
        this(id, name, dimension, color, name, new ArgbColor(0xFFFFFFFF), category, iconName, points, createdAt, updatedAt);
    }

    public Region {
        if (id == null) {
            throw new IllegalArgumentException("Region id cannot be null.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Region name cannot be blank.");
        }
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("Region dimension cannot be blank.");
        }
        if (color == null) {
            throw new IllegalArgumentException("Region color cannot be null.");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Region label cannot be blank.");
        }
        if (labelColor == null) {
            throw new IllegalArgumentException("Region label color cannot be null.");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Region category cannot be blank.");
        }
        if (iconName == null || iconName.isBlank()) {
            throw new IllegalArgumentException("Region icon name cannot be blank.");
        }
        if (points == null) {
            throw new IllegalArgumentException("Region points cannot be null.");
        }
        points = List.copyOf(points);
    }

    public boolean hasValidPolygon() {
        return PolygonMath.isValidPolygon(points);
    }
}
