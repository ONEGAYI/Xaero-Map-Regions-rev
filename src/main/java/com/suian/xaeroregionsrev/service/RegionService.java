package com.suian.xaeroregionsrev.service;

import com.suian.xaeroregionsrev.data.RegionSavedData;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionStyleUpdater;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RegionService {
    public Collection<Region> list(ServerLevel level) {
        Objects.requireNonNull(level, "Server level cannot be null.");
        return RegionSavedData.get(level).allRegions();
    }

    public List<Region> snapshot(MinecraftServer server) {
        Objects.requireNonNull(server, "Minecraft server cannot be null.");
        List<Region> regions = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            regions.addAll(list(level));
        }
        return List.copyOf(regions);
    }

    public Optional<Region> find(ServerLevel level, RegionId id) {
        Objects.requireNonNull(level, "Server level cannot be null.");
        Objects.requireNonNull(id, "Region id cannot be null.");
        return RegionSavedData.get(level).find(id);
    }

    public void upsert(ServerLevel level, Region region) {
        Objects.requireNonNull(level, "Server level cannot be null.");
        Objects.requireNonNull(region, "Region cannot be null.");
        if (!region.hasValidPolygon()) {
            throw new IllegalArgumentException("Region polygon must contain at least three points.");
        }
        RegionSavedData.get(level).put(region);
    }

    public Optional<Region> updateStyle(
            ServerLevel level,
            RegionId id,
            ArgbColor fillColor,
            String label,
            ArgbColor labelColor,
            long now
    ) {
        Objects.requireNonNull(level, "Server level cannot be null.");
        Objects.requireNonNull(id, "Region id cannot be null.");
        RegionSavedData data = RegionSavedData.get(level);
        Optional<Region> existing = data.find(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        Region updated = RegionStyleUpdater.withStyle(existing.get(), fillColor, label, labelColor, now);
        data.put(updated);
        return Optional.of(updated);
    }

    public boolean delete(ServerLevel level, RegionId id) {
        Objects.requireNonNull(level, "Server level cannot be null.");
        Objects.requireNonNull(id, "Region id cannot be null.");
        return RegionSavedData.get(level).remove(id);
    }
}
