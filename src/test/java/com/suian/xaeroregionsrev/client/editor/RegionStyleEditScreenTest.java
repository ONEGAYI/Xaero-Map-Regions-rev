package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionStyleEditScreenTest {
    @Test
    void fillColorCommandPreservesLabelAndLabelColor() {
        RegionStyleEditScreen.StyleValues values = RegionStyleEditScreen.updateValues(
                region(),
                RegionContextMenu.Command.EDIT_FILL_COLOR,
                "#AA112233",
                "Changed Label",
                "#FF000000"
        );

        assertEquals(new ArgbColor(0xAA112233), values.fillColor());
        assertEquals("Old Label", values.label());
        assertEquals(new ArgbColor(0xFFFFFFFF), values.labelColor());
    }

    @Test
    void labelTextCommandPreservesColors() {
        RegionStyleEditScreen.StyleValues values = RegionStyleEditScreen.updateValues(
                region(),
                RegionContextMenu.Command.EDIT_LABEL_TEXT,
                "#AA112233",
                "Changed Label",
                "#FF000000"
        );

        assertEquals(new ArgbColor(0x6600FF00), values.fillColor());
        assertEquals("Changed Label", values.label());
        assertEquals(new ArgbColor(0xFFFFFFFF), values.labelColor());
    }

    @Test
    void labelColorCommandPreservesFillAndLabel() {
        RegionStyleEditScreen.StyleValues values = RegionStyleEditScreen.updateValues(
                region(),
                RegionContextMenu.Command.EDIT_LABEL_COLOR,
                "#AA112233",
                "Changed Label",
                "#FF000000"
        );

        assertEquals(new ArgbColor(0x6600FF00), values.fillColor());
        assertEquals("Old Label", values.label());
        assertEquals(new ArgbColor(0xFF000000), values.labelColor());
    }

    @Test
    void createValuesRejectInvalidDraftBeforeSending() {
        assertThrows(IllegalArgumentException.class, () -> RegionStyleEditScreen.createValues(
                "Spawn",
                "#6600FF00",
                "Spawn Label",
                "#FFFFFFFF",
                List.of(new RegionPoint(0, 0), new RegionPoint(10, 0))
        ));
    }

    private static Region region() {
        return new Region(
                new RegionId("spawn"),
                "Spawn",
                "minecraft:overworld",
                new ArgbColor(0x6600FF00),
                "Old Label",
                new ArgbColor(0xFFFFFFFF),
                "default",
                "default",
                List.of(new RegionPoint(0, 0), new RegionPoint(10, 0), new RegionPoint(10, 10)),
                1L,
                2L
        );
    }
}
