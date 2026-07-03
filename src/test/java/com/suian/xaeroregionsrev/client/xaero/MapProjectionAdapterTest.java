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
}
