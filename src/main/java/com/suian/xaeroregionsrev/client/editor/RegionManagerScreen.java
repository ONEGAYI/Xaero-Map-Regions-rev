package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.client.ClientRegionCache;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.RegionRefreshRequestPacket;
import com.suian.xaeroregionsrev.region.Region;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class RegionManagerScreen extends Screen {
    private final Screen previous;

    public RegionManagerScreen(Screen previous) {
        super(Component.translatable("screen.xaeroregionsrev.region_manager"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.refresh"),
                        button -> RegionNetwork.CHANNEL.sendToServer(new RegionRefreshRequestPacket()))
                .bounds(width / 2 - 112, height - 32, 104, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.done"), button -> minecraft.setScreen(previous))
                .bounds(width / 2 + 8, height - 32, 104, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFFFF);
        String dimension = Minecraft.getInstance().level == null
                ? ""
                : Minecraft.getInstance().level.dimension().location().toString();
        List<Region> regions = ClientRegionCache.regions().stream()
                .filter(region -> region.dimension().equals(dimension))
                .toList();
        int left = width / 2 - 150;
        int y = 48;
        for (Region region : regions) {
            graphics.drawString(font, region.label(), left, y, region.labelColor().value(), true);
            graphics.drawString(font, region.id().value(), left + 150, y, 0xFFAAAAAA, false);
            y += 14;
            if (y > height - 46) {
                break;
            }
        }
        if (regions.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.xaeroregionsrev.no_regions"), width / 2, 58, 0xFFAAAAAA);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }
}
