package com.paulorchard.islandcraft.weatherofarrakis;

import com.mojang.logging.LogUtils;
import com.paulorchard.islandcraft.weatherofarrakis.net.WeatherPayload;
import com.paulorchard.islandcraft.weatherofarrakis.server.ServerEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

/** IslandCraft - Weather of Arrakis. The server owns the weather state; clients only render what they are told. */
@Mod(WeatherOfArrakis.MOD_ID)
public class WeatherOfArrakis {
    public static final String MOD_ID = "islandcraft_weatherofarrakis";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static boolean stormsInitDone;

    public WeatherOfArrakis(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, WeatherConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, com.paulorchard.islandcraft.weatherofarrakis.ClientConfig.SPEC);
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, WeatherOfArrakis::registerPayloads);
        modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class, e -> e.enqueueWork(com.paulorchard.islandcraft.weatherofarrakis.claims.ClaimProviders::init));
        NeoForge.EVENT_BUS.register(ServerEvents.class);
        // Prompt 03: the copied Immersive Storms entrypoints (the real ones are Yumi entrypoints and two @Mod classes upstream).
        if (net.neoforged.fml.ModList.get().isLoaded("immersive_storms")) {
            LOGGER.info("The real Immersive Storms mod is loaded: our copy of its sandstorm registers nothing (the two cannot run together).");
        } else {
            boolean client = net.neoforged.fml.loading.FMLEnvironment.getDist().isClient();
            if (client) com.paulorchard.islandcraft.weatherofarrakis.storms.StormsClientBoot.listeners(modEventBus, container);
            // Upstream registers its particle types, sounds and environment attributes from the Yumi init entrypoint; here the built-in registries are
            // already frozen at mod construction, so the same init runs inside the registration window (first RegisterEvent).
            modEventBus.addListener(net.neoforged.neoforge.registries.RegisterEvent.class, e -> {
                if (stormsInitDone) return;
                stormsInitDone = true;
                new com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms().onInitialize();
                // its constructor reaches the registered attributes too (ConfigSyncPacketPayload -> AdvancedSettings), so it is built here, after the init
                new com.paulorchard.islandcraft.weatherofarrakis.storms.neoforge.ImmersiveStormsNeo(modEventBus);
                if (client) com.paulorchard.islandcraft.weatherofarrakis.storms.StormsClientBoot.init();
            });
        }
        LOGGER.info("IslandCraft - Weather of Arrakis loaded");
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(WeatherPayload.TYPE, WeatherPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.paulorchard.islandcraft.weatherofarrakis.client.ClientWeather.apply(payload)))
                .playToClient(com.paulorchard.islandcraft.weatherofarrakis.net.ShakePayload.TYPE, com.paulorchard.islandcraft.weatherofarrakis.net.ShakePayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.paulorchard.islandcraft.weatherofarrakis.client.ShakeClient.add(payload.strength(), payload.seconds())));
    }
}
