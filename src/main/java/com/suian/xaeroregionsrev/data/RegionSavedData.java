package com.suian.xaeroregionsrev.data;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionNbtCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RegionSavedData extends SavedData {
    private static final String DATA_NAME = "xaeroregionsrev_regions";
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;
    private final Map<RegionId, Region> regions = new LinkedHashMap<>();

    public static RegionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RegionSavedData::load, RegionSavedData::new, DATA_NAME);
    }

    public static RegionSavedData load(CompoundTag tag) {
        RegionSavedData data = new RegionSavedData();
        if (tag.contains("regions") && !tag.contains("regions", TAG_LIST)) {
            throw new IllegalArgumentException("Saved region data field 'regions' must be a list.");
        }
        ListTag list = tag.getList("regions", TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                Region region = RegionNbtCodec.readRegion(list.getCompound(i));
                data.regions.put(region.id(), region);
            } catch (RuntimeException exception) {
                XaeroRegionsRev.LOGGER.warn("Skipping invalid saved region at index {}", i, exception);
            }
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
        return List.copyOf(regions.values());
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
