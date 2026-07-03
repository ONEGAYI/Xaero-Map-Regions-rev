package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.region.RegionPoint;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapProjectionAdapterTest {
    @Test
    void projectAndUnprojectRoundTripBlockCoordinates() {
        RegionPoint original = new RegionPoint(-13, 27);

        Vector2f projected = MapProjectionAdapter.projectRelative(
                original.x(),
                original.z(),
                -10.25D,
                20.5D,
                400.0F,
                300.0F,
                0.5F
        );

        assertEquals(original, MapProjectionAdapter.unprojectRelative(
                projected.x(),
                projected.y(),
                -10.25D,
                20.5D,
                400.0F,
                300.0F,
                0.5F
        ));
    }

    @Test
    void unprojectUsesFloorForNegativeCoordinates() {
        assertEquals(new RegionPoint(-2, -3), MapProjectionAdapter.unprojectRelative(
                399.9D,
                299.1D,
                -1.0D,
                -2.0D,
                400.0F,
                300.0F,
                1.0F
        ));
    }

    @Test
    void projectWithViewportMovesRegionWithMapCamera() {
        RegionPoint point = new RegionPoint(120, 220);
        MapProjectionAdapter.MapViewport originalViewport = new MapProjectionAdapter.MapViewport(
                100.0D,
                200.0D,
                400.0F,
                300.0F,
                2.0F,
                1.0D
        );
        MapProjectionAdapter.MapViewport draggedViewport = new MapProjectionAdapter.MapViewport(
                110.0D,
                205.0D,
                400.0F,
                300.0F,
                2.0F,
                1.0D
        );

        Vector2f original = MapProjectionAdapter.projectInViewport(point, originalViewport);
        Vector2f dragged = MapProjectionAdapter.projectInViewport(point, draggedViewport);

        assertEquals(440.0F, original.x());
        assertEquals(340.0F, original.y());
        assertEquals(420.0F, dragged.x());
        assertEquals(330.0F, dragged.y());
    }

    @Test
    void viewportProjectionRoundTripsCoordinates() {
        RegionPoint original = new RegionPoint(85, -34);
        MapProjectionAdapter.MapViewport viewport = new MapProjectionAdapter.MapViewport(
                80.5D,
                -40.25D,
                400.0F,
                300.0F,
                1.5F,
                1.0D
        );

        Vector2f projected = MapProjectionAdapter.projectInViewport(original, viewport);

        assertEquals(original, MapProjectionAdapter.unprojectInViewport(projected.x(), projected.y(), viewport));
    }

    @Test
    void viewportProjectionConvertsXaeroPhysicalScaleToGuiScale() {
        RegionPoint point = new RegionPoint(120, 220);
        MapProjectionAdapter.MapViewport viewport = new MapProjectionAdapter.MapViewport(
                100.0D,
                200.0D,
                400.0F,
                300.0F,
                2.0F,
                1.0D,
                2.0D
        );

        Vector2f projected = MapProjectionAdapter.projectInViewport(point, viewport);

        assertEquals(420.0F, projected.x());
        assertEquals(320.0F, projected.y());
        assertEquals(point, MapProjectionAdapter.unprojectInViewport(projected.x(), projected.y(), viewport));
    }

    @Test
    void viewportCalibrationAlignsProjectedWorldPointWithXaeroMouseWorldPoint() {
        MapProjectionAdapter.MapViewport rawViewport = new MapProjectionAdapter.MapViewport(
                100.0D,
                200.0D,
                400.0F,
                300.0F,
                1.0F,
                1.0D
        );

        MapProjectionAdapter.MapViewport calibrated = MapProjectionAdapter.calibrateViewport(
                rawViewport,
                430.0D,
                320.0D,
                new RegionPoint(125, 215)
        );

        assertEquals(new RegionPoint(125, 215), MapProjectionAdapter.unprojectInViewport(430.0D, 320.0D, calibrated));
        Vector2f projected = MapProjectionAdapter.projectInViewport(new RegionPoint(125, 215), calibrated);
        assertEquals(430.0F, projected.x());
        assertEquals(320.0F, projected.y());
    }

    @Test
    void calibrationRespectsCoordinateDivisor() {
        MapProjectionAdapter.MapViewport rawViewport = new MapProjectionAdapter.MapViewport(
                50.0D,
                80.0D,
                400.0F,
                300.0F,
                2.0F,
                8.0D,
                2.0D
        );

        MapProjectionAdapter.MapViewport calibrated = MapProjectionAdapter.calibrateViewport(
                rawViewport,
                410.0D,
                320.0D,
                new RegionPoint(440, 760)
        );

        assertEquals(new RegionPoint(440, 760), MapProjectionAdapter.unprojectInViewport(410.0D, 320.0D, calibrated));
    }

    @Test
    void calibrationIsDueImmediatelyThenWaitsForInterval() {
        assertEquals(true, MapProjectionAdapter.isCalibrationDue(100L, Long.MIN_VALUE));
        assertEquals(false, MapProjectionAdapter.isCalibrationDue(500_000_000L, 100L));
        assertEquals(true, MapProjectionAdapter.isCalibrationDue(1_000_000_100L, 100L));
    }

    @Test
    void calibrationDueTreatsClockRollbackAsDue() {
        assertEquals(true, MapProjectionAdapter.isCalibrationDue(50L, 100L));
    }

    @Test
    void viewportDefaultsNonFiniteCameraCoordinates() {
        MapProjectionAdapter.MapViewport viewport = new MapProjectionAdapter.MapViewport(
                Double.NaN,
                Double.POSITIVE_INFINITY,
                400.0F,
                300.0F,
                1.0F,
                1.0D
        );

        assertEquals(0.0D, viewport.cameraX());
        assertEquals(0.0D, viewport.cameraZ());
    }

    @Test
    void unprojectClampsCoordinatesToIntegerRange() {
        MapProjectionAdapter.MapViewport viewport = new MapProjectionAdapter.MapViewport(
                0.0D,
                0.0D,
                0.0F,
                0.0F,
                0.000001F,
                1.0D
        );

        assertEquals(new RegionPoint(Integer.MAX_VALUE, Integer.MIN_VALUE),
                MapProjectionAdapter.unprojectInViewport(Double.MAX_VALUE, -Double.MAX_VALUE, viewport));
    }
}
