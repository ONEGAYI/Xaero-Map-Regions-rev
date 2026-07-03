package com.suian.xaeroregionsrev.region;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolygonMathTest {
    @Test
    void pointInsideSquareReturnsTrue() {
        var polygon = List.of(
                new RegionPoint(0, 0),
                new RegionPoint(10, 0),
                new RegionPoint(10, 10),
                new RegionPoint(0, 10)
        );

        assertTrue(PolygonMath.contains(polygon, 5, 5));
    }

    @Test
    void pointOutsideSquareReturnsFalse() {
        var polygon = List.of(
                new RegionPoint(0, 0),
                new RegionPoint(10, 0),
                new RegionPoint(10, 10),
                new RegionPoint(0, 10)
        );

        assertFalse(PolygonMath.contains(polygon, 15, 5));
    }

    @Test
    void lessThanThreePointsIsInvalid() {
        assertFalse(PolygonMath.isValidPolygon(List.of(new RegionPoint(0, 0), new RegionPoint(1, 1))));
    }

    @Test
    void repeatedPointsAreInvalid() {
        assertFalse(PolygonMath.isValidPolygon(List.of(
                new RegionPoint(0, 0),
                new RegionPoint(10, 0),
                new RegionPoint(10, 10),
                new RegionPoint(10, 0)
        )));
    }

    @Test
    void selfIntersectingPolygonIsInvalid() {
        assertFalse(PolygonMath.isValidPolygon(List.of(
                new RegionPoint(0, 0),
                new RegionPoint(10, 10),
                new RegionPoint(0, 10),
                new RegionPoint(10, 0)
        )));
    }
}
