package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record RegionSyncPacket(List<Region> regions) {
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
        int size = buffer.readVarInt();
        List<Region> regions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            RegionId id = new RegionId(buffer.readUtf());
            String name = buffer.readUtf();
            String dimension = buffer.readUtf();
            ArgbColor color = new ArgbColor(buffer.readInt());
            String category = buffer.readUtf();
            String iconName = buffer.readUtf();
            long createdAt = buffer.readLong();
            long updatedAt = buffer.readLong();
            int pointCount = buffer.readVarInt();
            List<RegionPoint> points = new ArrayList<>(pointCount);
            for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
                points.add(new RegionPoint(buffer.readInt(), buffer.readInt()));
            }
            regions.add(new Region(id, name, dimension, color, category, iconName, points, createdAt, updatedAt));
        }
        return new RegionSyncPacket(regions);
    }
}
