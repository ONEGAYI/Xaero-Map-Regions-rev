package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.PolygonMath;
import com.suian.xaeroregionsrev.region.Region;

import java.util.List;
import java.util.Optional;

public final class RegionSelection {
    private RegionSelection() {
    }

    public static Optional<Region> selectTopmost(List<Region> regions, String dimension, int blockX, int blockZ) {
        Region selected = null;
        for (Region region : regions) {
            if (!region.dimension().equals(dimension)) {
                continue;
            }
            if (region.points().size() >= 3 && PolygonMath.contains(region.points(), blockX, blockZ)) {
                selected = region;
            }
        }
        return Optional.ofNullable(selected);
    }
}
