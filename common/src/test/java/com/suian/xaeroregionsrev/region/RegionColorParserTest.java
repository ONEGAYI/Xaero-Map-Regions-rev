package com.suian.xaeroregionsrev.region;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionColorParserTest {
    @Test
    void parsesRgbInputsAsOpaqueColors() {
        assertEquals(0xFF112233, RegionColorParser.parse("#112233").value());
        assertEquals(0xFF112233, RegionColorParser.parse("112233").value());
        assertEquals(0xFF112233, RegionColorParser.parse("0x112233").value());
    }

    @Test
    void parsesArgbInputsWithoutChangingAlpha() {
        assertEquals(0x80112233, RegionColorParser.parse("#80112233").value());
        assertEquals(0x80112233, RegionColorParser.parse("80112233").value());
        assertEquals(0x80112233, RegionColorParser.parse("0x80112233").value());
    }

    @Test
    void rejectsBlankTooLongAndNonHexInputs() {
        assertThrows(IllegalArgumentException.class, () -> RegionColorParser.parse(" "));
        assertThrows(IllegalArgumentException.class, () -> RegionColorParser.parse("#123456789"));
        assertThrows(IllegalArgumentException.class, () -> RegionColorParser.parse("#1122ZZ"));
    }

    @Test
    void formatsAsEightDigitArgbHexWithHashPrefix() {
        assertEquals("#80112233", RegionColorParser.format(new ArgbColor(0x80112233)));
    }
}
