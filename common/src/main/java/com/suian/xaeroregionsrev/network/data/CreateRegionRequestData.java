package com.suian.xaeroregionsrev.network.data;

import com.suian.xaeroregionsrev.network.buffer.PacketBuffer;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.RegionLimits;
import com.suian.xaeroregionsrev.region.RegionPoint;

import java.util.ArrayList;
import java.util.List;

public record CreateRegionRequestData(
        long requestId,
        String name,
        ArgbColor fillColor,
        String label,
        ArgbColor labelColor,
        List<RegionPoint> points
) {
    public CreateRegionRequestData(String name, ArgbColor fillColor, String label, ArgbColor labelColor,
                                   List<RegionPoint> points) {
        this(0L, name, fillColor, label, labelColor, points);
    }

    public CreateRegionRequestData {
        points = List.copyOf(points);
    }

    public static void encode(PacketBuffer buffer, CreateRegionRequestData data) {
        buffer.writeLong(data.requestId);
        buffer.writeUtf(data.name, RegionLimits.MAX_NAME_LENGTH);
        buffer.writeInt(data.fillColor.value());
        buffer.writeUtf(data.label, RegionLimits.MAX_LABEL_LENGTH);
        buffer.writeInt(data.labelColor.value());
        buffer.writeVarInt(data.points.size());
        for (RegionPoint point : data.points) {
            buffer.writeInt(point.x());
            buffer.writeInt(point.z());
        }
    }

    public static CreateRegionRequestData decode(PacketBuffer buffer) {
        long requestId = buffer.readLong();
        String name = buffer.readUtf(RegionLimits.MAX_NAME_LENGTH);
        ArgbColor fillColor = new ArgbColor(buffer.readInt());
        String label = buffer.readUtf(RegionLimits.MAX_LABEL_LENGTH);
        ArgbColor labelColor = new ArgbColor(buffer.readInt());
        int pointCount = readBoundedPointCount(buffer);
        List<RegionPoint> points = new ArrayList<>(pointCount);
        for (int index = 0; index < pointCount; index++) {
            points.add(new RegionPoint(buffer.readInt(), buffer.readInt()));
        }
        return new CreateRegionRequestData(requestId, name, fillColor, label, labelColor, points);
    }

    private static int readBoundedPointCount(PacketBuffer buffer) {
        int pointCount = buffer.readVarInt();
        if (pointCount < 0 || pointCount > RegionLimits.MAX_POINTS_PER_REQUEST) {
            throw new IllegalArgumentException("Region point count must be between 0 and "
                    + RegionLimits.MAX_POINTS_PER_REQUEST + ".");
        }
        return pointCount;
    }
}
