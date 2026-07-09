package com.suian.xaeroregionsrev.platform;

import com.suian.xaeroregionsrev.service.RegionStore;
import com.suian.xaeroregionsrev.service.ServerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link ServerContext} 适配实现，包装 {@link MinecraftServer}
 * 并按维度键解析 {@link RegionSavedDataStore}。
 */
public final class NeoForgeServerContext implements ServerContext {
    private final MinecraftServer server;

    public NeoForgeServerContext(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    @Override
    public RegionStore overworld() {
        return getLevel(Level.OVERWORLD.location().toString());
    }

    @Override
    public RegionStore getLevel(String dimensionKey) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimensionKey)) {
                return RegionSavedDataStore.of(level);
            }
        }
        return null;
    }

    @Override
    public Iterable<RegionStore> allLevels() {
        List<RegionStore> stores = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            stores.add(RegionSavedDataStore.of(level));
        }
        return stores;
    }
}
