package com.suian.xaeroregionsrev.client.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;

final class RegionTooltipRenderer {
    private static final int Z = 400;
    private static final int ROW_GAP = 2;
    private static final int FULL_BRIGHT = 0x00F000F0;

    private RegionTooltipRenderer() {
    }

    static void render(GuiGraphics graphics, Screen screen, RegionHoverTooltipLayout.Layout layout,
                       int mouseX, int mouseY) {
        if (layout.rows().isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        List<RenderedLine> lines = renderedLines(layout);
        int width = lines.stream().mapToInt(line -> font.width(line.text())).max().orElse(0);
        int height = lines.size() * font.lineHeight + Math.max(0, lines.size() - 1) * ROW_GAP;
        Vector2ic position = DefaultTooltipPositioner.INSTANCE.positionTooltip(
                screen.width, screen.height, mouseX, mouseY, width, height);

        graphics.flush();
        TooltipRenderUtil.renderTooltipBackground(graphics, position.x(), position.y(), width, height, Z);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, Z);
        int y = position.y();
        for (RenderedLine line : lines) {
            font.drawInBatch8xOutline(line.text(), position.x(), y, line.textRgb(), line.outlineArgb(),
                    graphics.pose().last().pose(), graphics.bufferSource(), FULL_BRIGHT);
            y += font.lineHeight + ROW_GAP;
        }
        graphics.flush();
        graphics.pose().popPose();
    }

    private static List<RenderedLine> renderedLines(RegionHoverTooltipLayout.Layout layout) {
        List<RenderedLine> lines = new ArrayList<>(layout.rows().size());
        for (RegionHoverTooltipLayout.Row row : layout.rows()) {
            if (row instanceof RegionHoverTooltipLayout.RegionRow regionRow) {
                lines.add(new RenderedLine(
                        Component.literal(regionRow.candidate().label()).getVisualOrderText(),
                        regionRow.textRgb(), regionRow.outlineArgb()));
            } else if (row instanceof RegionHoverTooltipLayout.OverflowRow overflowRow) {
                int rgb = 0xFFFFFF;
                lines.add(new RenderedLine(
                        Component.translatable("tooltip.xaeroregionsrev.more_regions", overflowRow.hiddenCount())
                                .getVisualOrderText(),
                        rgb, RegionHoverTooltipLayout.outlineArgbForRgb(rgb)));
            }
        }
        return List.copyOf(lines);
    }

    private record RenderedLine(FormattedCharSequence text, int textRgb, int outlineArgb) {
    }
}
