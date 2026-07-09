package com.suian.xaeroregionsrev.region.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * Forge 平台的 {@link NbtFactory} 适配实现，
 * 创建包装后的 {@link CompoundTag} 与 {@link ListTag}。
 */
public final class CompoundTagNbtFactory implements NbtFactory {
    @Override
    public NbtCompound createCompound() {
        return new CompoundTagNbtCompound(new CompoundTag());
    }

    @Override
    public NbtList createList() {
        return new ListTagNbtList(new ListTag());
    }
}
