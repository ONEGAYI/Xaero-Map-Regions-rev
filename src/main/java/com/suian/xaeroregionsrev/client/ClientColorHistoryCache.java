package com.suian.xaeroregionsrev.client;

import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.region.ArgbColor;
import org.slf4j.Logger;

import java.util.List;

public final class ClientColorHistoryCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static List<ArgbColor> colors = List.of();

    private ClientColorHistoryCache() {
    }

    public static List<ArgbColor> colors() {
        return colors;
    }

    public static void replaceAll(List<ArgbColor> syncedColors) {
        colors = List.copyOf(syncedColors);
        LOGGER.info("Client color history cache updated: {} color(s).", colors.size());
    }

    public static void clear() {
        colors = List.of();
        LOGGER.info("Client color history cache cleared.");
    }
}
