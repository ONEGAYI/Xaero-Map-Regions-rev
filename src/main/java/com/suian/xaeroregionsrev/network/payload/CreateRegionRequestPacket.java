package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.RegionLimits;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record CreateRegionRequestPacket(
        String name,
        ArgbColor fillColor,
        String label,
        ArgbColor labelColor,
        List<RegionPoint> points
) {
    public CreateRegionRequestPacket {
        points = List.copyOf(points);
    }

    public static void encode(CreateRegionRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.name, RegionLimits.MAX_NAME_LENGTH);
        buffer.writeInt(packet.fillColor.value());
        buffer.writeUtf(packet.label, RegionLimits.MAX_LABEL_LENGTH);
        buffer.writeInt(packet.labelColor.value());
        buffer.writeVarInt(packet.points.size());
        for (RegionPoint point : packet.points) {
            buffer.writeInt(point.x());
            buffer.writeInt(point.z());
        }
    }

    public static CreateRegionRequestPacket decode(FriendlyByteBuf buffer) {
        String name = buffer.readUtf(RegionLimits.MAX_NAME_LENGTH);
        ArgbColor fillColor = new ArgbColor(buffer.readInt());
        String label = buffer.readUtf(RegionLimits.MAX_LABEL_LENGTH);
        ArgbColor labelColor = new ArgbColor(buffer.readInt());
        int pointCount = readBoundedPointCount(buffer);
        List<RegionPoint> points = new ArrayList<>(pointCount);
        for (int index = 0; index < pointCount; index++) {
            points.add(new RegionPoint(buffer.readInt(), buffer.readInt()));
        }
        return new CreateRegionRequestPacket(name, fillColor, label, labelColor, points);
    }

    private static int readBoundedPointCount(FriendlyByteBuf buffer) {
        int pointCount = buffer.readVarInt();
        if (pointCount < 0 || pointCount > RegionLimits.MAX_POINTS_PER_REQUEST) {
            throw new IllegalArgumentException("Region point count must be between 0 and "
                    + RegionLimits.MAX_POINTS_PER_REQUEST + ".");
        }
        return pointCount;
    }
}
