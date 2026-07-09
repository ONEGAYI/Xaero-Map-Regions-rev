package com.suian.xaeroregionsrev.network.data;

import com.suian.xaeroregionsrev.network.buffer.PacketBuffer;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.ColorPaletteLimits;

import java.util.ArrayList;
import java.util.List;

public record ColorHistorySyncData(List<ArgbColor> colors) {
    public static final int MAX_COLORS = ColorPaletteLimits.MAX_COLORS;

    public ColorHistorySyncData {
        colors = List.copyOf(colors);
    }

    public static void encode(PacketBuffer buffer, ColorHistorySyncData data) {
        validateColorCount(data.colors.size());
        buffer.writeVarInt(data.colors.size());
        for (ArgbColor color : data.colors) {
            buffer.writeInt(color.value());
        }
    }

    public static ColorHistorySyncData decode(PacketBuffer buffer) {
        int count = buffer.readVarInt();
        validateColorCount(count);
        List<ArgbColor> colors = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            colors.add(new ArgbColor(buffer.readInt()));
        }
        return new ColorHistorySyncData(colors);
    }

    private static void validateColorCount(int count) {
        if (count < 0 || count > MAX_COLORS) {
            throw new IllegalArgumentException("Color history count must be between 0 and " + MAX_COLORS + ".");
        }
    }
}
