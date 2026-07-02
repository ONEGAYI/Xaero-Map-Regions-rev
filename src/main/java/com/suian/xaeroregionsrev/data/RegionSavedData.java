package com.suian.xaeroregionsrev.data;

import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionNbtCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class RegionSavedData extends SavedData {
    private static final String DATA_NAME = "xaeroregionsrev_regions";
    private final Map<RegionId, Region> regions = new LinkedHashMap<>();

    public static RegionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RegionSavedData::load, RegionSavedData::new, DATA_NAME);
    }

    public static RegionSavedData load(CompoundTag tag) {
        RegionSavedData data = new RegionSavedData();
        ListTag list = tag.getList("regions", 10);
        for (int i = 0; i < list.size(); i++) {
            Region region = RegionNbtCodec.readRegion(list.getCompound(i));
            data.regions.put(region.id(), region);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Region region : regions.values()) {
            list.add(RegionNbtCodec.writeRegion(region));
        }
        tag.put("regions", list);
        return tag;
    }

    public Collection<Region> allRegions() {
        return regions.values();
    }

    public Optional<Region> find(RegionId id) {
        return Optional.ofNullable(regions.get(id));
    }

    public void put(Region region) {
        regions.put(region.id(), region);
        setDirty();
    }

    public boolean remove(RegionId id) {
        boolean removed = regions.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }
}
