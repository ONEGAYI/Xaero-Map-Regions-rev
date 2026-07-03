package com.suian.xaeroregionsrev.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.suian.xaeroregionsrev.client.xaero.XaeroScreenDetector;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

public final class RegionKeyMappings {
    public static final String CATEGORY = "key.categories.xaeroregionsrev";
    private static final IKeyConflictContext XAERO_MAP_CONTEXT = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return XaeroScreenDetector.isWorldMapScreen(Minecraft.getInstance().screen);
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return this == other;
        }
    };

    public static final Lazy<KeyMapping> TOGGLE_EDIT_MODE = Lazy.of(() -> new KeyMapping(
            "key.xaeroregionsrev.toggle_edit_mode",
            XAERO_MAP_CONTEXT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    ));

    public static final Lazy<KeyMapping> OPEN_REGION_MANAGER = Lazy.of(() -> new KeyMapping(
            "key.xaeroregionsrev.open_region_manager",
            XAERO_MAP_CONTEXT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            CATEGORY
    ));

    private RegionKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_EDIT_MODE.get());
        event.register(OPEN_REGION_MANAGER.get());
    }
}
