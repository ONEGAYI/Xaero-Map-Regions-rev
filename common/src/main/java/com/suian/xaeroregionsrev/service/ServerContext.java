package com.suian.xaeroregionsrev.service;

/**
 * 服务端上下文抽象接口。
 * 各平台子项目包装 MinecraftServer/ServerLevel 实现。
 */
public interface ServerContext {
    /** 获取主世界存储 */
    RegionStore overworld();

    /** 获取指定维度的存储 */
    RegionStore getLevel(String dimensionKey);

    /** 遍历所有维度的存储 */
    Iterable<RegionStore> allLevels();
}
