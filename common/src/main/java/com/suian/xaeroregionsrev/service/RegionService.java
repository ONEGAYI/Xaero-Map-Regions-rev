package com.suian.xaeroregionsrev.service;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionStyleUpdater;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RegionService {
    public Collection<Region> list(RegionStore store) {
        Objects.requireNonNull(store, "Region store cannot be null.");
        return store.allRegions();
    }

    public List<Region> snapshot(ServerContext server) {
        Objects.requireNonNull(server, "Server context cannot be null.");
        List<Region> regions = new ArrayList<>();
        for (RegionStore store : server.allLevels()) {
            regions.addAll(list(store));
        }
        return List.copyOf(regions);
    }

    public Optional<Region> find(RegionStore store, RegionId id) {
        Objects.requireNonNull(store, "Region store cannot be null.");
        Objects.requireNonNull(id, "Region id cannot be null.");
        return store.find(id);
    }

    public void upsert(RegionStore store, Region region) {
        Objects.requireNonNull(store, "Region store cannot be null.");
        Objects.requireNonNull(region, "Region cannot be null.");
        if (!region.hasValidPolygon()) {
            throw new IllegalArgumentException("Region polygon must contain at least three points.");
        }
        store.put(region);
    }

    public Optional<Region> updateStyle(
            RegionStore store,
            RegionId id,
            ArgbColor fillColor,
            String label,
            ArgbColor labelColor,
            long now
    ) {
        Objects.requireNonNull(store, "Region store cannot be null.");
        Objects.requireNonNull(id, "Region id cannot be null.");
        Optional<Region> existing = store.find(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        Region updated = RegionStyleUpdater.withStyle(existing.get(), fillColor, label, labelColor, now);
        store.put(updated);
        return Optional.of(updated);
    }

    public boolean delete(RegionStore store, RegionId id) {
        Objects.requireNonNull(store, "Region store cannot be null.");
        Objects.requireNonNull(id, "Region id cannot be null.");
        return store.remove(id);
    }

    public List<ArgbColor> colorHistory(ServerContext server) {
        Objects.requireNonNull(server, "Server context cannot be null.");
        return server.overworld().colorHistory();
    }

    public List<ArgbColor> rememberColor(ServerContext server, ArgbColor color, int limit) {
        Objects.requireNonNull(server, "Server context cannot be null.");
        Objects.requireNonNull(color, "Color cannot be null.");
        server.overworld().rememberColor(color, limit);
        return server.overworld().colorHistory();
    }
}
