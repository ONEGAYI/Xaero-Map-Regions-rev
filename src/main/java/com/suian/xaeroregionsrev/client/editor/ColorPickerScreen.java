package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.client.ClientColorHistoryCache;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.ColorHistoryUpdateRequestPacket;
import com.suian.xaeroregionsrev.region.RegionColorParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class ColorPickerScreen extends Screen {
    private static final int CHANNEL_ROW_HEIGHT = 26;
    private static final int SWATCH_SIZE = 18;
    private static final int SWATCH_GAP = 3;
    private static final ClientFavoriteColorStore FAVORITE_COLOR_STORE = ClientFavoriteColorStore.createDefault();

    private final Screen previous;
    private final Consumer<ArgbColor> onSave;
    private ColorPickerModel.Channels channels;
    private EditBox redBox;
    private EditBox greenBox;
    private EditBox blueBox;
    private EditBox alphaBox;
    private ChannelSlider redSlider;
    private ChannelSlider greenSlider;
    private ChannelSlider blueSlider;
    private ChannelSlider alphaSlider;
    private boolean syncingControls;
    private final List<Swatch> swatches = new ArrayList<>();

    public ColorPickerScreen(Screen previous, Component title, ArgbColor initialColor, Consumer<ArgbColor> onSave) {
        super(title);
        this.previous = previous;
        this.channels = ColorPickerModel.Channels.from(initialColor);
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        ColorPickerModel.Layout layout = layout();
        int top = layout.top() + 44;
        redBox = channelBox(layout.inputX(), top, ColorPickerModel.Channel.RED);
        greenBox = channelBox(layout.inputX(), top + CHANNEL_ROW_HEIGHT, ColorPickerModel.Channel.GREEN);
        blueBox = channelBox(layout.inputX(), top + CHANNEL_ROW_HEIGHT * 2, ColorPickerModel.Channel.BLUE);
        alphaBox = channelBox(layout.inputX(), top + CHANNEL_ROW_HEIGHT * 3, ColorPickerModel.Channel.ALPHA);
        redSlider = channelSlider(layout.sliderX(), top, layout.sliderWidth(), ColorPickerModel.Channel.RED);
        greenSlider = channelSlider(layout.sliderX(), top + CHANNEL_ROW_HEIGHT, layout.sliderWidth(),
                ColorPickerModel.Channel.GREEN);
        blueSlider = channelSlider(layout.sliderX(), top + CHANNEL_ROW_HEIGHT * 2, layout.sliderWidth(),
                ColorPickerModel.Channel.BLUE);
        alphaSlider = channelSlider(layout.sliderX(), top + CHANNEL_ROW_HEIGHT * 3, layout.sliderWidth(),
                ColorPickerModel.Channel.ALPHA);
        addRenderableWidget(redBox);
        addRenderableWidget(greenBox);
        addRenderableWidget(blueBox);
        addRenderableWidget(alphaBox);
        addRenderableWidget(redSlider);
        addRenderableWidget(greenSlider);
        addRenderableWidget(blueSlider);
        addRenderableWidget(alphaSlider);

        ColorPickerModel.PaletteLayout palette = paletteLayout(layout);
        int buttonY = palette.buttonY();
        addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.save"), button -> save())
                .bounds(layout.left() + layout.width() - 232, buttonY, 68, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.favorite_color"),
                        button -> rememberFavorite())
                .bounds(layout.left() + layout.width() - 156, buttonY, 68, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.cancel"),
                        button -> minecraft.setScreen(previous))
                .bounds(layout.left() + layout.width() - 80, buttonY, 68, 20)
                .build());
        syncControls();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        ColorPickerModel.Layout layout = layout();
        renderPanel(graphics);
        graphics.drawCenteredString(font, title, width / 2, layout.top() + 14, 0xFFFFFFFF);
        renderColorPlate(graphics);
        renderChannelLabels(graphics);
        swatches.clear();
        ColorPickerModel.PaletteLayout palette = paletteLayout(layout);
        renderSwatches(graphics, Component.translatable("field.xaeroregionsrev.recent_colors"), ClientColorHistoryCache.colors(),
                layout.controlsLeft(), palette.recentY(), palette.rows(), false);
        renderSwatches(graphics, Component.translatable("field.xaeroregionsrev.favorite_colors"), FAVORITE_COLOR_STORE.colors(),
                layout.controlsLeft(), palette.favoriteY(), palette.rows(), true);
        renderCurrentColor(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (handleColorPlateClick(mouseX, mouseY)) {
                return true;
            }
            for (Swatch swatch : swatches) {
                if (swatch.removable() && swatch.closeContains(mouseX, mouseY)) {
                    FAVORITE_COLOR_STORE.release(swatch.color());
                    return true;
                }
                if (swatch.contains(mouseX, mouseY)) {
                    channels = ColorPickerModel.Channels.from(swatch.color());
                    syncControls();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    private EditBox channelBox(int x, int y, ColorPickerModel.Channel channel) {
        EditBox box = new EditBox(font, x, y, 50, 20, Component.literal(channel.name()));
        box.setMaxLength(3);
        box.setResponder(value -> applyChannelText(channel, value));
        return box;
    }

    private ChannelSlider channelSlider(int x, int y, int width, ColorPickerModel.Channel channel) {
        return new ChannelSlider(x, y, width, 20, channel);
    }

    private void applyChannelText(ColorPickerModel.Channel channel, String text) {
        if (syncingControls || text.isBlank()) {
            return;
        }
        try {
            channels = channels.with(channel, Integer.parseInt(text.trim()));
            syncSliders();
        } catch (NumberFormatException ignored) {
            // Keep the user's partial input; the previous valid channel value remains active.
        }
    }

    private boolean handleColorPlateClick(double mouseX, double mouseY) {
        ColorPickerModel.Layout layout = layout();
        int diameter = layout.plateDiameter();
        int left = layout.plateLeft();
        int top = layout.plateTop();
        double radius = diameter / 2.0D;
        double dx = mouseX - left - radius;
        double dy = mouseY - top - radius;
        return ColorPickerModel.colorAtWheel(dx / radius, dy / radius, channels.alpha())
                .map(color -> {
                    channels = ColorPickerModel.Channels.from(color);
                    syncControls();
                    return true;
                })
                .orElse(false);
    }

    private void save() {
        ArgbColor color = channels.toColor();
        RegionNetwork.CHANNEL.sendToServer(new ColorHistoryUpdateRequestPacket(color));
        onSave.accept(color);
    }

    private void rememberFavorite() {
        FAVORITE_COLOR_STORE.remember(channels.toColor());
    }

    private void syncControls() {
        syncingControls = true;
        redBox.setValue(Integer.toString(channels.red()));
        greenBox.setValue(Integer.toString(channels.green()));
        blueBox.setValue(Integer.toString(channels.blue()));
        alphaBox.setValue(Integer.toString(channels.alpha()));
        syncingControls = false;
        syncSliders();
    }

    private void syncSliders() {
        redSlider.syncFromChannels();
        greenSlider.syncFromChannels();
        blueSlider.syncFromChannels();
        alphaSlider.syncFromChannels();
    }

    private void renderPanel(GuiGraphics graphics) {
        ColorPickerModel.Layout layout = layout();
        int left = layout.left();
        int top = layout.top();
        int right = left + layout.width();
        int bottom = top + layout.height();
        graphics.fill(left, top, right, bottom, 0xDD111827);
        graphics.fill(left, top, right, top + 1, 0xFF9CA3AF);
        graphics.fill(left, bottom - 1, right, bottom, 0xFF030712);
        graphics.fill(left, top, left + 1, bottom, 0xFF9CA3AF);
        graphics.fill(right - 1, top, right, bottom, 0xFF030712);
    }

    private void renderColorPlate(GuiGraphics graphics) {
        ColorPickerModel.Layout layout = layout();
        int diameter = layout.plateDiameter();
        int left = layout.plateLeft();
        int top = layout.plateTop();
        float radius = diameter / 2.0F;
        for (int y = 0; y < diameter; y += 4) {
            for (int x = 0; x < diameter; x += 4) {
                float dx = x + 2.0F - radius;
                float dy = y + 2.0F - radius;
                Optional<ArgbColor> color = ColorPickerModel.colorAtWheel(dx / radius, dy / radius, 0xFF);
                if (color.isPresent()) {
                    graphics.fill(left + x, top + y, left + x + 4, top + y + 4, color.get().value());
                }
            }
        }
        ColorPickerModel.WheelPosition marker = ColorPickerModel.wheelPosition(channels);
        int markerX = Math.round((float) (left + radius + marker.x() * radius));
        int markerY = Math.round((float) (top + radius + marker.y() * radius));
        drawWheelMarker(graphics, markerX, markerY);
    }

    private void renderChannelLabels(GuiGraphics graphics) {
        ColorPickerModel.Layout layout = layout();
        int x = layout.controlsLeft();
        int y = layout.top() + 49;
        graphics.drawString(font, "R", x, y, 0xFFFF7777, false);
        graphics.drawString(font, "G", x, y + CHANNEL_ROW_HEIGHT, 0xFF7CFC8A, false);
        graphics.drawString(font, "B", x, y + CHANNEL_ROW_HEIGHT * 2, 0xFF79B8FF, false);
        graphics.drawString(font, "A", x, y + CHANNEL_ROW_HEIGHT * 3, 0xFFE5E7EB, false);
    }

    private void renderSwatches(GuiGraphics graphics, Component label, List<ArgbColor> colors, int x, int y,
                                int rows, boolean removable) {
        graphics.drawString(font, label, x, y - 12, 0xFFFFCC66, false);
        int columns = swatchColumns(layout());
        for (int i = 0; i < columns * rows; i++) {
            int row = i / columns;
            int column = i % columns;
            int swatchX = x + column * (SWATCH_SIZE + SWATCH_GAP);
            int swatchY = y + row * (SWATCH_SIZE + SWATCH_GAP);
            graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF111827);
            graphics.fill(swatchX + 1, swatchY + 1, swatchX + SWATCH_SIZE - 1, swatchY + SWATCH_SIZE - 1,
                    i < colors.size() ? colors.get(i).value() : 0xFF374151);
            if (i < colors.size()) {
                if (removable) {
                    drawCloseMark(graphics, swatchX + SWATCH_SIZE - 7, swatchY + 1);
                }
                swatches.add(new Swatch(swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, colors.get(i), removable));
            }
        }
    }

    private void renderCurrentColor(GuiGraphics graphics) {
        ColorPickerModel.Layout layout = layout();
        int x = layout.left() + 18;
        int y = Math.min(layout.plateTop() + layout.plateDiameter() + 10, layout.top() + layout.height() - 42);
        graphics.drawString(font, RegionColorParser.format(channels.toColor()), x + 38, y + 8, 0xFFE5E7EB, false);
        graphics.fill(x, y, x + 28, y + 28, 0xFF111827);
        graphics.fill(x + 2, y + 2, x + 26, y + 26, channels.toColor().value());
    }

    private ColorPickerModel.Layout layout() {
        return ColorPickerModel.layout(width, height);
    }

    private int swatchColumns(ColorPickerModel.Layout layout) {
        int availableWidth = layout.left() + layout.width() - layout.controlsLeft() - 12;
        return Math.max(4, availableWidth / (SWATCH_SIZE + SWATCH_GAP));
    }

    private ColorPickerModel.PaletteLayout paletteLayout(ColorPickerModel.Layout layout) {
        return ColorPickerModel.paletteLayout(layout, SWATCH_SIZE, SWATCH_GAP);
    }

    private void drawWheelMarker(GuiGraphics graphics, int centerX, int centerY) {
        drawCircleRing(graphics, centerX, centerY, 5, 4, 0xFF111827);
        drawCircleRing(graphics, centerX, centerY, 4, 3, 0xFFFFFFFF);
    }

    private void drawCloseMark(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 6, y + 6, 0xCC111827);
        for (int i = 0; i < 5; i++) {
            graphics.fill(x + 1 + i, y + 1 + i, x + 2 + i, y + 2 + i, 0xFFFF4444);
            graphics.fill(x + 5 - i, y + 1 + i, x + 6 - i, y + 2 + i, 0xFFFF4444);
        }
    }

    private void drawCircleRing(GuiGraphics graphics, int centerX, int centerY, int outerRadius, int innerRadius,
                                int color) {
        int outerSquared = outerRadius * outerRadius;
        int innerSquared = innerRadius * innerRadius;
        for (int y = -outerRadius; y <= outerRadius; y++) {
            for (int x = -outerRadius; x <= outerRadius; x++) {
                int distanceSquared = x * x + y * y;
                if (distanceSquared <= outerSquared && distanceSquared >= innerSquared) {
                    graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }

    private record Swatch(int x, int y, int width, int height, ArgbColor color, boolean removable) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        boolean closeContains(double mouseX, double mouseY) {
            return mouseX >= x + width - 8 && mouseX < x + width && mouseY >= y && mouseY < y + 8;
        }
    }

    private final class ChannelSlider extends AbstractSliderButton {
        private final ColorPickerModel.Channel channel;

        private ChannelSlider(int x, int y, int width, int height, ColorPickerModel.Channel channel) {
            super(x, y, width, height, Component.empty(), channels.value(channel) / 255.0D);
            this.channel = channel;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(Integer.toString(channels.value(channel))));
        }

        @Override
        protected void applyValue() {
            if (syncingControls) {
                return;
            }
            channels = channels.with(channel, (int) Math.round(value * 255.0D));
            syncControls();
        }

        private void syncFromChannels() {
            value = channels.value(channel) / 255.0D;
            updateMessage();
        }
    }
}
