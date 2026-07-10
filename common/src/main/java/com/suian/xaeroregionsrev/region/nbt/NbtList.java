package com.suian.xaeroregionsrev.region.nbt;

/**
 * 平台无关的 NBT 列表标签接口。
 */
public interface NbtList {
    void add(NbtCompound compound);
    NbtCompound getCompound(int index);
    int size();
}
