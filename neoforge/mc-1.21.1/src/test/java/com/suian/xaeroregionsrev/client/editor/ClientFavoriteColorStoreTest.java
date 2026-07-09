package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.ArgbColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientFavoriteColorStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void missingFileLoadsDefaultFavorites() {
        ClientFavoriteColorStore store = new ClientFavoriteColorStore(tempDir.resolve("favourite.json"));

        assertEquals(ClientFavoriteColorStore.DEFAULT_COLORS, store.colors());
    }

    @Test
    void remembersAndReloadsFavoritesFromJson() {
        Path file = tempDir.resolve("xaero_map_region_rev").resolve("favourite.json");
        ClientFavoriteColorStore store = new ClientFavoriteColorStore(file);

        store.remember(new ArgbColor(0xFF112233));
        store.remember(new ArgbColor(0x80445566));

        ClientFavoriteColorStore reloaded = new ClientFavoriteColorStore(file);

        assertEquals(List.of(
                new ArgbColor(0x80445566),
                new ArgbColor(0xFF112233),
                new ArgbColor(0x66E53E3E),
                new ArgbColor(0x6638A169),
                new ArgbColor(0x663182CE),
                new ArgbColor(0x66D69E2E),
                new ArgbColor(0x66805AD5),
                new ArgbColor(0xFFFFFFFF)
        ), reloaded.colors());
    }

    @Test
    void releasePersistsCompactedFavorites() {
        Path file = tempDir.resolve("favourite.json");
        ClientFavoriteColorStore store = new ClientFavoriteColorStore(file);

        store.release(new ArgbColor(0x663182CE));

        ClientFavoriteColorStore reloaded = new ClientFavoriteColorStore(file);

        assertEquals(List.of(
                new ArgbColor(0x66E53E3E),
                new ArgbColor(0x6638A169),
                new ArgbColor(0x66D69E2E),
                new ArgbColor(0x66805AD5),
                new ArgbColor(0xFFFFFFFF)
        ), reloaded.colors());
    }
}
