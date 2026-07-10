package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.ArgbColor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorPickerModelTest {
    @Test
    void channelsRoundTripArgbColor() {
        ColorPickerModel.Channels channels = ColorPickerModel.Channels.from(new ArgbColor(0x66224488));

        assertEquals(0x22, channels.red());
        assertEquals(0x44, channels.green());
        assertEquals(0x88, channels.blue());
        assertEquals(0x66, channels.alpha());
        assertEquals(new ArgbColor(0x66224488), channels.toColor());
    }

    @Test
    void channelUpdatesClampToByteRange() {
        ColorPickerModel.Channels channels = ColorPickerModel.Channels.from(new ArgbColor(0x66000000))
                .with(ColorPickerModel.Channel.RED, 300)
                .with(ColorPickerModel.Channel.GREEN, -20)
                .with(ColorPickerModel.Channel.BLUE, 17)
                .with(ColorPickerModel.Channel.ALPHA, 128);

        assertEquals(new ArgbColor(0x80FF0011), channels.toColor());
    }

    @Test
    void rememberColorMovesRecentDuplicatesToFront() {
        List<ArgbColor> remembered = ColorPickerModel.rememberColor(
                List.of(new ArgbColor(0xFF000000), new ArgbColor(0xFFFFFFFF)),
                new ArgbColor(0xFF000000),
                4
        );

        assertEquals(List.of(new ArgbColor(0xFF000000), new ArgbColor(0xFFFFFFFF)), remembered);
    }

    @Test
    void rememberColorTrimsToLimit() {
        List<ArgbColor> remembered = ColorPickerModel.rememberColor(
                List.of(new ArgbColor(0xFF111111), new ArgbColor(0xFF222222), new ArgbColor(0xFF333333)),
                new ArgbColor(0xFF444444),
                3
        );

        assertEquals(List.of(
                new ArgbColor(0xFF444444),
                new ArgbColor(0xFF111111),
                new ArgbColor(0xFF222222)
        ), remembered);
    }

    @Test
    void releaseColorRemovesFavoriteAndShiftsRemainingColorsLeft() {
        List<ArgbColor> colors = List.of(
                new ArgbColor(0xFF111111),
                new ArgbColor(0xFF222222),
                new ArgbColor(0xFF333333)
        );

        List<ArgbColor> released = ColorPickerModel.releaseColor(colors, new ArgbColor(0xFF222222));

        assertEquals(List.of(new ArgbColor(0xFF111111), new ArgbColor(0xFF333333)), released);
    }

    @Test
    void pickerLayoutStaysInsideScaledScreens() {
        ColorPickerModel.Layout layout = ColorPickerModel.layout(427, 240);

        assertTrue(layout.left() >= 8);
        assertTrue(layout.top() >= 8);
        assertTrue(layout.left() + layout.width() <= 427 - 8);
        assertTrue(layout.top() + layout.height() <= 240 - 8);
        assertTrue(layout.plateDiameter() <= layout.controlsLeft() - layout.left() - 24);
    }

    @Test
    void pickerLayoutUsesTallerPanelOnHighResolutionScreens() {
        ColorPickerModel.Layout layout = ColorPickerModel.layout(2048, 1152);

        assertTrue(layout.height() >= 400);
    }

    @Test
    void pickerPaletteRowsDoNotOverlapActionButtons() {
        ColorPickerModel.Layout layout = ColorPickerModel.layout(2048, 1152);
        ColorPickerModel.PaletteLayout palette = ColorPickerModel.paletteLayout(layout, 18, 3);
        int favoriteSwatchesBottom = palette.favoriteY() + palette.rows() * (18 + 3) - 3;

        assertTrue(palette.recentY() < palette.favoriteY());
        assertTrue(favoriteSwatchesBottom + 10 <= palette.buttonY());
    }

    @Test
    void wheelCenterIsWhiteAndRightEdgeIsRed() {
        Optional<ArgbColor> center = ColorPickerModel.colorAtWheel(0.0D, 0.0D, 128);
        Optional<ArgbColor> rightEdge = ColorPickerModel.colorAtWheel(1.0D, 0.0D, 128);

        assertEquals(Optional.of(new ArgbColor(0x80FFFFFF)), center);
        assertEquals(Optional.of(new ArgbColor(0x80FF0000)), rightEdge);
    }

    @Test
    void wheelRejectsPointsOutsideCircle() {
        assertEquals(Optional.empty(), ColorPickerModel.colorAtWheel(1.1D, 0.0D, 255));
    }
}
