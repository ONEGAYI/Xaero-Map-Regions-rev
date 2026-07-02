package com.suian.xaeroregionsrev.client;

import com.suian.xaeroregionsrev.region.Region;

import java.util.List;

public final class ClientRegionCache {
    private static List<Region> regions = List.of();

    private ClientRegionCache() {
    }

    public static List<Region> regions() {
        return regions;
    }

    public static void replaceAll(List<Region> syncedRegions) {
        regions = List.copyOf(syncedRegions);
    }

    public static void clear() {
        regions = List.of();
    }
}
