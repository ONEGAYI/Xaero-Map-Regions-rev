package com.suian.xaeroregionsrev.region.nbt;

/**
 * NBT 对象工厂接口，供平台子项目注入 CompoundTag/ListTag 创建逻辑。
 */
public interface NbtFactory {
    NbtCompound createCompound();
    NbtList createList();
}
