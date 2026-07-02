package com.suian.xaeroregionsrev.region;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public final class RegionNbtCodec {
    private RegionNbtCodec() {
    }

    public static CompoundTag writeRegion(Region region) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", region.id().value());
        tag.putString("name", region.name());
        tag.putString("dimension", region.dimension());
        tag.putInt("color", region.color().value());
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
        List<RegionPoint> points = new ArrayList<>();
        ListTag pointTags = tag.getList("points", 10);
        for (int i = 0; i < pointTags.size(); i++) {
            CompoundTag pointTag = pointTags.getCompound(i);
            points.add(new RegionPoint(pointTag.getInt("x"), pointTag.getInt("z")));
        }

        return new Region(
                new RegionId(tag.getString("id")),
                tag.getString("name"),
                tag.getString("dimension"),
                new ArgbColor(tag.getInt("color")),
                tag.getString("category"),
                tag.getString("iconName"),
                points,
                tag.getLong("createdAt"),
                tag.getLong("updatedAt")
        );
    }
}
