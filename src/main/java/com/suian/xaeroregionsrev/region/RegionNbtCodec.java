package com.suian.xaeroregionsrev.region;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RegionNbtCodec {
    private static final int TAG_INT = 3;
    private static final int TAG_LONG = 4;
    private static final int TAG_STRING = 8;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;

    private RegionNbtCodec() {
    }

    public static CompoundTag writeRegion(Region region) {
        Objects.requireNonNull(region, "Region cannot be null.");

        CompoundTag tag = new CompoundTag();
        tag.putString("id", region.id().value());
        tag.putString("name", region.name());
        tag.putString("dimension", region.dimension());
        tag.putInt("color", region.color().value());
        tag.putString("label", region.label());
        tag.putInt("labelColor", region.labelColor().value());
        tag.putString("category", region.category());
        tag.putString("iconName", region.iconName());
        tag.putLong("createdAt", region.createdAt());
        tag.putLong("updatedAt", region.updatedAt());

        ListTag points = new ListTag();
        for (RegionPoint point : region.points()) {
            CompoundTag pointTag = new CompoundTag();
            pointTag.putInt("x", point.x());
            pointTag.putInt("z", point.z());
            points.add(pointTag);
        }
        tag.put("points", points);
        return tag;
    }

    public static Region readRegion(CompoundTag tag) {
        Objects.requireNonNull(tag, "Region tag cannot be null.");
        requireField(tag, "id", TAG_STRING);
        requireField(tag, "name", TAG_STRING);
        requireField(tag, "dimension", TAG_STRING);
        requireField(tag, "color", TAG_INT);
        requireField(tag, "category", TAG_STRING);
        requireField(tag, "iconName", TAG_STRING);
        requireField(tag, "createdAt", TAG_LONG);
        requireField(tag, "updatedAt", TAG_LONG);
        requireField(tag, "points", TAG_LIST);

        List<RegionPoint> points = new ArrayList<>();
        ListTag pointTags = tag.getList("points", TAG_COMPOUND);
        if (pointTags.size() > RegionLimits.MAX_POINTS_PER_REQUEST) {
            throw new IllegalArgumentException("Region point count cannot exceed "
                    + RegionLimits.MAX_POINTS_PER_REQUEST + ".");
        }
        for (int i = 0; i < pointTags.size(); i++) {
            CompoundTag pointTag = pointTags.getCompound(i);
            requireField(pointTag, "x", TAG_INT);
            requireField(pointTag, "z", TAG_INT);
            points.add(new RegionPoint(pointTag.getInt("x"), pointTag.getInt("z")));
        }
        if (!PolygonMath.isValidPolygon(points)) {
            throw new IllegalArgumentException("Region points must form a valid polygon.");
        }

        String id = boundedString(tag, "id", RegionLimits.MAX_ID_LENGTH);
        String name = boundedString(tag, "name", RegionLimits.MAX_NAME_LENGTH);
        String dimension = boundedString(tag, "dimension", RegionLimits.MAX_DIMENSION_LENGTH);
        String label = tag.contains("label", TAG_STRING)
                ? boundedString(tag, "label", RegionLimits.MAX_LABEL_LENGTH)
                : name;
        String category = boundedString(tag, "category", RegionLimits.MAX_CATEGORY_LENGTH);
        String iconName = boundedString(tag, "iconName", RegionLimits.MAX_ICON_NAME_LENGTH);

        return new Region(
                new RegionId(id),
                name,
                dimension,
                new ArgbColor(tag.getInt("color")),
                label,
                tag.contains("labelColor", TAG_INT) ? new ArgbColor(tag.getInt("labelColor")) : new ArgbColor(0xFFFFFFFF),
                category,
                iconName,
                points,
                tag.getLong("createdAt"),
                tag.getLong("updatedAt")
        );
    }

    private static void requireField(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) {
            throw new IllegalArgumentException("Missing or invalid region NBT field: " + key);
        }
    }

    private static String boundedString(CompoundTag tag, String key, int maxBytes) {
        String value = tag.getString(key);
        if (value.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            throw new IllegalArgumentException("Saved region NBT field '" + key
                    + "' cannot exceed " + maxBytes + " UTF-8 bytes.");
        }
        return value;
    }
}
