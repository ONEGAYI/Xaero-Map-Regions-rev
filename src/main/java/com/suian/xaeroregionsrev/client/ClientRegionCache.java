package com.suian.xaeroregionsrev.client;

import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.region.Region;
import org.slf4j.Logger;

import java.util.List;

public final class ClientRegionCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static List<Region> regions = List.of();

    private ClientRegionCache() {
    }

    public static List<Region> regions() {
        return regions;
    }

    public static void replaceAll(List<Region> syncedRegions) {
        regions = List.copyOf(syncedRegions);
        LOGGER.info("Client region cache updated: {} region(s).", regions.size());
    }

    public static void clear() {
        regions = List.of();
        LOGGER.info("Client region cache cleared.");
    }
}
