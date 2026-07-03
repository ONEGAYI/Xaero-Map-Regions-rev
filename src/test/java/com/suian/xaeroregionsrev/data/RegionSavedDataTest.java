package com.suian.xaeroregionsrev.data;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionSavedDataTest {
    @Test
    void loadRejectsCorruptRegionsTagInsteadOfTreatingItAsEmpty() {
        CompoundTag tag = new CompoundTag();
        tag.putString("regions", "not a list");

        assertThrows(IllegalArgumentException.class, () -> RegionSavedData.load(tag));
    }
}
