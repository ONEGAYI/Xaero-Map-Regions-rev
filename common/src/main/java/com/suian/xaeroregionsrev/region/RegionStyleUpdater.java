package com.suian.xaeroregionsrev.region;

import java.util.Objects;

public final class RegionStyleUpdater {
    private RegionStyleUpdater() {
    }

    public static Region withStyle(
            Region region,
            ArgbColor fillColor,
            String label,
            ArgbColor labelColor,
            long updatedAt
    ) {
        Objects.requireNonNull(region, "Region cannot be null.");
        return new Region(
                region.id(),
                region.name(),
                region.dimension(),
                fillColor,
                label,
                labelColor,
                region.category(),
                region.iconName(),
                region.points(),
                region.createdAt(),
                updatedAt
        );
    }
}
