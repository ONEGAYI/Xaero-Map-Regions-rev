package com.suian.xaeroregionsrev.data;

import com.suian.xaeroregionsrev.region.ArgbColor;
import net.minecraft.nbt.CompoundTag;
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
    void loadRejectsCorruptColorHistoryTagInsteadOfTreatingItAsEmpty() {
        CompoundTag tag = new CompoundTag();
        tag.putString("colorHistory", "not a list");

        assertThrows(IllegalArgumentException.class, () -> RegionSavedData.load(tag));
    }
}
