package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.client.EditResultHandler;
import com.suian.xaeroregionsrev.network.payload.RegionEditResultPacket;
import net.minecraft.client.Minecraft;

/**
 * 客户端 {@link EditResultHandler} 的 Forge 实现。
 * 检查当前屏幕是否为 {@link RegionStyleEditScreen} 并转发编辑结果。
 */
public final class RegionStyleEditResultHandler implements EditResultHandler {
    @Override
    public void handleEditResult(long requestId, boolean success, boolean closeScreen, String message) {
        if (Minecraft.getInstance().screen instanceof RegionStyleEditScreen screen) {
            screen.handleEditResult(new RegionEditResultPacket(requestId, success, closeScreen, message));
        }
    }
}
