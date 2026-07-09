package com.suian.xaeroregionsrev.data;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.ColorPaletteLimits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionSavedDataTest {
    @Test
    void loadRejectsCorruptRegionsTagInsteadOfTreatingItAsEmpty() {
        CompoundTag tag = new CompoundTag();
        tag.putString("regions", "not a list");

        assertThrows(IllegalArgumentException.class, () -> RegionSavedData.load(tag));
    }

    @Test
    void savesAndLoadsSharedColorHistory() {
        RegionSavedData data = new RegionSavedData();
        data.rememberColor(new ArgbColor(0xFF112233), 4);
        data.rememberColor(new ArgbColor(0x80445566), 4);

        RegionSavedData loaded = RegionSavedData.load(data.save(new CompoundTag()));

        assertEquals(List.of(new ArgbColor(0x80445566), new ArgbColor(0xFF112233)), loaded.colorHistory());
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
    void loadTrimsOversizedSharedColorHistoryToSyncLimit() {
        CompoundTag root = new CompoundTag();
        ListTag colorHistory = new ListTag();
        for (int i = 0; i < ColorPaletteLimits.MAX_COLORS + 3; i++) {
            colorHistory.add(IntTag.valueOf(0xFF000000 | i));
        }
        root.put("colorHistory", colorHistory);

        RegionSavedData loaded = RegionSavedData.load(root);

        assertEquals(ColorPaletteLimits.MAX_COLORS, loaded.colorHistory().size());
        assertEquals(new ArgbColor(0xFF000000), loaded.colorHistory().get(0));
        assertEquals(new ArgbColor(0xFF000000 | (ColorPaletteLimits.MAX_COLORS - 1)),
                loaded.colorHistory().get(loaded.colorHistory().size() - 1));
    }

    @Test
    void loadRejectsCorruptColorHistoryTagInsteadOfTreatingItAsEmpty() {
        CompoundTag tag = new CompoundTag();
        tag.putString("colorHistory", "not a list");

        assertThrows(IllegalArgumentException.class, () -> RegionSavedData.load(tag));
    }
}
