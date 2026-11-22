package com.paulorchard.islandcraft.weatherofarrakis.storms.shim;

import java.nio.file.Path;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

/** Stand-in for dev.yumi.mc.core.api.YumiMods (not a dependency here). Our own code, not adapted from upstream. */
public final class YumiMods {
    private static final YumiMods INSTANCE = new YumiMods();

    public static YumiMods get() {
        return INSTANCE;
    }

    public boolean isModLoaded(String id) {
        ModList list = ModList.get();
        return list != null && list.isLoaded(id);
    }

    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
