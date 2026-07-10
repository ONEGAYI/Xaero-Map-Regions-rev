package com.suian.xaeroregionsrev.region;

import com.suian.xaeroregionsrev.region.nbt.CompoundTagNbtCompound;
import com.suian.xaeroregionsrev.region.nbt.CompoundTagNbtFactory;
import com.suian.xaeroregionsrev.region.nbt.NbtCompound;
import com.suian.xaeroregionsrev.region.nbt.RegionNbtCodec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegionNbtCodecTest {
    private static final CompoundTagNbtFactory FACTORY = new CompoundTagNbtFactory();

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

        NbtCompound tag = RegionNbtCodec.writeRegion(FACTORY, original);
        var decoded = RegionNbtCodec.readRegion(tag);

        assertEquals(original, decoded);
    }

    @Test
    void writeRegionIncludesLabelAndLabelColor() {
        NbtCompound tag = validRegionTag();

        assertEquals("Spawn Label", tag.getString("label"));
        assertEquals(0xFFFFAA00, tag.getInt("labelColor"));
    }

    @Test
    void readRegionDefaultsMissingLabelToNameForLegacyData() {
        NbtCompound tag = validRegionTag();
        tag.remove("label");

        Region decoded = RegionNbtCodec.readRegion(tag);

        assertEquals("Spawn", decoded.label());
        assertEquals(0xFFFFAA00, decoded.labelColor().value());
    }

    @Test
    void readRegionDefaultsMissingLabelColorToWhiteForLegacyData() {
        NbtCompound tag = validRegionTag();
        tag.remove("labelColor");

        Region decoded = RegionNbtCodec.readRegion(tag);

        assertEquals("Spawn Label", decoded.label());
        assertEquals(0xFFFFFFFF, decoded.labelColor().value());
    }

    @Test
    void readRegionRejectsMissingPointsList() {
        NbtCompound tag = validRegionTag();
        tag.remove("points");

        assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(tag));
    }

    @Test
    void readRegionRejectsPointWithoutCoordinates() {
        NbtCompound tag = validRegionTag();
        var points = FACTORY.createList();
        var point = FACTORY.createCompound();
        point.putInt("x", 0);
        points.add(point);
        tag.put("points", points);

        assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(tag));
    }

    @Test
    void readRegionRejectsMissingRequiredScalars() {
        NbtCompound missingColor = validRegionTag();
        missingColor.remove("color");
        NbtCompound missingCreatedAt = validRegionTag();
        missingCreatedAt.remove("createdAt");
        NbtCompound missingUpdatedAt = validRegionTag();
        missingUpdatedAt.remove("updatedAt");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(missingColor)),
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(missingCreatedAt)),
                () -> assertThrows(IllegalArgumentException.class, () -> RegionNbtCodec.readRegion(missingUpdatedAt))
        );
    }

    @Test
    void readRegionRejectsStringsThatCannotBeSynced() {
        NbtCompound longId = validRegionTag();
        longId.putString("id", "a".repeat(RegionLimits.MAX_ID_LENGTH + 1));
        NbtCompound longName = validRegionTag();
        longName.putString("name", "a".repeat(RegionLimits.MAX_NAME_LENGTH + 1));
        NbtCompound longDimension = validRegionTag();
        longDimension.putString("dimension", "a".repeat(RegionLimits.MAX_DIMENSION_LENGTH + 1));
        NbtCompound longLabel = validRegionTag();
        longLabel.putString("label", "a".repeat(RegionLimits.MAX_LABEL_LENGTH + 1));
        NbtCompound longCategory = validRegionTag();
        longCategory.putString("category", "a".repeat(RegionLimits.MAX_CATEGORY_LENGTH + 1));
        NbtCompound longIcon = validRegionTag();
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
        NbtCompound tag = validRegionTag();
        tag.putString("label", "界".repeat(RegionLimits.MAX_LABEL_LENGTH));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RegionNbtCodec.readRegion(tag)
        );

        assertTrue(exception.getMessage().contains("UTF-8 bytes"));
    }

    private static NbtCompound validRegionTag() {
        return RegionNbtCodec.writeRegion(FACTORY, new Region(
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
