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

    public static CreateValues createValues(String name, String fillColorText, String label, String labelColorText,
                                            List<RegionPoint> points) {
        ArgbColor fillColor = RegionColorParser.parse(fillColorText);
        ArgbColor labelColor = RegionColorParser.parse(labelColorText);
        RegionRequestValidator.ValidatedRegionCreateRequest request =
                RegionRequestValidator.validateCreate(name, fillColor, label, labelColor, points);
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

    @Override
    protected void init() {
        int formWidth = 220;
        int left = (width - formWidth) / 2;
        int top = Math.max(32, height / 2 - 78);
        nameBox = new EditBox(font, left, top + 18, formWidth, 20, Component.translatable("field.xaeroregionsrev.name"));
        labelBox = new EditBox(font, left, top + 50, formWidth, 20, Component.translatable("field.xaeroregionsrev.label"));
        fillColorBox = new EditBox(font, left, top + 82, formWidth, 20, Component.translatable("field.xaeroregionsrev.fill_color"));
        labelColorBox = new EditBox(font, left, top + 114, formWidth, 20, Component.translatable("field.xaeroregionsrev.label_color"));
        nameBox.setMaxLength(RegionLimits.MAX_NAME_LENGTH);
        labelBox.setMaxLength(RegionLimits.MAX_LABEL_LENGTH);
        fillColorBox.setMaxLength(10);
        labelColorBox.setMaxLength(10);

        if (region == null) {
            String generatedName = "region_" + System.currentTimeMillis();
            nameBox.setValue(generatedName);
            labelBox.setValue(generatedName);
            fillColorBox.setValue("#6600AAFF");
            labelColorBox.setValue("#FFFFFFFF");
        } else {
            nameBox.setValue(region.name());
            nameBox.setEditable(false);
            labelBox.setValue(region.label());
            fillColorBox.setValue(RegionColorParser.format(region.color()));
            labelColorBox.setValue(RegionColorParser.format(region.labelColor()));
            applyEditMode();
        }

        addRenderableWidget(nameBox);
        addRenderableWidget(labelBox);
        addRenderableWidget(fillColorBox);
        addRenderableWidget(labelColorBox);
        addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.save"), button -> save())
                .bounds(left, top + 146, 104, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.cancel"), button -> minecraft.setScreen(previous))
                .bounds(left + 116, top + 146, 104, 20)
                .build());
        setInitialFocus(region == null ? nameBox : labelBox);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, Math.max(16, height / 2 - 102), 0xFFFFFFFF);
        int left = nameBox.getX();
        drawFieldLabel(graphics, Component.translatable("field.xaeroregionsrev.name"), left, nameBox.getY() - 10);
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
                        nameBox.getValue(),
                        fillColorBox.getValue(),
                        labelBox.getValue(),
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

    private void drawFieldLabel(GuiGraphics graphics, Component label, int x, int y) {
        graphics.drawString(font, label, x, y, 0xFFCCCCCC, false);
    }
}
