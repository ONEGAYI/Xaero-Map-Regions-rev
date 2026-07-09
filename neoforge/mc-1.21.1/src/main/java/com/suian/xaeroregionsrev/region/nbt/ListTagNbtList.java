package com.suian.xaeroregionsrev.region.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * NeoForge 平台的 {@link NbtList} 适配实现，
 * 将平台无关接口委托给 {@link ListTag}。
 */
public final class ListTagNbtList implements NbtList {
    private final ListTag tag;

    public ListTagNbtList(ListTag tag) {
        this.tag = tag;
    }

    public ListTag tag() {
        return tag;
    }

    @Override
    public void add(NbtCompound compound) {
        tag.add(((CompoundTagNbtCompound) compound).tag());
    }

    @Override
    public NbtCompound getCompound(int index) {
        CompoundTag compound = tag.getCompound(index);
        return new CompoundTagNbtCompound(compound);
    }

    @Override
    public int size() {
        return tag.size();
    }
}
