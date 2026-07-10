package com.suian.xaeroregionsrev.client;

import com.suian.xaeroregionsrev.network.payload.ColorHistorySyncPacket;
import com.suian.xaeroregionsrev.network.payload.RegionEditResultPacket;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;

public final class ClientboundPayloadBridge {
    private ClientboundPayloadBridge() {
    }

    public static void handleRegionSync(RegionSyncPacket packet) {
        ClientRegionCache.replaceAll(packet.regions());
    }

    public static void handleColorHistorySync(ColorHistorySyncPacket packet) {
        ClientColorHistoryCache.replaceAll(packet.colors());
    }

    public static void handleRegionEditResult(RegionEditResultPacket packet) {
        ClientRegionEditResultHandler.handle(
                packet.requestId(),
                packet.success(),
                packet.closeScreen(),
                packet.message()
        );
    }
}
