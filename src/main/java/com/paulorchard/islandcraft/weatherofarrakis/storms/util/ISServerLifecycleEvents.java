// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.util;

import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.event.Event;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

public final class ISServerLifecycleEvents {
    public static final Event<Identifier, ServerStarting> SERVER_STARTING = ImmersiveStorms.EVENT_MANAGER.create(
            ServerStarting.class,
            listeners -> (server) -> {
                for (ServerStarting listener : listeners) {
                    listener.onServerStarting(server);
                }
            });

    @FunctionalInterface
    public interface ServerStarting {
        void onServerStarting(MinecraftServer server);
    }

    private ISServerLifecycleEvents() {

    }
}