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
    private static final long CALIBRATION_INTERVAL_MS = 1000L;
    private static final String XAERO_GUI_MAP_CLASS = "xaero.map.gui.GuiMap";
    private MapCalibration calibration = MapCalibration.NONE;
    private long lastCalibrationAtMs = Long.MIN_VALUE;

    public Vector2f project(Screen screen, RegionPoint point) {
        Optional<MapViewport> viewport = readXaeroViewport(screen);
        if (viewport.isPresent()) {
            return projectInViewport(point, viewport.get().withCalibration(calibration));
        }
        return projectWithPlayerFallback(screen, point);
    }

    public RegionPoint unproject(Screen screen, double screenX, double screenY) {
        Optional<MapViewport> viewport = readXaeroViewport(screen);
        if (viewport.isPresent()) {
            return unprojectInViewport(screenX, screenY, viewport.get().withCalibration(calibration));
        }
        return unprojectWithPlayerFallback(screen, screenX, screenY);
    }

    public void calibrate(Screen screen, double mouseX, double mouseY, long nowMs) {
        if (!isCalibrationDue(nowMs, lastCalibrationAtMs)) {
            return;
        }
        Optional<MapViewport> viewport = readXaeroViewport(screen);
        Optional<RegionPoint> xaeroMousePoint = readXaeroMousePoint(screen);
        if (viewport.isEmpty() || xaeroMousePoint.isEmpty()) {
            return;
        }
        calibration = calibrateViewport(viewport.get(), mouseX, mouseY, xaeroMousePoint.get()).calibration();
        lastCalibrationAtMs = nowMs;
    }

    static boolean isCalibrationDue(long nowMs, long lastCalibrationAtMs) {
        return lastCalibrationAtMs == Long.MIN_VALUE || nowMs - lastCalibrationAtMs >= CALIBRATION_INTERVAL_MS;
    }

    public static Vector2f projectInViewport(RegionPoint point, MapViewport viewport) {
        double mapX = point.x() / viewport.coordinateDivisor() - viewport.calibration().mapXOffset();
        double mapZ = point.z() / viewport.coordinateDivisor() - viewport.calibration().mapZOffset();
        float x = viewport.centerX() + (float) ((mapX - viewport.cameraX()) * viewport.guiPixelsPerBlock());
        float y = viewport.centerY() + (float) ((mapZ - viewport.cameraZ()) * viewport.guiPixelsPerBlock());
        return new Vector2f(x, y);
    }

    public static RegionPoint unprojectInViewport(double screenX, double screenY, MapViewport viewport) {
        double mapX = viewport.cameraX()
                + (screenX - viewport.centerX()) / viewport.guiPixelsPerBlock()
                + viewport.calibration().mapXOffset();
        double mapZ = viewport.cameraZ()
                + (screenY - viewport.centerY()) / viewport.guiPixelsPerBlock()
                + viewport.calibration().mapZOffset();
        double worldX = mapX * viewport.coordinateDivisor();
        double worldZ = mapZ * viewport.coordinateDivisor();
        return new RegionPoint((int) Math.floor(worldX), (int) Math.floor(worldZ));
    }

    public static MapViewport calibrateViewport(MapViewport viewport, double screenX, double screenY,
                                                RegionPoint xaeroWorldPoint) {
        double rawMapX = viewport.cameraX() + (screenX - viewport.centerX()) / viewport.guiPixelsPerBlock();
        double rawMapZ = viewport.cameraZ() + (screenY - viewport.centerY()) / viewport.guiPixelsPerBlock();
        double xaeroMapX = xaeroWorldPoint.x() / viewport.coordinateDivisor();
        double xaeroMapZ = xaeroWorldPoint.z() / viewport.coordinateDivisor();
        return viewport.withCalibration(new MapCalibration(xaeroMapX - rawMapX, xaeroMapZ - rawMapZ));
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

    private static Optional<Integer> readInt(Object owner, String fieldName) {
        try {
            Field field = findField(owner.getClass(), fieldName);
            field.setAccessible(true);
            return Optional.of(field.getInt(owner));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<RegionPoint> readXaeroMousePoint(Screen screen) {
        if (!XAERO_GUI_MAP_CLASS.equals(screen.getClass().getName())) {
            return Optional.empty();
        }
        return readInt(screen, "mouseBlockPosX")
                .flatMap(x -> readInt(screen, "mouseBlockPosZ")
                        .map(z -> new RegionPoint(x, z)));
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
                              float physicalPixelsPerBlock, double coordinateDivisor, double screenScale,
                              MapCalibration calibration) {
        public MapViewport(double cameraX, double cameraZ, float centerX, float centerY, float physicalPixelsPerBlock,
                           double coordinateDivisor, double screenScale) {
            this(cameraX, cameraZ, centerX, centerY, physicalPixelsPerBlock, coordinateDivisor, screenScale,
                    MapCalibration.NONE);
        }

        public MapViewport(double cameraX, double cameraZ, float centerX, float centerY, float physicalPixelsPerBlock,
                           double coordinateDivisor) {
            this(cameraX, cameraZ, centerX, centerY, physicalPixelsPerBlock, coordinateDivisor,
                    DEFAULT_SCREEN_SCALE, MapCalibration.NONE);
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
            if (calibration == null) {
                calibration = MapCalibration.NONE;
            }
        }

        public float guiPixelsPerBlock() {
            return (float) (physicalPixelsPerBlock / screenScale);
        }

        public MapViewport withCalibration(MapCalibration calibration) {
            return new MapViewport(cameraX, cameraZ, centerX, centerY, physicalPixelsPerBlock, coordinateDivisor,
                    screenScale, calibration);
        }
    }

    public record MapCalibration(double mapXOffset, double mapZOffset) {
        public static final MapCalibration NONE = new MapCalibration(0.0D, 0.0D);

        public MapCalibration {
            if (!Double.isFinite(mapXOffset)) {
                mapXOffset = 0.0D;
            }
            if (!Double.isFinite(mapZOffset)) {
                mapZOffset = 0.0D;
            }
        }
    }
}
