package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.ColorPaletteLimits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ColorHistorySyncPacket(List<ArgbColor> colors) implements CustomPacketPayload {
    public static final Type<ColorHistorySyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "color_history_sync")
    );
    public static final StreamCodec<FriendlyByteBuf, ColorHistorySyncPacket> STREAM_CODEC = PacketCodecs.of(
            ColorHistorySyncPacket::encode,
            ColorHistorySyncPacket::decode
    );
    public static final int MAX_COLORS = ColorPaletteLimits.MAX_COLORS;

    public ColorHistorySyncPacket {
        colors = List.copyOf(colors);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ColorHistorySyncPacket packet, FriendlyByteBuf buffer) {
        validateColorCount(packet.colors.size());
        buffer.writeVarInt(packet.colors.size());
        for (ArgbColor color : packet.colors) {
            buffer.writeInt(color.value());
        }
    }

    public static ColorHistorySyncPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        validateColorCount(count);
        List<ArgbColor> colors = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            colors.add(new ArgbColor(buffer.readInt()));
        }
        return new ColorHistorySyncPacket(colors);
    }

    private static void validateColorCount(int count) {
        if (count < 0 || count > MAX_COLORS) {
            throw new IllegalArgumentException("Color history count must be between 0 and " + MAX_COLORS + ".");
        }
    }
}
