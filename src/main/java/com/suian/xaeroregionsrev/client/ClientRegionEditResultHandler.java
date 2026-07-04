package com.suian.xaeroregionsrev.client;

import com.suian.xaeroregionsrev.client.editor.RegionStyleEditScreen;
import com.suian.xaeroregionsrev.network.payload.RegionEditResultPacket;
import net.minecraft.client.Minecraft;

public final class ClientRegionEditResultHandler {
    private ClientRegionEditResultHandler() {
    }

    public static void handle(RegionEditResultPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof RegionStyleEditScreen screen) {
            screen.handleEditResult(packet);
        }
    }
}
