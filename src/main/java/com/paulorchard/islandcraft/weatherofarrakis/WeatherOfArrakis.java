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

    public WeatherOfArrakis(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, WeatherConfig.SPEC);
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, WeatherOfArrakis::registerPayloads);
        NeoForge.EVENT_BUS.register(ServerEvents.class);
        LOGGER.info("IslandCraft - Weather of Arrakis loaded");
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(WeatherPayload.TYPE, WeatherPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.paulorchard.islandcraft.weatherofarrakis.client.ClientWeather.apply(payload)));
    }
}
