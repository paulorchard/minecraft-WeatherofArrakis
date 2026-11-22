// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.util;

import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;
import com.paulorchard.islandcraft.weatherofarrakis.storms.particle.ParticleHelper;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.YumiMods;

import java.util.ServiceLoader;

public final class ServicesHelper {
    public static final ParticleHelper PARTICLE_HELPER = load(ParticleHelper.class);

    public static <T> T load(Class<T> clazz) {
        final String loader = isFabric() ? "fabric" : "neoforge";

        final T loadedService = ServiceLoader.load(clazz)
                .stream()
                .filter(provider -> provider.type().getName().contains(loader))
                .findFirst()
                .map(ServiceLoader.Provider::get)
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));

        if (YumiMods.get().isDevelopmentEnvironment()) {
            ImmersiveStorms.LOGGER.info("Loaded {} for service {}", loadedService.getClass(), clazz);
        }

        return loadedService;
    }

    private static boolean isFabric() {
        return YumiMods.get().isModLoaded("fabricloader") && !YumiMods.get().isModLoaded("connector");
    }

    private ServicesHelper() {

    }
}