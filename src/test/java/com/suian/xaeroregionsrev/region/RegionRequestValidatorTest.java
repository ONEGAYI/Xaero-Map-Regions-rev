package com.suian.xaeroregionsrev.region;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionRequestValidatorTest {
    @Test
    void trimsNameAndLabelForCreateRequest() {
        var fillColor = new ArgbColor(0x8800FF00);
        var labelColor = new ArgbColor(0xFFFFFFFF);

        var validated = RegionRequestValidator.validateCreate(
                " Spawn ",
                fillColor,
                " Spawn Label ",
                labelColor,
                triangle()
        );

        assertEquals("Spawn", validated.name());
        assertEquals("Spawn Label", validated.label());
        assertEquals(fillColor, validated.fillColor());
        assertEquals(labelColor, validated.labelColor());
        assertEquals(triangle(), validated.points());
    }

    @Test
    void rejectsBlankNameAndLabel() {
        assertThrows(IllegalArgumentException.class, () -> RegionRequestValidator.validateCreate(
                " ",
                new ArgbColor(0x8800FF00),
                "Label",
                new ArgbColor(0xFFFFFFFF),
                triangle()
        ));
        assertThrows(IllegalArgumentException.class, () -> RegionRequestValidator.validateCreate(
                "Spawn",
                new ArgbColor(0x8800FF00),
                "\t",
                new ArgbColor(0xFFFFFFFF),
                triangle()
        ));
    }

    @Test
    void rejectsOverLimitNameLabelAndPoints() {
        String tooLongName = "n".repeat(RegionLimits.MAX_NAME_LENGTH + 1);
        String tooLongLabel = "l".repeat(RegionLimits.MAX_LABEL_LENGTH + 1);
        List<RegionPoint> tooManyPoints = new ArrayList<>();
        for (int index = 0; index <= RegionLimits.MAX_POINTS_PER_REQUEST; index++) {
            tooManyPoints.add(new RegionPoint(index, index % 2));
        }

        assertThrows(IllegalArgumentException.class, () -> RegionRequestValidator.validateCreate(
                tooLongName,
                new ArgbColor(0x8800FF00),
                "Label",
                new ArgbColor(0xFFFFFFFF),
                triangle()
        ));
        assertThrows(IllegalArgumentException.class, () -> RegionRequestValidator.validateCreate(
                "Spawn",
                new ArgbColor(0x8800FF00),
                tooLongLabel,
                new ArgbColor(0xFFFFFFFF),
                triangle()
        ));
        assertThrows(IllegalArgumentException.class, () -> RegionRequestValidator.validateCreate(
                "Spawn",
                new ArgbColor(0x8800FF00),
                "Label",
                new ArgbColor(0xFFFFFFFF),
                tooManyPoints
        ));
    }

    @Test
    void rejectsTooFewPointsAndDegeneratePolygons() {
        assertThrows(IllegalArgumentException.class, () -> RegionRequestValidator.validateCreate(
                "Spawn",
                new ArgbColor(0x8800FF00),
                "Label",
                new ArgbColor(0xFFFFFFFF),
                List.of(new RegionPoint(0, 0), new RegionPoint(16, 0))
        ));
        assertThrows(IllegalArgumentException.class, () -> RegionRequestValidator.validateCreate(
                "Spawn",
                new ArgbColor(0x8800FF00),
                "Label",
                new ArgbColor(0xFFFFFFFF),
                List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(32, 0))
        ));
    }

    @Test
    void returnedPointsAreImmutable() {
        var validated = RegionRequestValidator.validateCreate(
                "Spawn",
                new ArgbColor(0x8800FF00),
                "Label",
                new ArgbColor(0xFFFFFFFF),
                triangle()
        );

        assertThrows(UnsupportedOperationException.class, () -> validated.points().add(new RegionPoint(1, 1)));
    }

    private static List<RegionPoint> triangle() {
        return List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16));
    }
}
