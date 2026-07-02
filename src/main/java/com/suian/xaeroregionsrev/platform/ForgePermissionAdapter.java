package com.suian.xaeroregionsrev.platform;

import com.suian.xaeroregionsrev.region.PermissionProfile;
import net.minecraft.server.level.ServerPlayer;

public final class ForgePermissionAdapter {
    private ForgePermissionAdapter() {
    }

    public static PermissionProfile from(ServerPlayer player) {
        boolean operator = player.hasPermissions(2);
        boolean creative = player.gameMode.isCreative();
        return new PermissionProfile(operator, creative);
    }
}
