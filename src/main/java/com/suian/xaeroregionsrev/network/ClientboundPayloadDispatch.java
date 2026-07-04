package com.suian.xaeroregionsrev.network;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.network.payload.ColorHistorySyncPacket;
import com.suian.xaeroregionsrev.network.payload.RegionEditResultPacket;
import com.suian.xaeroregionsrev.network.payload.RegionSyncPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

final class ClientboundPayloadDispatch {
    private static final String CLIENT_BRIDGE_CLASS = "com.suian.xaeroregionsrev.client.ClientboundPayloadBridge";

    private ClientboundPayloadDispatch() {
    }

    static void handleRegionSync(RegionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                invokeClientBridge("handleRegionSync", RegionSyncPacket.class, packet);
            }
        });
    }

    static void handleColorHistorySync(ColorHistorySyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                invokeClientBridge("handleColorHistorySync", ColorHistorySyncPacket.class, packet);
            }
        });
    }

    static void handleRegionEditResult(RegionEditResultPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                invokeClientBridge("handleRegionEditResult", RegionEditResultPacket.class, packet);
            }
        });
    }

    private static void invokeClientBridge(String methodName, Class<?> packetType, Object packet) {
        try {
            Class<?> bridgeClass = Class.forName(CLIENT_BRIDGE_CLASS);
            bridgeClass.getMethod(methodName, packetType).invoke(null, packet);
        } catch (ReflectiveOperationException exception) {
            XaeroRegionsRev.LOGGER.error("Failed to dispatch clientbound region payload to client bridge.", exception);
        }
    }
}
