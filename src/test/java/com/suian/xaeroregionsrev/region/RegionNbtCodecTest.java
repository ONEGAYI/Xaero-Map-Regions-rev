package com.suian.xaeroregionsrev.region;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegionNbtCodecTest {
    @Test
    void roundTripsRegionThroughNbt() {
        var original = new Region(
                new RegionId("spawn"),
                "Spawn",
                "minecraft:overworld",
                new ArgbColor(0x8800FF00),
                "Spawn Label",
                new ArgbColor(0xFFFFAA00),
                "town",
                "home",
                List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                100L,
                200L
        );

        var tag = RegionNbtCodec.writeRegion(original);
        var decoded = RegionNbtCodec.readRegion(tag);

        assertEquals(original, decoded);
    }

    @Test
    void writeRegionIncludesLabelAndLabelColor() {
        var tag = validRegionTag();

        assertEquals("Spawn Label", tag.getString("label"));
        assertEquals(0xFFFFAA00, tag.getInt("labelColor"));
    }

    @Test
    void readRegionDefaultsMissingLabelToNameForLegacyData() {
        CompoundTag tag = validRegionTag();
        tag.remove("label");

        Region decoded = RegionNbtCodec.readRegion(tag);

        assertEquals("Spawn", decoded.label());
        assertEquals(0xFFFFAA00, decoded.labelColor().value());
    }

    @Test
    void readRegionDefaultsMissingLabelColorToWhiteForLegacyData() {
        CompoundTag tag = validRegionTag();
        tag.remove("labelColor");

        Region decoded = RegionNbtCodec.readRegion(tag);

        assertEquals("Spawn Label", decoded.label());
        assertEquals(0xFFFFFFFF, decoded.labelColor().value());
    }

    @Test
    void readRegionRejectsMissingPointsList() {
        CompoundTag tag = validRegionTag();
        tag.remove("points");

        assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(tag));
    }

    @Test
    void readRegionRejectsPointWithoutCoordinates() {
        CompoundTag tag = validRegionTag();
        ListTag points = new ListTag();
        CompoundTag point = new CompoundTag();
        point.putInt("x", 0);
        points.add(point);
        tag.put("points", points);

        assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(tag));
    }

    @Test
    void readRegionRejectsMissingRequiredScalars() {
        CompoundTag missingColor = validRegionTag();
        missingColor.remove("color");
        CompoundTag missingCreatedAt = validRegionTag();
        missingCreatedAt.remove("createdAt");
        CompoundTag missingUpdatedAt = validRegionTag();
        missingUpdatedAt.remove("updatedAt");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(missingColor)),
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(missingCreatedAt)),
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(missingUpdatedAt))
        );
    }

    @Test
    void readRegionRejectsStringsThatCannotBeSynced() {
        CompoundTag longId = validRegionTag();
        longId.putString("id", "a".repeat(RegionLimits.MAX_ID_LENGTH + 1));
        CompoundTag longName = validRegionTag();
        longName.putString("name", "a".repeat(RegionLimits.MAX_NAME_LENGTH + 1));
        CompoundTag longDimension = validRegionTag();
        longDimension.putString("dimension", "a".repeat(RegionLimits.MAX_DIMENSION_LENGTH + 1));
        CompoundTag longLabel = validRegionTag();
        longLabel.putString("label", "a".repeat(RegionLimits.MAX_LABEL_LENGTH + 1));
        CompoundTag longCategory = validRegionTag();
        longCategory.putString("category", "a".repeat(RegionLimits.MAX_CATEGORY_LENGTH + 1));
        CompoundTag longIcon = validRegionTag();
        longIcon.putString("iconName", "a".repeat(RegionLimits.MAX_ICON_NAME_LENGTH + 1));

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(longId)),
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(longName)),
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(longDimension)),
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(longLabel)),
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(longCategory)),
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(longIcon))
        );
    }

    @Test
    void readRegionRejectsCjkStringsThatExceedUtf8SyncLimit() {
        CompoundTag tag = validRegionTag();
        tag.putString("label", "界".repeat(RegionLimits.MAX_LABEL_LENGTH));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RegionNbtCodec.readRegion(tag)
        );

        assertTrue(exception.getMessage().contains("UTF-8 bytes"));
    }

    private static CompoundTag validRegionTag() {
        return RegionNbtCodec.writeRegion(new Region(
                new RegionId("spawn"),
                "Spawn",
                "minecraft:overworld",
                new ArgbColor(0x8800FF00),
                "Spawn Label",
                new ArgbColor(0xFFFFAA00),
                "town",
                "home",
                List.of(new RegionPoint(0, 0), new RegionPoint(16, 0), new RegionPoint(16, 16)),
                100L,
                200L
        ));
    }
}
