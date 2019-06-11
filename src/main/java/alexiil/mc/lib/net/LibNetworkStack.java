package alexiil.mc.lib.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;

public class LibNetworkStack implements ModInitializer {

    public static final boolean DEBUG = Boolean.getBoolean("libnetworkstack.debug");
    public static final Logger LOGGER = LogManager.getLogger("LibNetworkStack");

    @Override
    public void onInitialize() {
        CoreMinecraftNetUtil.load();
    }
}
