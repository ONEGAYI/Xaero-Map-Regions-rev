package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.PolygonMath;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record RegionSyncPacket(List<Region> regions) {
    public static final int MAX_REGIONS = 4096;
    public static final int MAX_POINTS_PER_REGION = 1024;

    public RegionSyncPacket {
        regions = List.copyOf(regions);
    }

    public static void encode(RegionSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.regions.size());
        for (Region region : packet.regions) {
            buffer.writeUtf(region.id().value());
            buffer.writeUtf(region.name());
            buffer.writeUtf(region.dimension());
            buffer.writeInt(region.color().value());
            buffer.writeUtf(region.label());
            buffer.writeInt(region.labelColor().value());
            buffer.writeUtf(region.category());
            buffer.writeUtf(region.iconName());
            buffer.writeLong(region.createdAt());
            buffer.writeLong(region.updatedAt());
            buffer.writeVarInt(region.points().size());
            for (RegionPoint point : region.points()) {
                buffer.writeInt(point.x());
                buffer.writeInt(point.z());
            }
        }
    }

    public static RegionSyncPacket decode(FriendlyByteBuf buffer) {
        int size = readBoundedCount(buffer, MAX_REGIONS, "Region count must be between 0 and 4096.");
        List<Region> regions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            RegionId id = new RegionId(buffer.readUtf());
            String name = buffer.readUtf();
            String dimension = buffer.readUtf();
            ArgbColor color = new ArgbColor(buffer.readInt());
            String label = buffer.readUtf();
            ArgbColor labelColor = new ArgbColor(buffer.readInt());
            String category = buffer.readUtf();
            String iconName = buffer.readUtf();
            long createdAt = buffer.readLong();
            long updatedAt = buffer.readLong();
            int pointCount = readBoundedCount(buffer, MAX_POINTS_PER_REGION, "Region point count must be between 0 and 1024.");
            List<RegionPoint> points = new ArrayList<>(pointCount);
            for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
                points.add(new RegionPoint(buffer.readInt(), buffer.readInt()));
            }
            if (pointCount < 3 || !PolygonMath.isValidPolygon(points)) {
                throw new IllegalArgumentException("Region points must form a valid polygon.");
            }
            regions.add(new Region(id, name, dimension, color, label, labelColor, category, iconName, points, createdAt, updatedAt));
        }
        return new RegionSyncPacket(regions);
    }

    private static int readBoundedCount(FriendlyByteBuf buffer, int max, String message) {
        int count = buffer.readVarInt();
        if (count < 0 || count > max) {
            throw new IllegalArgumentException(message);
        }
        return count;
    }
}
