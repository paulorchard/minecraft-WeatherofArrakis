// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.client;

import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;
import com.paulorchard.islandcraft.weatherofarrakis.storms.config.Updater;
import com.paulorchard.islandcraft.weatherofarrakis.storms.config.section.GeneralSettings;
import com.paulorchard.islandcraft.weatherofarrakis.storms.config.section.SandstormSettings;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.particle.BlackWaterDropParticle;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.particle.ClientParticleHelper;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.particle.DustGrainParticle;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.util.ISClientTickEvents;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.world.BiomeWindEffects;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.world.SandstormParticles;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.world.SandstormSounds;
import com.paulorchard.islandcraft.weatherofarrakis.storms.registry.ISParticleTypes;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.YumiMods;

public class ImmersiveStormsClient {
    private static boolean isDistantHorizonsLoaded = false;

    public void onInitializeClient() {
        checkDistantHorizons();
        registerConfig();

        boolean disableSandstormEffects = SandstormSettings.HANDLER.instance().isDetectParticleRain()
                && YumiMods.get().isModLoaded("particlerain");

        if (!disableSandstormEffects) {
            ISClientTickEvents.END_LEVEL.register(new SandstormParticles());
            ISClientTickEvents.END_LEVEL.register(new SandstormSounds());
        } else {
            ImmersiveStorms.LOGGER.info("Particle Rain has been detected, disabling Immersive Storms sandstorm particle and sound effects");
        }

        ISClientTickEvents.END_LEVEL.register(new BiomeWindEffects());

        ClientParticleHelper particleRegistry = ClientParticleHelper.getInstance();
        particleRegistry.registerProvider(ISParticleTypes.DUST_GRAIN, DustGrainParticle.Provider::new);
        particleRegistry.registerProvider(ISParticleTypes.BLACK_RAIN, BlackWaterDropParticle.Provider::new);
    }

    public static GeneralSettings getConfig() {
        return GeneralSettings.HANDLER.instance();
    }

    public static boolean isDistantHorizonsLoaded() {
        return isDistantHorizonsLoaded;
    }

    private static void registerConfig() {
        Updater.initialize();
        GeneralSettings.HANDLER.load();
        SandstormSettings.HANDLER.load();
    }

    private static void checkDistantHorizons() {
        isDistantHorizonsLoaded = YumiMods.get().isModLoaded("distanthorizons");
    }
}