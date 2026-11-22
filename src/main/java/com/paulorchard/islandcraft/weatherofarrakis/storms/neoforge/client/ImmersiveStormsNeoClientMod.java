// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.neoforge.client;

import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.util.ISClientTickEvents;
import com.paulorchard.islandcraft.weatherofarrakis.storms.network.ConfigSyncPacketPayload;
import com.paulorchard.islandcraft.weatherofarrakis.storms.world.AttributeMapHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public class ImmersiveStormsNeoClientMod {
    public ImmersiveStormsNeoClientMod(IEventBus modBus, ModContainer modContainer) {
        modBus.addListener(ImmersiveStormsNeoClientMod::registerParticles);
        modBus.addListener(ImmersiveStormsNeoClientMod::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(ImmersiveStormsNeoClientMod::clientLevelTick);
    }

    private static void clientLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide() && event.getLevel() instanceof ClientLevel clientLevel) {
            ISClientTickEvents.END_LEVEL.invoker().onEndTick(clientLevel);
        }
    }

    private static void registerParticles(RegisterParticleProvidersEvent event) {
        for (var pair : ClientParticleHelperImpl.REGISTRY) {
            pair.register(event);
        }
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(
                ConfigSyncPacketPayload.TYPE,
                (payload, context) -> {
                    if (context.listener() instanceof AttributeMapHolder mapHolder) {
                        mapHolder.islandcraft_weatherofarrakis$rebindAttributeMap(payload.settings());
                    }
                }
        );
    }
}