package com.paulorchard.islandcraft.weatherofarrakis.storms;

import com.paulorchard.islandcraft.weatherofarrakis.storms.client.ImmersiveStormsClient;
import com.paulorchard.islandcraft.weatherofarrakis.storms.neoforge.client.ImmersiveStormsNeoClientMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/** Our own code (not adapted): the client half of the copied entrypoints, in its own class so a dedicated server never loads it. */
public final class StormsClientBoot {
    private StormsClientBoot() {}

    /** At mod construction: only registers listeners (the copied client @Mod class). */
    public static void listeners(IEventBus modBus, ModContainer container) {
        new ImmersiveStormsNeoClientMod(modBus, container);
    }

    /** Inside the registration window, after the common init (it touches the registered particle types). */
    public static void init() {
        new ImmersiveStormsClient().onInitializeClient();
    }
}
