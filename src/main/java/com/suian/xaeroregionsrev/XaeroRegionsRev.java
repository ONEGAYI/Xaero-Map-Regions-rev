package com.suian.xaeroregionsrev;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(XaeroRegionsRev.MOD_ID)
public final class XaeroRegionsRev {
    public static final String MOD_ID = "xaeroregionsrev";
    public static final Logger LOGGER = LogUtils.getLogger();

    public XaeroRegionsRev() {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Xaero Map Regions Rev loaded.");
    }
}
