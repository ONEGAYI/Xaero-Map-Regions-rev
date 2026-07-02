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
        float x = centerX + (float) (point.x() - playerX) * DEFAULT_PIXELS_PER_BLOCK;
        float y = centerY + (float) (point.z() - playerZ) * DEFAULT_PIXELS_PER_BLOCK;
        return new Vector2f(x, y);
    }
}
