package com.suian.xaeroregionsrev.region;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public final class RegionRequestValidator {
    private RegionRequestValidator() {
    }

    public static ValidatedRegionCreateRequest validateCreate(
            String name,
            ArgbColor fillColor,
            String label,
            ArgbColor labelColor,
            List<RegionPoint> points
    ) {
        String normalizedName = normalizeRequiredText(name, RegionLimits.MAX_NAME_LENGTH, "Region name");
        var style = validateStyle(fillColor, label, labelColor);
        List<RegionPoint> normalizedPoints = validatePoints(points);
        return new ValidatedRegionCreateRequest(
                normalizedName,
                style.fillColor(),
                style.label(),
                style.labelColor(),
                normalizedPoints
        );
    }

    public static ValidatedRegionStyleRequest validateStyle(
            ArgbColor fillColor,
            String label,
            ArgbColor labelColor
    ) {
        Objects.requireNonNull(fillColor, "Region fill color cannot be null.");
        Objects.requireNonNull(labelColor, "Region label color cannot be null.");
        String normalizedLabel = normalizeRequiredText(label, RegionLimits.MAX_LABEL_LENGTH, "Region label");
        return new ValidatedRegionStyleRequest(fillColor, normalizedLabel, labelColor);
    }

    private static String normalizeRequiredText(String text, int maxLength, String fieldName) {
        if (text == null) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        String normalized = text.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        if (normalized.getBytes(StandardCharsets.UTF_8).length > maxLength) {
            throw new IllegalArgumentException(fieldName + " cannot exceed " + maxLength + " UTF-8 bytes.");
        }
        return normalized;
    }

    private static List<RegionPoint> validatePoints(List<RegionPoint> points) {
        if (points == null) {
            throw new IllegalArgumentException("Region points cannot be null.");
        }
        if (points.size() > RegionLimits.MAX_POINTS_PER_REQUEST) {
            throw new IllegalArgumentException("Region point count cannot exceed " + RegionLimits.MAX_POINTS_PER_REQUEST + ".");
        }
        List<RegionPoint> normalizedPoints = List.copyOf(points);
        if (!PolygonMath.isValidPolygon(normalizedPoints)) {
            throw new IllegalArgumentException("Region points must form a valid polygon.");
        }
        return normalizedPoints;
    }

    public record ValidatedRegionCreateRequest(
            String name,
            ArgbColor fillColor,
            String label,
            ArgbColor labelColor,
            List<RegionPoint> points
    ) {
        public ValidatedRegionCreateRequest {
            points = List.copyOf(points);
        }
    }

    public record ValidatedRegionStyleRequest(
            ArgbColor fillColor,
            String label,
            ArgbColor labelColor
    ) {
    }
}
