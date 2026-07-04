package com.suian.xaeroregionsrev.data;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionSavedDataTest {
    @Test
    void loadRejectsCorruptRegionsTagInsteadOfTreatingItAsEmpty() {
        CompoundTag tag = new CompoundTag();
        tag.putString("regions", "not a list");

        assertThrows(IllegalArgumentException.class, () -> RegionSavedData.load(tag, emptyRegistries()));
    }

    @Test
    void savesAndLoadsSharedColorHistory() {
        RegionSavedData data = new RegionSavedData();
        data.rememberColor(new ArgbColor(0xFF112233), 4);
        data.rememberColor(new ArgbColor(0x80445566), 4);

        RegionSavedData loaded = RegionSavedData.load(data.save(new CompoundTag(), emptyRegistries()), emptyRegistries());

        assertEquals(List.of(new ArgbColor(0x80445566), new ArgbColor(0xFF112233)), loaded.colorHistory());
    }

    @Test
    void loadsForge1201RegionAndColorHistoryFixture() {
        CompoundTag root = new CompoundTag();
        ListTag regions = new ListTag();
        CompoundTag region = new CompoundTag();
        region.putString("id", "spawn");
        region.putString("name", "Spawn");
        region.putString("dimension", "minecraft:overworld");
        region.putInt("color", 0x8800FF00);
        region.putString("label", "Spawn Label");
        region.putInt("labelColor", 0xFFFFFFFF);
        region.putString("category", "default");
        region.putString("iconName", "default");
        region.putLong("createdAt", 100L);
        region.putLong("updatedAt", 200L);

        ListTag points = new ListTag();
        points.add(point(0, 0));
        points.add(point(16, 0));
        points.add(point(16, 16));
        region.put("points", points);
        regions.add(region);
        root.put("regions", regions);

        ListTag colorHistory = new ListTag();
        colorHistory.add(IntTag.valueOf(0x8800FF00));
        root.put("colorHistory", colorHistory);

        RegionSavedData loaded = RegionSavedData.load(root, emptyRegistries());

        assertEquals(1, loaded.allRegions().size());
        var loadedRegion = loaded.allRegions().iterator().next();
        assertEquals("spawn", loadedRegion.id().value());
        assertEquals("Spawn", loadedRegion.name());
        assertEquals("minecraft:overworld", loadedRegion.dimension());
        assertEquals(new ArgbColor(0x8800FF00), loadedRegion.color());
        assertEquals("Spawn Label", loadedRegion.label());
        assertEquals(new ArgbColor(0xFFFFFFFF), loadedRegion.labelColor());
        assertEquals("default", loadedRegion.category());
        assertEquals("default", loadedRegion.iconName());
        assertEquals(List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                loadedRegion.points());
        assertEquals(100L, loadedRegion.createdAt());
        assertEquals(200L, loadedRegion.updatedAt());
        assertEquals(List.of(new ArgbColor(0x8800FF00)), loaded.colorHistory());
    }

    @Test
    void sharedColorHistoryMovesDuplicatesToFrontAndTrimsLimit() {
        RegionSavedData data = new RegionSavedData();

        data.rememberColor(new ArgbColor(0xFF111111), 3);
        data.rememberColor(new ArgbColor(0xFF222222), 3);
        data.rememberColor(new ArgbColor(0xFF333333), 3);
        data.rememberColor(new ArgbColor(0xFF111111), 3);
        data.rememberColor(new ArgbColor(0xFF444444), 3);

        assertEquals(List.of(
                new ArgbColor(0xFF444444),
                new ArgbColor(0xFF111111),
                new ArgbColor(0xFF333333)
        ), data.colorHistory());
    }

    @Test
    void loadRejectsCorruptColorHistoryTagInsteadOfTreatingItAsEmpty() {
        CompoundTag tag = new CompoundTag();
        tag.putString("colorHistory", "not a list");

        assertThrows(IllegalArgumentException.class, () -> RegionSavedData.load(tag, emptyRegistries()));
    }

    private static HolderLookup.Provider emptyRegistries() {
        return HolderLookup.Provider.create(Stream.empty());
    }

    private static CompoundTag point(int x, int z) {
        CompoundTag point = new CompoundTag();
        point.putInt("x", x);
        point.putInt("z", z);
        return point;
    }
}
