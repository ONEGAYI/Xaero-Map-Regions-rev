package com.suian.xaeroregionsrev.region.nbt;

/**
 * 平台无关的 NBT 复合标签接口。
 * 各平台子项目提供适配实现（如 CompoundTagNbtCompound）。
 */
public interface NbtCompound {
    void putString(String key, String value);
    String getString(String key);

    void putInt(String key, int value);
    int getInt(String key);

    void putLong(String key, long value);
    long getLong(String key);

    void put(String key, NbtList list);
    NbtList getList(String key, int type);

    boolean contains(String key, int type);
    void remove(String key);

    // NBT type id 常量（与 Minecraft 的 TagTags 对应）
    int TAG_INT = 3;
    int TAG_LONG = 4;
    int TAG_STRING = 8;
    int TAG_LIST = 9;
    int TAG_COMPOUND = 10;
}
