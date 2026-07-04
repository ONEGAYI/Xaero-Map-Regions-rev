package com.suian.xaeroregionsrev.client.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.ColorPaletteLimits;
import com.suian.xaeroregionsrev.region.RegionColorParser;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ClientFavoriteColorStore {
    public static final List<ArgbColor> DEFAULT_COLORS = List.of(
            new ArgbColor(0x66E53E3E),
            new ArgbColor(0x6638A169),
            new ArgbColor(0x663182CE),
            new ArgbColor(0x66D69E2E),
            new ArgbColor(0x66805AD5),
            new ArgbColor(0xFFFFFFFF)
    );
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private List<ArgbColor> colors;

    public ClientFavoriteColorStore(Path file) {
        this.file = file;
    }

    public static ClientFavoriteColorStore createDefault() {
        return new ClientFavoriteColorStore(FMLPaths.CONFIGDIR.get()
                .resolve("xaero_map_region_rev")
                .resolve("favourite.json"));
    }

    public List<ArgbColor> colors() {
        ensureLoaded();
        return colors;
    }

    public void remember(ArgbColor color) {
        ensureLoaded();
        colors = ColorPickerModel.rememberColor(colors, color, ColorPaletteLimits.MAX_COLORS);
        save();
    }

    public void release(ArgbColor color) {
        ensureLoaded();
        colors = ColorPickerModel.releaseColor(colors, color);
        save();
    }

    private void ensureLoaded() {
        if (colors != null) {
            return;
        }
        if (!Files.isRegularFile(file)) {
            colors = DEFAULT_COLORS;
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            FavoriteColorsDocument document = GSON.fromJson(reader, FavoriteColorsDocument.class);
            colors = decode(document);
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            LOGGER.warn("Failed to load favorite colors from {}. Using defaults.", file, exception);
            colors = DEFAULT_COLORS;
        }
    }

    private List<ArgbColor> decode(FavoriteColorsDocument document) {
        if (document == null || document.colors == null) {
            return DEFAULT_COLORS;
        }
        List<ArgbColor> decoded = new ArrayList<>();
        for (String colorText : document.colors) {
            if (decoded.size() >= ColorPaletteLimits.MAX_COLORS) {
                break;
            }
            decoded.add(RegionColorParser.parse(colorText));
        }
        return List.copyOf(decoded);
    }

    private void save() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            FavoriteColorsDocument document = new FavoriteColorsDocument();
            document.colors = colors.stream()
                    .map(RegionColorParser::format)
                    .toList();
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(document, writer);
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to save favorite colors to {}.", file, exception);
        }
    }

    private static final class FavoriteColorsDocument {
        private List<String> colors = List.of();
    }
}
