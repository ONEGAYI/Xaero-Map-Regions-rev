package com.suian.xaeroregionsrev.platform;

import com.suian.xaeroregionsrev.data.RegionSavedData;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.service.RegionStore;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link RegionStore} 适配实现，委托给指定维度的 {@link RegionSavedData}。
 */
public final class RegionSavedDataStore implements RegionStore {
    private final RegionSavedData savedData;

    public RegionSavedDataStore(RegionSavedData savedData) {
        this.savedData = Objects.requireNonNull(savedData, "savedData");
    }

    /**
     * 从 {@link ServerLevel} 解析 {@link RegionSavedData} 并包装为 {@link RegionSavedDataStore}。
     */
    public static RegionSavedDataStore of(ServerLevel level) {
        return new RegionSavedDataStore(RegionSavedData.get(level));
    }

    public RegionSavedData savedData() {
        return savedData;
    }

    @Override
    public Collection<Region> allRegions() {
        return savedData.allRegions();
    }

    @Override
    public Optional<Region> find(RegionId id) {
        return savedData.find(id);
    }

    @Override
    public void put(Region region) {
        savedData.put(region);
    }

    @Override
    public boolean remove(RegionId id) {
        return savedData.remove(id);
    }

    @Override
    public List<ArgbColor> colorHistory() {
        return savedData.colorHistory();
    }

    @Override
    public void rememberColor(ArgbColor color, int limit) {
        savedData.rememberColor(color, limit);
    }
}
