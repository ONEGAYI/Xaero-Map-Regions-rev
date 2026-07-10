package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.client.ClientRegionCache;
import com.suian.xaeroregionsrev.client.xaero.MapProjectionAdapter;
import com.suian.xaeroregionsrev.network.RegionNetwork;
import com.suian.xaeroregionsrev.network.payload.DeleteRegionRequestPacket;
import com.suian.xaeroregionsrev.network.payload.RegionRefreshRequestPacket;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public final class RegionManagerScreen extends Screen {
    private static final int MAX_LIST_WIDTH = 620;
    private static final int HORIZONTAL_MARGIN = 16;
    private static final int ROW_HEIGHT = 18;
    private static final int MAX_PAGE_SIZE = 10;
    private static final int LIST_TOP = 48;
    private static final int BUTTON_COUNT = 6;
    private static final int BUTTON_WIDTH = 96;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int BOTTOM_MARGIN = 8;
    private static final int PAGE_LABEL_GAP = 16;
    private final Screen previous;
    private RegionId selectedRegionId;
    private int page;
    private Button previousPageButton;
    private Button nextPageButton;
    private Button jumpButton;
    private Button deleteButton;

    public RegionManagerScreen(Screen previous) {
        super(Component.translatable("screen.xaeroregionsrev.region_manager"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        ButtonBounds previousBounds = buttonBounds(0);
        previousPageButton = addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.previous_page"),
                        button -> {
                            page = Math.max(0, page - 1);
                            updateButtonState();
                        })
                .bounds(previousBounds.x(), previousBounds.y(), previousBounds.width(), previousBounds.height())
                .build());
        ButtonBounds nextBounds = buttonBounds(1);
        nextPageButton = addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.next_page"),
                        button -> {
                            page = Math.min(RegionManagerListModel.pageCount(currentRegions().size(), pageSize()) - 1, page + 1);
                            updateButtonState();
                        })
                .bounds(nextBounds.x(), nextBounds.y(), nextBounds.width(), nextBounds.height())
                .build());
        ButtonBounds jumpBounds = buttonBounds(2);
        jumpButton = addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.jump"),
                        button -> selectedRegion().ifPresent(this::jumpTo))
                .bounds(jumpBounds.x(), jumpBounds.y(), jumpBounds.width(), jumpBounds.height())
                .build());
        ButtonBounds deleteBounds = buttonBounds(3);
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("menu.xaeroregionsrev.delete"),
                        button -> selectedRegion().ifPresent(this::confirmDelete))
                .bounds(deleteBounds.x(), deleteBounds.y(), deleteBounds.width(), deleteBounds.height())
                .build());
        ButtonBounds refreshBounds = buttonBounds(4);
        addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.refresh"),
                        button -> {
                            RegionNetwork.sendToServer(new RegionRefreshRequestPacket());
                            updateButtonState();
                        })
                .bounds(refreshBounds.x(), refreshBounds.y(), refreshBounds.width(), refreshBounds.height())
                .build());
        ButtonBounds doneBounds = buttonBounds(5);
        addRenderableWidget(Button.builder(Component.translatable("button.xaeroregionsrev.done"), button -> minecraft.setScreen(previous))
                .bounds(doneBounds.x(), doneBounds.y(), doneBounds.width(), doneBounds.height())
                .build());
        updateButtonState();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateButtonState();
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFFFF);
        renderListText(graphics);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderListBackgrounds(graphics, mouseX, mouseY);
    }

    private void renderListBackgrounds(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Region> regions = currentRegions();
        int pageSize = pageSize();
        int listWidth = listWidth();
        page = RegionManagerListModel.clampPage(page, regions.size(), pageSize);
        int left = listLeft();
        List<Region> visibleRegions = RegionManagerListModel.pageItems(regions, page, pageSize);
        for (int index = 0; index < visibleRegions.size(); index++) {
            Region region = visibleRegions.get(index);
            int rowY = LIST_TOP + index * ROW_HEIGHT;
            boolean selected = selectedRegionId != null && selectedRegionId.equals(region.id());
            boolean hovered = rowBoundsContains(mouseX, mouseY, index);
            int fill = selected ? 0x884169E1 : hovered ? 0x552D3748 : 0x33000000;
            graphics.fill(left, rowY - 3, left + listWidth, rowY + ROW_HEIGHT - 3, fill);
        }
    }

    private void renderListText(GuiGraphics graphics) {
        List<Region> regions = currentRegions();
        int pageSize = pageSize();
        int listWidth = listWidth();
        page = RegionManagerListModel.clampPage(page, regions.size(), pageSize);
        int left = listLeft();
        int labelX = left + 8;
        int centerX = left + Math.max(listWidth / 2, listWidth - 160);
        graphics.drawString(font, Component.translatable("field.xaeroregionsrev.label"), labelX, LIST_TOP - 14, 0xFFAAAAAA, false);
        graphics.drawString(font, Component.translatable("field.xaeroregionsrev.center"), centerX, LIST_TOP - 14, 0xFFAAAAAA, false);
        List<Region> visibleRegions = RegionManagerListModel.pageItems(regions, page, pageSize);
        for (int index = 0; index < visibleRegions.size(); index++) {
            Region region = visibleRegions.get(index);
            int rowY = LIST_TOP + index * ROW_HEIGHT;
            graphics.drawString(font, region.label(), labelX, rowY, region.labelColor().value(), true);
            graphics.drawString(font, RegionManagerListModel.formatCenter(region), centerX, rowY, 0xFFE5E7EB, false);
        }
        if (regions.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.xaeroregionsrev.no_regions"), width / 2, 58, 0xFFAAAAAA);
        }
        int pageCount = RegionManagerListModel.pageCount(regions.size(), pageSize);
        graphics.drawCenteredString(font, Component.literal((page + 1) + " / " + pageCount), width / 2, pageLabelY(), 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int pageSize = pageSize();
            List<Region> visibleRegions = RegionManagerListModel.pageItems(currentRegions(), page, pageSize);
            int left = listLeft();
            Optional<Region> clicked = RegionManagerListModel.rowAt(mouseX, mouseY, left, LIST_TOP, listWidth(), ROW_HEIGHT, visibleRegions.size())
                    .stream()
                    .mapToObj(visibleRegions::get)
                    .findFirst();
            if (clicked.isPresent()) {
                selectedRegionId = clicked.get().id();
                updateButtonState();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    private int listLeft() {
        return width / 2 - listWidth() / 2;
    }

    private int listWidth() {
        return RegionManagerListModel.listWidth(width, MAX_LIST_WIDTH, HORIZONTAL_MARGIN);
    }

    private int pageSize() {
        return RegionManagerListModel.visiblePageSize(height, LIST_TOP, pageLabelY() - 8, ROW_HEIGHT, MAX_PAGE_SIZE);
    }

    private int pageLabelY() {
        return bottomControlsTop() - PAGE_LABEL_GAP;
    }

    private int bottomControlsTop() {
        int rows = RegionManagerListModel.buttonRows(width, BUTTON_COUNT, BUTTON_WIDTH, BUTTON_GAP, HORIZONTAL_MARGIN);
        return height - BOTTOM_MARGIN - rows * BUTTON_HEIGHT - (rows - 1) * BUTTON_GAP;
    }

    private ButtonBounds buttonBounds(int index) {
        int columns = RegionManagerListModel.buttonColumns(width, BUTTON_COUNT, BUTTON_WIDTH, BUTTON_GAP,
                HORIZONTAL_MARGIN);
        int row = index / columns;
        int column = index % columns;
        int buttonsInRow = Math.min(columns, BUTTON_COUNT - row * columns);
        int rowWidth = buttonsInRow * BUTTON_WIDTH + (buttonsInRow - 1) * BUTTON_GAP;
        int x = width / 2 - rowWidth / 2 + column * (BUTTON_WIDTH + BUTTON_GAP);
        int y = bottomControlsTop() + row * (BUTTON_HEIGHT + BUTTON_GAP);
        return new ButtonBounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private boolean rowBoundsContains(double mouseX, double mouseY, int visibleRow) {
        int left = listLeft();
        return mouseX >= left
                && mouseX < left + listWidth()
                && mouseY >= LIST_TOP + visibleRow * ROW_HEIGHT
                && mouseY < LIST_TOP + (visibleRow + 1) * ROW_HEIGHT;
    }

    private List<Region> currentRegions() {
        String dimension = Minecraft.getInstance().level == null
                ? ""
                : Minecraft.getInstance().level.dimension().location().toString();
        return ClientRegionCache.regions().stream()
                .filter(region -> region.dimension().equals(dimension))
                .toList();
    }

    private Optional<Region> selectedRegion() {
        if (selectedRegionId == null) {
            return Optional.empty();
        }
        return currentRegions().stream()
                .filter(region -> region.id().equals(selectedRegionId))
                .findFirst();
    }

    private void updateButtonState() {
        List<Region> regions = currentRegions();
        int pageSize = pageSize();
        page = RegionManagerListModel.clampPage(page, regions.size(), pageSize);
        boolean hasSelection = selectedRegion().isPresent();
        if (!hasSelection) {
            selectedRegionId = null;
        }
        if (previousPageButton != null) {
            previousPageButton.active = page > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.active = page < RegionManagerListModel.pageCount(regions.size(), pageSize) - 1;
        }
        if (jumpButton != null) {
            jumpButton.active = hasSelection;
        }
        if (deleteButton != null) {
            deleteButton.active = hasSelection;
        }
    }

    private void confirmDelete(Region region) {
        Minecraft.getInstance().setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                RegionNetwork.sendToServer(new DeleteRegionRequestPacket(region.id()));
                selectedRegionId = null;
            }
            Minecraft.getInstance().setScreen(this);
        }, Component.translatable("screen.xaeroregionsrev.confirm_delete"), Component.literal(region.label())));
    }

    private void jumpTo(Region region) {
        RegionPoint center = RegionManagerListModel.centerPoint(region);
        MapProjectionAdapter.shared().centerOn(previous, center);
        minecraft.setScreen(previous);
    }

    private record ButtonBounds(int x, int y, int width, int height) {
    }
}
