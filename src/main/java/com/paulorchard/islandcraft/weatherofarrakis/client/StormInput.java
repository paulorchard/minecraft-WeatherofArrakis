package com.paulorchard.islandcraft.weatherofarrakis.client;

import net.minecraft.client.multiplayer.ClientLevel;

/** Prompt 04 (our code): the rain and thunder strength the copied sandstorm effects read. Part 3 adds the Coriolis override here. */
public final class StormInput {
    private StormInput() {}

    public static float rain(ClientLevel level, float partialTick) {
        return level.getRainLevel(partialTick);
    }

    public static float thunder(ClientLevel level, float partialTick) {
        return level.getThunderLevel(partialTick);
    }
}
