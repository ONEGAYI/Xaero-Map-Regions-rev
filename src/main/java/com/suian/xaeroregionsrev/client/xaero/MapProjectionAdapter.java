package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Vector2f;

import java.lang.reflect.Field;
import java.util.Optional;

public final class MapProjectionAdapter {
    private static final float DEFAULT_PIXELS_PER_BLOCK = 0.25F;
    private static final double DEFAULT_COORDINATE_DIVISOR = 1.0D;
    private static final double DEFAULT_SCREEN_SCALE = 1.0D;
    private static final String XAERO_GUI_MAP_CLASS = "xaero.map.gui.GuiMap";

    public Vector2f project(Screen screen, RegionPoint point) {
        Optional<MapViewport> viewport = readXaeroViewport(screen);
        if (viewport.isPresent()) {
            return projectInViewport(point, viewport.get());
        }
        return projectWithPlayerFallback(screen, point);
    }

    public RegionPoint unproject(Screen screen, double screenX, double screenY) {
        Optional<MapViewport> viewport = readXaeroViewport(screen);
        if (viewport.isPresent()) {
            return unprojectInViewport(screenX, screenY, viewport.get());
        }
        return unprojectWithPlayerFallback(screen, screenX, screenY);
    }

    public static Vector2f projectInViewport(RegionPoint point, MapViewport viewport) {
        double mapX = point.x() / viewport.coordinateDivisor();
        double mapZ = point.z() / viewport.coordinateDivisor();
        float x = viewport.centerX() + (float) ((mapX - viewport.cameraX()) * viewport.guiPixelsPerBlock());
        float y = viewport.centerY() + (float) ((mapZ - viewport.cameraZ()) * viewport.guiPixelsPerBlock());
        return new Vector2f(x, y);
    }

    public static RegionPoint unprojectInViewport(double screenX, double screenY, MapViewport viewport) {
        double mapX = viewport.cameraX() + (screenX - viewport.centerX()) / viewport.guiPixelsPerBlock();
        double mapZ = viewport.cameraZ() + (screenY - viewport.centerY()) / viewport.guiPixelsPerBlock();
        double worldX = mapX * viewport.coordinateDivisor();
        double worldZ = mapZ * viewport.coordinateDivisor();
        return new RegionPoint((int) Math.floor(worldX), (int) Math.floor(worldZ));
    }

    private static Optional<MapViewport> readXaeroViewport(Screen screen) {
        if (!XAERO_GUI_MAP_CLASS.equals(screen.getClass().getName())) {
            return Optional.empty();
        }
        return readDouble(screen, "cameraX")
                .flatMap(cameraX -> readDouble(screen, "cameraZ")
                        .flatMap(cameraZ -> readDouble(screen, "scale")
                                .map(scale -> {
                                    double coordinateDivisor = readDouble(screen, "prevPlayerDimDiv")
                                            .filter(value -> value > 0.0D)
                                            .orElse(DEFAULT_COORDINATE_DIVISOR);
                                    double screenScale = readDouble(screen, "screenScale")
                                            .filter(value -> value > 0.0D)
                                            .orElseGet(MapProjectionAdapter::minecraftGuiScale);
                                    return new MapViewport(
                                            cameraX,
                                            cameraZ,
                                            screen.width / 2.0F,
                                            screen.height / 2.0F,
                                            scale.floatValue(),
                                            coordinateDivisor,
                                            screenScale
                                    );
                                })));
    }

    private static double minecraftGuiScale() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return DEFAULT_SCREEN_SCALE;
        }
        return minecraft.getWindow().getGuiScale();
    }

    private static Optional<Double> readDouble(Object owner, String fieldName) {
        try {
            Field field = findField(owner.getClass(), fieldName);
            field.setAccessible(true);
            return Optional.of(field.getDouble(owner));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static Vector2f projectWithPlayerFallback(Screen screen, RegionPoint point) {
        Minecraft minecraft = Minecraft.getInstance();
        double playerX = minecraft.player == null ? 0.0D : minecraft.player.getX();
        double playerZ = minecraft.player == null ? 0.0D : minecraft.player.getZ();
        float centerX = screen.width / 2.0F;
        float centerY = screen.height / 2.0F;
        return projectRelative(point.x(), point.z(), playerX, playerZ, centerX, centerY, DEFAULT_PIXELS_PER_BLOCK);
    }

    private static RegionPoint unprojectWithPlayerFallback(Screen screen, double screenX, double screenY) {
        Minecraft minecraft = Minecraft.getInstance();
        double playerX = minecraft.player == null ? 0.0D : minecraft.player.getX();
        double playerZ = minecraft.player == null ? 0.0D : minecraft.player.getZ();
        float centerX = screen.width / 2.0F;
        float centerY = screen.height / 2.0F;
        return unprojectRelative(screenX, screenY, playerX, playerZ, centerX, centerY, DEFAULT_PIXELS_PER_BLOCK);
    }

    public static Vector2f projectRelative(int blockX, int blockZ, double playerX, double playerZ,
                                           float centerX, float centerY, float pixelsPerBlock) {
        float x = centerX + (float) (blockX - playerX) * pixelsPerBlock;
        float y = centerY + (float) (blockZ - playerZ) * pixelsPerBlock;
        return new Vector2f(x, y);
    }

    public static RegionPoint unprojectRelative(double screenX, double screenY, double playerX, double playerZ,
                                                float centerX, float centerY, float pixelsPerBlock) {
        double worldX = playerX + (screenX - centerX) / pixelsPerBlock;
        double worldZ = playerZ + (screenY - centerY) / pixelsPerBlock;
        return new RegionPoint((int) Math.floor(worldX), (int) Math.floor(worldZ));
    }

    public record MapViewport(double cameraX, double cameraZ, float centerX, float centerY,
                              float physicalPixelsPerBlock, double coordinateDivisor, double screenScale) {
        public MapViewport(double cameraX, double cameraZ, float centerX, float centerY, float physicalPixelsPerBlock,
                           double coordinateDivisor) {
            this(cameraX, cameraZ, centerX, centerY, physicalPixelsPerBlock, coordinateDivisor,
                    DEFAULT_SCREEN_SCALE);
        }

        public MapViewport {
            if (!Float.isFinite(physicalPixelsPerBlock) || physicalPixelsPerBlock <= 0.0F) {
                physicalPixelsPerBlock = DEFAULT_PIXELS_PER_BLOCK;
            }
            if (!Double.isFinite(coordinateDivisor) || coordinateDivisor <= 0.0D) {
                coordinateDivisor = DEFAULT_COORDINATE_DIVISOR;
            }
            if (!Double.isFinite(screenScale) || screenScale <= 0.0D) {
                screenScale = DEFAULT_SCREEN_SCALE;
            }
        }

        public float guiPixelsPerBlock() {
            return (float) (physicalPixelsPerBlock / screenScale);
        }
    }
}
