package com.suian.xaeroregionsrev.region;

import java.util.List;

public final class PolygonMath {
    private PolygonMath() {
    }

    public static boolean isValidPolygon(List<RegionPoint> points) {
        return points != null && points.size() >= 3;
    }

    public static boolean contains(List<RegionPoint> polygon, int x, int z) {
        if (!isValidPolygon(polygon)) {
            return false;
        }

        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            RegionPoint pi = polygon.get(i);
            RegionPoint pj = polygon.get(j);
            boolean intersects = ((pi.z() > z) != (pj.z() > z))
                    && (x < (long) (pj.x() - pi.x()) * (z - pi.z()) / (double) (pj.z() - pi.z()) + pi.x());
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }
}
