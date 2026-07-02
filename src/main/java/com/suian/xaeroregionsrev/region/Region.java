package com.suian.xaeroregionsrev.region;

import java.util.List;

public record Region(
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
    public Region {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Region name cannot be blank.");
        }
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("Region dimension cannot be blank.");
        }
        points = List.copyOf(points);
    }

    public boolean hasValidPolygon() {
        return PolygonMath.isValidPolygon(points);
    }
}
