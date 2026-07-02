package com.suian.xaeroregionsrev.service;

import com.suian.xaeroregionsrev.data.RegionSavedData;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public final class RegionService {
    public Collection<Region> list(ServerLevel level) {
        Objects.requireNonNull(level, "Server level cannot be null.");
        return RegionSavedData.get(level).allRegions();
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

    public boolean delete(ServerLevel level, RegionId id) {
        Objects.requireNonNull(level, "Server level cannot be null.");
        Objects.requireNonNull(id, "Region id cannot be null.");
        return RegionSavedData.get(level).remove(id);
    }
}
