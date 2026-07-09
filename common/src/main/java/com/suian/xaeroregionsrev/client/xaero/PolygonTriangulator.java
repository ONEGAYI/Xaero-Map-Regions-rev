package com.suian.xaeroregionsrev.client.xaero;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public final class PolygonTriangulator {
    private static final float EPSILON = 0.00001F;

    private PolygonTriangulator() {
    }

    public record Triangle(Vector2f a, Vector2f b, Vector2f c) {
    }

    public static List<Triangle> triangulate(List<Vector2f> polygon) {
        if (polygon == null || polygon.size() < 3) {
            return List.of();
        }
        List<Integer> indices = new ArrayList<>(polygon.size());
        for (int index = 0; index < polygon.size(); index++) {
            indices.add(index);
        }
        float winding = Math.signum(signedArea(polygon));
        if (winding == 0.0F) {
            return List.of();
        }

        List<Triangle> triangles = new ArrayList<>(polygon.size() - 2);
        int guard = polygon.size() * polygon.size();
        while (indices.size() > 3 && guard-- > 0) {
            boolean clipped = false;
            for (int index = 0; index < indices.size(); index++) {
                int previous = indices.get((index - 1 + indices.size()) % indices.size());
                int current = indices.get(index);
                int next = indices.get((index + 1) % indices.size());
                if (!isEar(polygon, indices, previous, current, next, winding)) {
                    continue;
                }
                triangles.add(new Triangle(polygon.get(previous), polygon.get(current), polygon.get(next)));
                indices.remove(index);
                clipped = true;
                break;
            }
            if (!clipped) {
                return fanFallback(polygon);
            }
        }
        if (indices.size() == 3) {
            triangles.add(new Triangle(polygon.get(indices.get(0)), polygon.get(indices.get(1)), polygon.get(indices.get(2))));
        }
        return List.copyOf(triangles);
    }

    private static boolean isEar(List<Vector2f> polygon, List<Integer> indices, int previous, int current, int next,
                                 float winding) {
        Vector2f a = polygon.get(previous);
        Vector2f b = polygon.get(current);
        Vector2f c = polygon.get(next);
        if (cross(a, b, c) * winding <= EPSILON) {
            return false;
        }
        for (int candidate : indices) {
            if (candidate == previous || candidate == current || candidate == next) {
                continue;
            }
            if (isPointInsideTriangle(polygon.get(candidate), a, b, c, winding)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPointInsideTriangle(Vector2f point, Vector2f a, Vector2f b, Vector2f c, float winding) {
        return cross(a, b, point) * winding >= -EPSILON
                && cross(b, c, point) * winding >= -EPSILON
                && cross(c, a, point) * winding >= -EPSILON;
    }

    private static List<Triangle> fanFallback(List<Vector2f> polygon) {
        List<Triangle> triangles = new ArrayList<>(Math.max(0, polygon.size() - 2));
        for (int index = 1; index < polygon.size() - 1; index++) {
            triangles.add(new Triangle(polygon.get(0), polygon.get(index), polygon.get(index + 1)));
        }
        return List.copyOf(triangles);
    }

    private static float signedArea(List<Vector2f> polygon) {
        float area = 0.0F;
        for (int index = 0; index < polygon.size(); index++) {
            Vector2f current = polygon.get(index);
            Vector2f next = polygon.get((index + 1) % polygon.size());
            area += current.x() * next.y() - next.x() * current.y();
        }
        return area / 2.0F;
    }

    private static float cross(Vector2f a, Vector2f b, Vector2f c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }
}
