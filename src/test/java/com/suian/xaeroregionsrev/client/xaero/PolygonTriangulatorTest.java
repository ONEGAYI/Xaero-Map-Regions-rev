package com.suian.xaeroregionsrev.client.xaero;

import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolygonTriangulatorTest {
    @Test
    void triangulatesConcavePolygonWithoutFillingTheNotch() {
        List<Vector2f> lShape = List.of(
                new Vector2f(0, 0),
                new Vector2f(4, 0),
                new Vector2f(4, 1),
                new Vector2f(1, 1),
                new Vector2f(1, 4),
                new Vector2f(0, 4)
        );

        List<PolygonTriangulator.Triangle> triangles = PolygonTriangulator.triangulate(lShape);

        assertEquals(4, triangles.size());
        assertEquals(7.0F, totalArea(triangles), 0.0001F);
    }

    private static float totalArea(List<PolygonTriangulator.Triangle> triangles) {
        float area = 0.0F;
        for (PolygonTriangulator.Triangle triangle : triangles) {
            area += Math.abs(cross(triangle.a(), triangle.b(), triangle.c())) / 2.0F;
        }
        return area;
    }

    private static float cross(Vector2f a, Vector2f b, Vector2f c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }
}
