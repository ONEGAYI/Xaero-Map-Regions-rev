package com.suian.xaeroregionsrev.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientLocalConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultsToAutoCalibrateDisabled() {
        assertFalse(ClientLocalConfig.defaults().isAutoCalibrateEnabled());
    }

    @Test
    void missingFileLoadsAutoCalibrateDisabled() {
        ClientLocalConfig config = ClientLocalConfig.forPath(tempDir.resolve("client.json"));

        assertFalse(config.isAutoCalibrateEnabled());
    }

    @Test
    void enablingAutoCalibratePersistsAcrossReloads() {
        Path file = tempDir.resolve("xaero_map_region_rev").resolve("client.json");
        ClientLocalConfig.forPath(file).setAutoCalibrateEnabled(true);

        ClientLocalConfig reloaded = ClientLocalConfig.forPath(file);

        assertTrue(reloaded.isAutoCalibrateEnabled());
    }

    @Test
    void disablingAutoCalibratePersistsAcrossReloads() {
        Path file = tempDir.resolve("client.json");
        ClientLocalConfig config = ClientLocalConfig.forPath(file);
        config.setAutoCalibrateEnabled(true);
        config.setAutoCalibrateEnabled(false);

        ClientLocalConfig reloaded = ClientLocalConfig.forPath(file);

        assertFalse(reloaded.isAutoCalibrateEnabled());
    }

    @Test
    void saveWritesAutoCalibrateFieldToDisk() throws Exception {
        Path file = tempDir.resolve("client.json");
        ClientLocalConfig.forPath(file).setAutoCalibrateEnabled(true);

        assertTrue(Files.isRegularFile(file));
        assertEquals(true, readAutoCalibrateFromFile(file));
    }

    @Test
    void corruptJsonFallsBackToDisabledWithoutThrowing() throws Exception {
        Path file = tempDir.resolve("client.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{ this is not valid json");

        ClientLocalConfig config = ClientLocalConfig.forPath(file);

        assertFalse(config.isAutoCalibrateEnabled());
    }

    @Test
    void missingAutoCalibrateFieldFallsBackToDisabled() throws Exception {
        Path file = tempDir.resolve("client.json");
        Files.createDirectories(file.getParent());
        JsonObject document = new JsonObject();
        document.add("unrelated", new JsonPrimitive(42));
        Files.writeString(file, document.toString());

        ClientLocalConfig config = ClientLocalConfig.forPath(file);

        assertFalse(config.isAutoCalibrateEnabled());
    }

    @Test
    void explicitlyTrueInFileIsLoaded() throws Exception {
        Path file = tempDir.resolve("client.json");
        Files.createDirectories(file.getParent());
        JsonObject document = new JsonObject();
        document.add("autoCalibrate", new JsonPrimitive(true));
        Files.writeString(file, document.toString());

        ClientLocalConfig config = ClientLocalConfig.forPath(file);

        assertTrue(config.isAutoCalibrateEnabled());
    }

    @Test
    void defaultsDoesNotPersistAcrossInstances() {
        ClientLocalConfig first = ClientLocalConfig.defaults();
        first.setAutoCalibrateEnabled(true);

        assertFalse(ClientLocalConfig.defaults().isAutoCalibrateEnabled());
    }

    private boolean readAutoCalibrateFromFile(Path file) throws Exception {
        String content = Files.readString(file);
        JsonObject document = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
        return document.get("autoCalibrate").getAsBoolean();
    }
}
