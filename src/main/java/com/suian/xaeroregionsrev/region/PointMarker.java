package com.suian.xaeroregionsrev.region;

import java.util.UUID;

public record PointMarker(
        UUID targetPlayer,
        String mode,
        String iconName,
        String label,
        int x,
        int y,
        int z
) {
    public PointMarker {
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("Point marker mode cannot be blank.");
        }
        if (iconName == null || iconName.isBlank()) {
            throw new IllegalArgumentException("Point marker icon name cannot be blank.");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Point marker label cannot be blank.");
        }
    }
}
