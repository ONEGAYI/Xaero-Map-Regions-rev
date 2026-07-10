package com.suian.xaeroregionsrev.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 客户端本地配置存储，负责持久化与自动校准无关的个人偏好项。
 *
 * <p>配置文件位于 {@code config/xaero_map_region_rev/client.json}，采用懒加载 +
 * 失败回退默认值 + 立即写盘的模式（与 {@code ClientFavoriteColorStore} 一致）。
 * {@link #defaults()} 返回的实例永不落盘，用于无 Minecraft 环境的单元测试。
 */
public final class ClientLocalConfig {
    public static final boolean DEFAULT_AUTO_CALIBRATE_ENABLED = false;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile ClientLocalConfig shared;

    private final Path file;
    private boolean autoCalibrateEnabled = DEFAULT_AUTO_CALIBRATE_ENABLED;
    private boolean loaded = false;

    private ClientLocalConfig(Path file) {
        this.file = file;
    }

    /**
     * 运行时单例，配置路径固定为 {@code config/xaero_map_region_rev/client.json}。
     * 在客户端主线程首次访问时延迟初始化。
     */
    public static ClientLocalConfig shared() {
        ClientLocalConfig local = shared;
        if (local != null) {
            return local;
        }
        synchronized (ClientLocalConfig.class) {
            local = shared;
            if (local == null) {
                shared = local = forPath(createDefaultPath());
            }
            return local;
        }
    }

    /**
     * 注入配置文件路径的可测试入口，不依赖 {@link FMLPaths}。
     */
    public static ClientLocalConfig forPath(Path file) {
        return new ClientLocalConfig(file);
    }

    /**
     * 不落盘的默认实例，仅用于单元测试。{@code file} 为 {@code null} 时
     * {@link #setAutoCalibrateEnabled(boolean)} 的修改不会被任何后续实例看见。
     */
    public static ClientLocalConfig defaults() {
        return new ClientLocalConfig(null);
    }

    private static Path createDefaultPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve("xaero_map_region_rev")
                .resolve("client.json");
    }

    public boolean isAutoCalibrateEnabled() {
        ensureLoaded();
        return autoCalibrateEnabled;
    }

    public void setAutoCalibrateEnabled(boolean value) {
        ensureLoaded();
        autoCalibrateEnabled = value;
        save();
    }

    private void ensureLoaded() {
        if (loaded || file == null) {
            return;
        }
        loaded = true;
        if (!Files.isRegularFile(file)) {
            autoCalibrateEnabled = DEFAULT_AUTO_CALIBRATE_ENABLED;
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            ClientConfigDocument document = GSON.fromJson(reader, ClientConfigDocument.class);
            autoCalibrateEnabled = document != null && document.autoCalibrate != null
                    ? document.autoCalibrate
                    : DEFAULT_AUTO_CALIBRATE_ENABLED;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Failed to load client config from {}. Using defaults.", file, exception);
            autoCalibrateEnabled = DEFAULT_AUTO_CALIBRATE_ENABLED;
        }
    }

    private void save() {
        if (file == null) {
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ClientConfigDocument document = new ClientConfigDocument();
            document.autoCalibrate = autoCalibrateEnabled;
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(document, writer);
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to save client config to {}.", file, exception);
        }
    }

    private static final class ClientConfigDocument {
        private Boolean autoCalibrate;
    }
}
