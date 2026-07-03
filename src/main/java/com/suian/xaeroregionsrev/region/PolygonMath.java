package com.suian.xaeroregionsrev.region;

import java.math.BigInteger;
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
        return !hasZeroArea(points) && !hasSelfIntersection(points);
    }

    public static boolean contains(List<RegionPoint> polygon, int x, int z) {
        if (!isValidPolygon(polygon)) {
            return false;
        }

        RegionPoint point = new RegionPoint(x, z);
        int windingNumber = 0;
        for (int index = 0; index < polygon.size(); index++) {
            RegionPoint current = polygon.get(index);
            RegionPoint next = polygon.get((index + 1) % polygon.size());
            long orientation = orientation(current, next, point);
            if (orientation == 0L && onSegment(current, point, next)) {
                return true;
            }
            if (current.z() <= z) {
                if (next.z() > z && orientation > 0L) {
                    windingNumber++;
                }
            } else if (next.z() <= z && orientation < 0L) {
                windingNumber--;
            }
        }
        return windingNumber != 0;
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
        return Long.signum(orientationExact(a, b, c).signum());
    }

    private static BigInteger orientationExact(RegionPoint a, RegionPoint b, RegionPoint c) {
        BigInteger abX = BigInteger.valueOf((long) b.x() - a.x());
        BigInteger acZ = BigInteger.valueOf((long) c.z() - a.z());
        BigInteger abZ = BigInteger.valueOf((long) b.z() - a.z());
        BigInteger acX = BigInteger.valueOf((long) c.x() - a.x());
        return abX.multiply(acZ).subtract(abZ.multiply(acX));
    }

    private static boolean onSegment(RegionPoint a, RegionPoint b, RegionPoint c) {
        return Math.min(a.x(), c.x()) <= b.x()
                && b.x() <= Math.max(a.x(), c.x())
                && Math.min(a.z(), c.z()) <= b.z()
                && b.z() <= Math.max(a.z(), c.z());
    }

    private static boolean hasZeroArea(List<RegionPoint> points) {
        BigInteger doubleArea = BigInteger.ZERO;
        for (int index = 0; index < points.size(); index++) {
            RegionPoint current = points.get(index);
            RegionPoint next = points.get((index + 1) % points.size());
            doubleArea = doubleArea.add(BigInteger.valueOf(current.x()).multiply(BigInteger.valueOf(next.z()))
                    .subtract(BigInteger.valueOf(next.x()).multiply(BigInteger.valueOf(current.z()))));
        }
        return doubleArea.signum() == 0;
    }
}
