package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.CreateRegionRequestPacket;
import com.suian.xaeroregionsrev.network.payload.UpdateRegionStyleRequestPacket;
import com.suian.xaeroregionsrev.region.ArgbColor;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionColorParser;
import com.suian.xaeroregionsrev.region.RegionLimits;
import com.suian.xaeroregionsrev.region.RegionPoint;
import com.suian.xaeroregionsrev.region.RegionRequestValidator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class RegionStyleEditScreen extends Screen {
    private static final int FORM_WIDTH = 220;
    private static final int COLOR_BOX_WIDTH = 178;
    private static final int COLOR_PICKER_BUTTON_WIDTH = 36;
    private static final int COLOR_PICKER_BUTTON_GAP = 6;
    private final Screen previous;
    private final Region region;
    private final RegionContextMenu.Command editCommand;
    private final List<RegionPoint> draftPoints;
    private final Runnable afterSave;
    private EditBox nameBox;
    private EditBox labelBox;
    private EditBox fillColorBox;
    private EditBox labelColorBox;
    private Component errorMessage;
    private FormText formTextOverride;

    private RegionStyleEditScreen(Screen previous, Region region, RegionContextMenu.Command editCommand,
                                  List<RegionPoint> draftPoints, Runnable afterSave) {
        super(Component.translatable(region == null
                ? "screen.xaeroregionsrev.create_region"
                : "screen.xaeroregionsrev.edit_region"));
        this.previous = previous;
        this.region = region;
        this.editCommand = editCommand;
        this.draftPoints = List.copyOf(draftPoints);
        this.afterSave = afterSave;
    }

    public static RegionStyleEditScreen create(Screen previous, List<RegionPoint> draftPoints, Runnable afterSave) {
        return new RegionStyleEditScreen(previous, null, null, draftPoints, afterSave);
    }

    public static RegionStyleEditScreen edit(Screen previous, Region region) {
        return edit(previous, region, RegionContextMenu.Command.EDIT_LABEL_TEXT);
    }

    public static RegionStyleEditScreen edit(Screen previous, Region region, RegionContextMenu.Command command) {
        return new RegionStyleEditScreen(previous, region, command, List.of(), () -> {
        });
    }

    public record StyleValues(ArgbColor fillColor, String label, ArgbColor labelColor) {
    }

    public record CreateValues(String name, ArgbColor fillColor, String label, ArgbColor labelColor,
                                List<RegionPoint> points) {
        public CreateValues {
            points = List.copyOf(points);
        }
    }

    public enum ColorTarget {
        FILL,
        LABEL
    }

    public record FormText(String label, String fillColorText, String labelColorText) {
    }

    public static CreateValues createValues(String label, String fillColorText, String labelColorText,
                                            List<RegionPoint> points) {
        ArgbColor fillColor = RegionColorParser.parse(fillColorText);
        ArgbColor labelColor = RegionColorParser.parse(labelColorText);
        String generatedName = "region_" + System.currentTimeMillis();
        RegionRequestValidator.ValidatedRegionCreateRequest request =
                RegionRequestValidator.validateCreate(generatedName, fillColor, label, labelColor, points);
        return new CreateValues(request.name(), request.fillColor(), request.label(), request.labelColor(), request.points());
    }

    public static StyleValues updateValues(Region region, RegionContextMenu.Command command, String fillColorText,
                                            String labelText, String labelColorText) {
        ArgbColor fillColor = command == RegionContextMenu.Command.EDIT_FILL_COLOR
                ? RegionColorParser.parse(fillColorText)
                : region.color();
        String label = command == RegionContextMenu.Command.EDIT_LABEL_TEXT ? labelText.trim() : region.label();
        ArgbColor labelColor = command == RegionContextMenu.Command.EDIT_LABEL_COLOR
                ? RegionColorParser.parse(labelColorText)
                : region.labelColor();
        RegionRequestValidator.ValidatedRegionStyleRequest request =
                RegionRequestValidator.validateStyle(fillColor, label, labelColor);
        return new StyleValues(request.fillColor(), request.label(), request.labelColor());
    }

    public static FormText formTextAfterPicker(ColorTarget target, FormText current, ArgbColor color) {
        String formattedColor = RegionColorParser.format(color);
        return switch (target) {
            case FILL -> new FormText(current.label(), formattedColor, current.labelColorText());
            case LABEL -> new FormText(current.label(), current.fillColorText(), formattedColor);
        };
    }

    @Override
    protected void init() {
        int left = (width - FORM_WIDTH) / 2;
        int top = Math.max(32, height / 2 - (region == null ? 62 : 78));
        int row = region == null ? 0 : 32;
        if (region != null) {
            nameBox = new EditBox(font, left, top + 18, FORM_WIDTH, 20, Component.translatable("field.xaeroregionsrev.name"));
            nameBox.setMaxLength(RegionLimits.MAX_NAME_LENGTH);
        }
        labelBox = new EditBox(font, left, top + 18 + row, FORM_WIDTH, 20, Component.translatable("field.xaeroregionsrev.label"));
        fillColorBox = new EditBox(font, left, top + 50 + row, COLOR_BOX_WIDTH, 20, Component.translatable("field.xaeroregionsrev.fill_color"));
        labelColorBox = new EditBox(font, left, top + 82 + row, COLOR_BOX_WIDTH, 20, Component.translatable("field.xaeroregionsrev.label_color"));
        labelBox.setMaxLength(RegionLimits.MAX_LABEL_LENGTH);
        fillColorBox.setMaxLength(10);
        labelColorBox.setMaxLength(10);

        if (region == null) {
            FormText formText = formTextOrDefault(new FormText("region_" + System.currentTimeMillis(),
                    "#6600AAFF", "#FFFFFFFF"));
            labelBox.setValue(formText.label());
            fillColorBox.setValue(formText.fillColorText());
            labelColorBox.setValue(formText.labelColorText());
        } else {
            nameBox.setValue(region.name());
            nameBox.setEditable(false);
            FormText formText = formTextOrDefault(new FormText(region.label(),
                    RegionColorParser.format(region.color()), RegionColorParser.format(region.labelColor())));
            labelBox.setValue(formText.label());
            fillColorBox.setValue(formText.fillColorText());
            labelColorBox.setValue(formText.labelColorText());
            applyEditMode();
        }

        if (nameBox != null) {
            addRenderableWidget(nameBox);
        }
        addRenderableWidget(labelBox);
        addRenderableWidget(fillColorBox);
        addRenderableWidget(labelColorBox);
        addRenderableWidget(Button.builder(Component.literal("PAL"),
                        button -> openColorPicker(ColorTarget.FILL,
                                Component.translatable("field.xaeroregionsrev.fill_color")))
                .bounds(colorPickerButtonX(left), fillColorBox.getY(), COLOR_PICKER_BUTTON_WIDTH, 20)
                .build()).active = region == null || editCommand == RegionContextMenu.Command.EDIT_FILL_COLOR;
        addRenderableWidget(Button.builder(Component.literal("PAL"),
                        button -> openColorPicker(ColorTarget.LABEL,
                                Component.translatable("field.xaeroregionsrev.label_color")))
                .bounds(colorPickerButtonX(left), labelColorBox.getY(), COLOR_PICKER_BUTTON_WIDTH, 20)
                .build()).active = region == null || editCommand == RegionContextMenu.Command.EDIT_LABEL_COLOR;
        addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.save"), button -> save())
                .bounds(left, top + 114 + row, 104, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.cancel"), button -> minecraft.setScreen(previous))
                .bounds(left + 116, top + 114 + row, 104, 20)
                .build());
        setInitialFocus(labelBox);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, Math.max(16, height / 2 - 102), 0xFFFFFFFF);
        int left = labelBox.getX();
        if (nameBox != null) {
            drawFieldLabel(graphics, Component.translatable("field.xaeroregionsrev.name"), left, nameBox.getY() - 10);
        }
        drawFieldLabel(graphics, Component.translatable("field.xaeroregionsrev.label"), left, labelBox.getY() - 10);
        drawFieldLabel(graphics, Component.translatable("field.xaeroregionsrev.fill_color"), left, fillColorBox.getY() - 10);
        drawFieldLabel(graphics, Component.translatable("field.xaeroregionsrev.label_color"), left, labelColorBox.getY() - 10);
        if (errorMessage != null) {
            graphics.drawCenteredString(font, errorMessage, width / 2, labelColorBox.getY() + 26, 0xFFFF7777);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    private void save() {
        try {
            if (region == null) {
                CreateValues values = createValues(
                        labelBox.getValue(),
                        fillColorBox.getValue(),
                        labelColorBox.getValue(),
                        draftPoints
                );
                RegionNetwork.CHANNEL.sendToServer(new CreateRegionRequestPacket(
                        values.name(), values.fillColor(), values.label(), values.labelColor(), values.points()));
                afterSave.run();
            } else {
                StyleValues values = updateValues(
                        region,
                        editCommand,
                        fillColorBox.getValue(),
                        labelBox.getValue(),
                        labelColorBox.getValue()
                );
                RegionNetwork.CHANNEL.sendToServer(new UpdateRegionStyleRequestPacket(
                        region.id(), values.fillColor(), values.label(), values.labelColor()));
            }
            minecraft.setScreen(previous);
        } catch (RuntimeException exception) {
            errorMessage = Component.literal(exception.getMessage());
        }
    }

    private void applyEditMode() {
        fillColorBox.setEditable(editCommand == RegionContextMenu.Command.EDIT_FILL_COLOR);
        labelBox.setEditable(editCommand == RegionContextMenu.Command.EDIT_LABEL_TEXT);
        labelColorBox.setEditable(editCommand == RegionContextMenu.Command.EDIT_LABEL_COLOR);
    }

    private void openColorPicker(ColorTarget target, Component pickerTitle) {
        captureFormText();
        try {
            ArgbColor initialColor = RegionColorParser.parse(target == ColorTarget.FILL
                    ? formTextOverride.fillColorText()
                    : formTextOverride.labelColorText());
            minecraft.setScreen(new ColorPickerScreen(this, pickerTitle, initialColor, color -> {
                formTextOverride = formTextAfterPicker(target, formTextOverride, color);
                minecraft.setScreen(this);
            }));
        } catch (RuntimeException exception) {
            errorMessage = Component.literal(exception.getMessage());
        }
    }

    private FormText formTextOrDefault(FormText defaultText) {
        return formTextOverride == null ? defaultText : formTextOverride;
    }

    private void captureFormText() {
        formTextOverride = new FormText(labelBox.getValue(), fillColorBox.getValue(), labelColorBox.getValue());
    }

    private int colorPickerButtonX(int formLeft) {
        return formLeft + COLOR_BOX_WIDTH + COLOR_PICKER_BUTTON_GAP;
    }

    private void drawFieldLabel(GuiGraphics graphics, Component label, int x, int y) {
        graphics.drawString(font, label, x, y, 0xFFCCCCCC, false);
    }
}
