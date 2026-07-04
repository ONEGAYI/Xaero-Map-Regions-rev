package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.XaeroRegionsRev;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionLimits;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record RegionSyncPacket(List<Region> regions) implements CustomPacketPayload {
    public static final Type<RegionSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(XaeroRegionsRev.MOD_ID, "region_sync")
    );
    public static final StreamCodec<FriendlyByteBuf, RegionSyncPacket> STREAM_CODEC = PacketCodecs.of(
            RegionSyncPacket::encode,
            RegionSyncPacket::decode
    );
    public static final int MAX_REGIONS = 4096;
    public static final int MAX_POINTS_PER_REGION = RegionLimits.MAX_POINTS_PER_REQUEST;
    public static final int MAX_TOTAL_POINTS = 8192;
    private static final int MAX_ID_LENGTH = RegionLimits.MAX_NAME_LENGTH;
    private static final int MAX_DIMENSION_LENGTH = 256;
    private static final int MAX_CATEGORY_LENGTH = 80;
    private static final int MAX_ICON_NAME_LENGTH = 80;

    public RegionSyncPacket {
        regions = List.copyOf(regions);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(RegionSyncPacket packet, FriendlyByteBuf buffer) {
        validateRegionCount(packet.regions.size());
        validateTotalPoints(packet.regions);
        buffer.writeVarInt(packet.regions.size());
        for (Region region : packet.regions) {
            validatePointCount(region.points().size());
            buffer.writeUtf(region.id().value(), MAX_ID_LENGTH);
            buffer.writeUtf(region.name(), RegionLimits.MAX_NAME_LENGTH);
            buffer.writeUtf(region.dimension(), MAX_DIMENSION_LENGTH);
            buffer.writeInt(region.color().value());
            buffer.writeUtf(region.label(), RegionLimits.MAX_LABEL_LENGTH);
            buffer.writeInt(region.labelColor().value());
            buffer.writeUtf(region.category(), MAX_CATEGORY_LENGTH);
            buffer.writeUtf(region.iconName(), MAX_ICON_NAME_LENGTH);
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
        int size = readBoundedCount(buffer, MAX_REGIONS, regionCountMessage());
        List<Region> regions = new ArrayList<>(size);
        int totalPoints = 0;
        for (int i = 0; i < size; i++) {
            RegionId id = new RegionId(buffer.readUtf(MAX_ID_LENGTH));
            String name = buffer.readUtf(RegionLimits.MAX_NAME_LENGTH);
            String dimension = buffer.readUtf(MAX_DIMENSION_LENGTH);
            ArgbColor color = new ArgbColor(buffer.readInt());
            String label = buffer.readUtf(RegionLimits.MAX_LABEL_LENGTH);
            ArgbColor labelColor = new ArgbColor(buffer.readInt());
            String category = buffer.readUtf(MAX_CATEGORY_LENGTH);
            String iconName = buffer.readUtf(MAX_ICON_NAME_LENGTH);
            long createdAt = buffer.readLong();
            long updatedAt = buffer.readLong();
            int pointCount = readBoundedCount(buffer, MAX_POINTS_PER_REGION, pointCountMessage());
            if (pointCount < 3) {
                throw new IllegalArgumentException("Region points must form a valid polygon.");
            }
            totalPoints += pointCount;
            if (totalPoints > MAX_TOTAL_POINTS) {
                throw new IllegalArgumentException("Total synced region point count cannot exceed " + MAX_TOTAL_POINTS + ".");
            }
            List<RegionPoint> points = new ArrayList<>(pointCount);
            for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
                points.add(new RegionPoint(buffer.readInt(), buffer.readInt()));
            }
            regions.add(new Region(id, name, dimension, color, label, labelColor, category, iconName, points, createdAt, updatedAt));
        }
        return new RegionSyncPacket(regions);
    }

    private static void validateRegionCount(int count) {
        if (count < 0 || count > MAX_REGIONS) {
            throw new IllegalArgumentException(regionCountMessage());
        }
    }

    private static void validatePointCount(int count) {
        if (count < 3 || count > MAX_POINTS_PER_REGION) {
            throw new IllegalArgumentException(pointCountMessage());
        }
    }

    private static void validateTotalPoints(List<Region> regions) {
        int totalPoints = 0;
        for (Region region : regions) {
            totalPoints += region.points().size();
            if (totalPoints > MAX_TOTAL_POINTS) {
                throw new IllegalArgumentException("Total synced region point count cannot exceed " + MAX_TOTAL_POINTS + ".");
            }
        }
    }

    private static int readBoundedCount(FriendlyByteBuf buffer, int max, String message) {
        int count = buffer.readVarInt();
        if (count < 0 || count > max) {
            throw new IllegalArgumentException(message);
        }
        return count;
    }

    private static String regionCountMessage() {
        return "Region count must be between 0 and " + MAX_REGIONS + ".";
    }

    private static String pointCountMessage() {
        return "Region point count must be between 0 and " + MAX_POINTS_PER_REGION + ".";
    }
}
