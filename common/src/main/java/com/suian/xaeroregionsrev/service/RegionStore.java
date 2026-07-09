package com.suian.xaeroregionsrev.service;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 区域持久化存储抽象接口。
 * 各平台子项目通过适配 RegionSavedData 实现。
 */
public interface RegionStore {
    Collection<Region> allRegions();
    Optional<Region> find(RegionId id);
    void put(Region region);
    boolean remove(RegionId id);
    List<ArgbColor> colorHistory();
    void rememberColor(ArgbColor color, int limit);
}
