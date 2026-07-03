package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.RegionId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class RegionContextMenu {
    public static final int ITEM_HEIGHT = 18;
    public static final int WIDTH = 118;
    public static final int HEIGHT = ITEM_HEIGHT * Command.values().length;

    private final RegionId regionId;
    private final int x;
    private final int y;

    public enum Command {
        DELETE,
        EDIT_FILL_COLOR,
        EDIT_LABEL_TEXT,
        EDIT_LABEL_COLOR
    }

    public RegionContextMenu(RegionId regionId, int x, int y) {
        this.regionId = regionId;
        this.x = x;
        this.y = y;
    }

    public RegionId regionId() {
        return regionId;
    }

    public static Command commandFor(int index) {
        Command[] commands = Command.values();
        if (index < 0 || index >= commands.length) {
            throw new IllegalArgumentException("Unknown context menu index: " + index);
        }
        return commands[index];
    }

    public Command commandAt(double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + WIDTH || mouseY < y || mouseY >= y + HEIGHT) {
            return null;
        }
        return commandFor((int) ((mouseY - y) / ITEM_HEIGHT));
    }

    public void render(GuiGraphics graphics) {
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xDD111111);
        for (Command command : Command.values()) {
            int index = command.ordinal();
            int top = y + index * ITEM_HEIGHT;
            graphics.fill(x, top, x + WIDTH, top + ITEM_HEIGHT, index % 2 == 0 ? 0x331A73E8 : 0x222C2C2C);
            graphics.drawString(
                    net.minecraft.client.Minecraft.getInstance().font,
                    label(command),
                    x + 6,
                    top + 5,
                    0xFFFFFFFF,
                    false
            );
        }
    }

    public static Component label(Command command) {
        return switch (command) {
            case DELETE -> Component.translatable("menu.xaeroregionsrev.delete");
            case EDIT_FILL_COLOR -> Component.translatable("menu.xaeroregionsrev.fill_color");
            case EDIT_LABEL_TEXT -> Component.translatable("menu.xaeroregionsrev.label_text");
            case EDIT_LABEL_COLOR -> Component.translatable("menu.xaeroregionsrev.label_color");
        };
    }
}
