package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Vector2f;

public final class MapProjectionAdapter {
    private static final float DEFAULT_PIXELS_PER_BLOCK = 0.25F;

    public Vector2f project(Screen screen, RegionPoint point) {
        Minecraft minecraft = Minecraft.getInstance();
        double playerX = minecraft.player == null ? 0.0D : minecraft.player.getX();
        double playerZ = minecraft.player == null ? 0.0D : minecraft.player.getZ();
        float centerX = screen.width / 2.0F;
        float centerY = screen.height / 2.0F;
        return projectRelative(point.x(), point.z(), playerX, playerZ, centerX, centerY, DEFAULT_PIXELS_PER_BLOCK);
    }

    public RegionPoint unproject(Screen screen, double screenX, double screenY) {
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
}
