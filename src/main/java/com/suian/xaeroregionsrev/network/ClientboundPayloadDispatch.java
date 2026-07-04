package com.suian.xaeroregionsrev.network;

import com.suian.xaeroregionsrev.network.payload.ColorHistorySyncPacket;
import com.suian.xaeroregionsrev.network.payload.RegionEditResultPacket;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

final class ClientboundPayloadDispatch {
    private ClientboundPayloadDispatch() {
    }

    static void handleRegionSync(RegionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.suian.xaeroregionsrev.client.ClientRegionCache.replaceAll(packet.regions());
            }
        });
    }

    static void handleColorHistorySync(ColorHistorySyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.suian.xaeroregionsrev.client.ClientColorHistoryCache.replaceAll(packet.colors());
            }
        });
    }

    static void handleRegionEditResult(RegionEditResultPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.suian.xaeroregionsrev.client.ClientRegionEditResultHandler.handle(packet);
            }
        });
    }
}
