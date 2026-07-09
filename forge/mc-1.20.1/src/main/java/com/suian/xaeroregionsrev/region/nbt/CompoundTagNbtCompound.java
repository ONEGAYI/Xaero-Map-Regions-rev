package com.suian.xaeroregionsrev.region.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * Forge 平台的 {@link NbtCompound} 适配实现，
 * 将平台无关接口委托给 {@link CompoundTag}。
 */
public final class CompoundTagNbtCompound implements NbtCompound {
    private final CompoundTag tag;

    public CompoundTagNbtCompound(CompoundTag tag) {
        this.tag = tag;
    }

    public CompoundTag tag() {
        return tag;
    }

    @Override
    public void putString(String key, String value) {
        tag.putString(key, value);
    }

    @Override
    public String getString(String key) {
        return tag.getString(key);
    }

    @Override
    public void putInt(String key, int value) {
        tag.putInt(key, value);
    }

    @Override
    public int getInt(String key) {
        return tag.getInt(key);
    }

    @Override
    public void putLong(String key, long value) {
        tag.putLong(key, value);
    }

    @Override
    public long getLong(String key) {
        return tag.getLong(key);
    }

    @Override
    public void put(String key, NbtList list) {
        tag.put(key, ((ListTagNbtList) list).tag());
    }

    @Override
    public NbtList getList(String key, int type) {
        ListTag list = tag.getList(key, type);
        return new ListTagNbtList(list);
    }

    @Override
    public boolean contains(String key, int type) {
        return tag.contains(key, type);
    }

    @Override
    public void remove(String key) {
        tag.remove(key);
    }
}
