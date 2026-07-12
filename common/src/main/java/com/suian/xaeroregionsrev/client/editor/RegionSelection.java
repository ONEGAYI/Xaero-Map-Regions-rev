package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.PolygonMath;
import com.suian.xaeroregionsrev.region.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RegionSelection {
    private RegionSelection() {
    }

    public static Optional<Region> selectTopmost(List<Region> regions, String dimension, int blockX, int blockZ) {
        List<Region> stack = selectStack(regions, dimension, blockX, blockZ);
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.get(stack.size() - 1));
    }

    public static List<Region> selectStack(List<Region> regions, String dimension, int blockX, int blockZ) {
        List<Region> hits = new ArrayList<>();
        for (Region region : regions) {
            if (!region.dimension().equals(dimension)) {
                continue;
            }
            if (region.points().size() >= 3 && PolygonMath.contains(region.points(), blockX, blockZ)) {
                hits.add(region);
            }
        }
        return hits;
    }
}
