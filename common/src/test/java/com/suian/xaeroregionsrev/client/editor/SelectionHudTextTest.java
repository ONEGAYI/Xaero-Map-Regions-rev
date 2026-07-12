package com.suian.xaeroregionsrev.client.editor;

import org.junit.jupiter.api.Test;

import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

class SelectionHudTextTest {

    // 简单的等宽模拟：每个字符宽度 = 6 像素
    private static final ToIntFunction<String> CHAR_WIDTH_6 = s -> s.length() * 6;

    @Test
    void singleLayerShowsLabelOnly() {
        SelectionHudText text = SelectionHudText.of("MyRegion", 1, 1, CHAR_WIDTH_6, 160);

        assertEquals("MyRegion", text.fullText());
        assertEquals("MyRegion", text.displayText());
        assertFalse(text.truncated());
    }

    @Test
    void multipleLayersShowsIndexPrefix() {
        SelectionHudText text = SelectionHudText.of("Castle", 2, 5, CHAR_WIDTH_6, 160);

        assertEquals("2/5  Castle", text.fullText());
        assertEquals("2/5  Castle", text.displayText());
        assertFalse(text.truncated());
    }

    @Test
    void truncatesLabelWhenExceedingMaxWidth() {
        // prefix "2/5  " = 5 chars = 30px, 省略号 "..." = 3 chars = 18px
        // maxWidth 60px → label 可用宽度 = 60 - 30 - 18 = 12px = 2 chars
        String longLabel = "AVeryLongRegionName";
        SelectionHudText text = SelectionHudText.of(longLabel, 2, 5, CHAR_WIDTH_6, 60);

        assertTrue(text.truncated());
        assertTrue(text.displayText().startsWith("2/5  "));
        assertTrue(text.displayText().endsWith("..."));
        assertEquals("2/5  AVeryLongRegionName", text.fullText());
    }

    @Test
    void singleLayerTruncatesLabelWhenExceedingMaxWidth() {
        // 单层无前缀，maxWidth 30px = 5 chars, "..." = 18px → label 2 chars
        SelectionHudText text = SelectionHudText.of("ABCDEFGH", 1, 1, CHAR_WIDTH_6, 30);

        assertTrue(text.truncated());
        assertTrue(text.displayText().endsWith("..."));
        assertEquals("ABCDEFGH", text.fullText());
    }

    @Test
    void notTruncatedWhenExactlyAtMaxWidth() {
        // "2/5  AB" = 7 chars = 42px, maxWidth 42 → 刚好不截断
        SelectionHudText text = SelectionHudText.of("AB", 2, 5, CHAR_WIDTH_6, 42);

        assertFalse(text.truncated());
        assertEquals("2/5  AB", text.displayText());
    }

    @Test
    void totalZeroOrNegativeShowsLabelOnlyNoPrefix() {
        SelectionHudText t0 = SelectionHudText.of("Name", 1, 0, CHAR_WIDTH_6, 160);
        assertEquals("Name", t0.fullText());
        assertEquals("Name", t0.displayText());
        assertFalse(t0.truncated());

        SelectionHudText tNeg = SelectionHudText.of("Name", 1, -1, CHAR_WIDTH_6, 160);
        assertEquals("Name", tNeg.fullText());
        assertFalse(tNeg.truncated());
    }

    @Test
    void emptyLabelProducesEmptyDisplay() {
        SelectionHudText single = SelectionHudText.of("", 1, 1, CHAR_WIDTH_6, 160);
        assertEquals("", single.fullText());
        assertEquals("", single.displayText());
        assertFalse(single.truncated());

        SelectionHudText multi = SelectionHudText.of("", 2, 5, CHAR_WIDTH_6, 160);
        assertEquals("2/5  ", multi.fullText());
        assertFalse(multi.truncated());
    }
}
