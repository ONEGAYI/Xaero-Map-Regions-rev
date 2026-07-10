package com.suian.xaeroregionsrev.client.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionContextMenuTest {
    @Test
    void menuContainsDeleteAndEditOnly() {
        assertEquals(RegionContextMenu.Command.DELETE, RegionContextMenu.commandFor(0));
        assertEquals(RegionContextMenu.Command.EDIT, RegionContextMenu.commandFor(1));
        assertEquals(RegionContextMenu.ITEM_HEIGHT * 2, RegionContextMenu.HEIGHT);
    }
}
