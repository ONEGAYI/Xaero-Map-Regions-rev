package com.suian.xaeroregionsrev.region;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PolygonMath {
    private PolygonMath() {
    }

    public static boolean isValidPolygon(List<RegionPoint> points) {
        if (points == null || points.size() < 3 || hasRepeatedPoints(points)) {
            return false;
        }
        return !hasSelfIntersection(points);
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

    private static boolean hasRepeatedPoints(List<RegionPoint> points) {
        Set<RegionPoint> uniquePoints = new HashSet<>();
        for (RegionPoint point : points) {
            if (point == null || !uniquePoints.add(point)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSelfIntersection(List<RegionPoint> points) {
        int size = points.size();
        for (int first = 0; first < size; first++) {
            RegionPoint a = points.get(first);
            RegionPoint b = points.get((first + 1) % size);
            for (int second = first + 1; second < size; second++) {
                if (areAdjacentEdges(first, second, size)) {
                    continue;
                }
                RegionPoint c = points.get(second);
                RegionPoint d = points.get((second + 1) % size);
                if (segmentsIntersect(a, b, c, d)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean areAdjacentEdges(int first, int second, int size) {
        return first == second
                || (first + 1) % size == second
                || (second + 1) % size == first;
    }

    private static boolean segmentsIntersect(RegionPoint a, RegionPoint b, RegionPoint c, RegionPoint d) {
        long abC = orientation(a, b, c);
        long abD = orientation(a, b, d);
        long cdA = orientation(c, d, a);
        long cdB = orientation(c, d, b);

        if (abC == 0L && onSegment(a, c, b)) {
            return true;
        }
        if (abD == 0L && onSegment(a, d, b)) {
            return true;
        }
        if (cdA == 0L && onSegment(c, a, d)) {
            return true;
        }
        if (cdB == 0L && onSegment(c, b, d)) {
            return true;
        }
        return (abC > 0L) != (abD > 0L) && (cdA > 0L) != (cdB > 0L);
    }

    private static long orientation(RegionPoint a, RegionPoint b, RegionPoint c) {
        return (long) (b.x() - a.x()) * (c.z() - a.z()) - (long) (b.z() - a.z()) * (c.x() - a.x());
    }

    private static boolean onSegment(RegionPoint a, RegionPoint b, RegionPoint c) {
        return Math.min(a.x(), c.x()) <= b.x()
                && b.x() <= Math.max(a.x(), c.x())
                && Math.min(a.z(), c.z()) <= b.z()
                && b.z() <= Math.max(a.z(), c.z());
    }
}
