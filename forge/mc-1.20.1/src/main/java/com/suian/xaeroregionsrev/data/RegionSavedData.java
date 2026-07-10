package com.suian.xaeroregionsrev.data;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.ColorPaletteLimits;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.nbt.CompoundTagNbtCompound;
import com.suian.xaeroregionsrev.region.nbt.CompoundTagNbtFactory;
import com.suian.xaeroregionsrev.region.nbt.RegionNbtCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RegionSavedData extends SavedData {
    private static final String DATA_NAME = "xaeroregionsrev_regions";
    private static final String REGIONS_KEY = "regions";
    private static final String COLOR_HISTORY_KEY = "colorHistory";
    private static final int TAG_INT = 3;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;
    private final Map<RegionId, Region> regions = new LinkedHashMap<>();
    private List<ArgbColor> colorHistory = List.of();

    public static RegionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RegionSavedData::load, RegionSavedData::new, DATA_NAME);
    }

    public static RegionSavedData load(CompoundTag tag) {
        RegionSavedData data = new RegionSavedData();
        if (tag.contains(REGIONS_KEY) && !tag.contains(REGIONS_KEY, TAG_LIST)) {
            throw new IllegalArgumentException("Saved region data field 'regions' must be a list.");
        }
        if (tag.contains(COLOR_HISTORY_KEY) && !tag.contains(COLOR_HISTORY_KEY, TAG_LIST)) {
            throw new IllegalArgumentException("Saved region data field 'colorHistory' must be a list.");
        }
        ListTag list = tag.getList(REGIONS_KEY, TAG_COMPOUND);
        CompoundTagNbtFactory factory = new CompoundTagNbtFactory();
        for (int i = 0; i < list.size(); i++) {
            try {
                Region region = RegionNbtCodec.readRegion(new CompoundTagNbtCompound(list.getCompound(i)));
                data.regions.put(region.id(), region);
            } catch (RuntimeException exception) {
                XaeroRegionsRev.LOGGER.warn("Skipping invalid saved region at index {}", i, exception);
            }
        }
        ListTag colorList = tag.getList(COLOR_HISTORY_KEY, TAG_INT);
        int colorCount = Math.min(colorList.size(), ColorPaletteLimits.MAX_COLORS);
        List<ArgbColor> loadedColors = new ArrayList<>(colorCount);
        for (int i = 0; i < colorCount; i++) {
            loadedColors.add(new ArgbColor(colorList.getInt(i)));
        }
        data.colorHistory = List.copyOf(loadedColors);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        CompoundTagNbtFactory factory = new CompoundTagNbtFactory();
        for (Region region : regions.values()) {
            list.add(((CompoundTagNbtCompound) RegionNbtCodec.writeRegion(factory, region)).tag());
        }
        tag.put(REGIONS_KEY, list);
        ListTag colorList = new ListTag();
        for (ArgbColor color : colorHistory) {
            colorList.add(IntTag.valueOf(color.value()));
        }
        tag.put(COLOR_HISTORY_KEY, colorList);
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

    public List<ArgbColor> colorHistory() {
        return colorHistory;
    }

    public void rememberColor(ArgbColor color, int limit) {
        List<ArgbColor> remembered = new ArrayList<>();
        remembered.add(color);
        for (ArgbColor existing : colorHistory) {
            if (!existing.equals(color)) {
                remembered.add(existing);
            }
            if (remembered.size() >= limit) {
                break;
            }
        }
        colorHistory = List.copyOf(remembered);
        setDirty();
    }
}
