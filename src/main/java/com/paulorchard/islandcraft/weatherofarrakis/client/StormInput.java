package com.paulorchard.islandcraft.weatherofarrakis.client;

import net.minecraft.world.level.Level;

/** Prompt 04 (our code): the rain and thunder strength the copied sandstorm effects read. Part 3 adds the Coriolis override here. */
public final class StormInput {
    private StormInput() {}

    public static float rain(Level level, float partialTick) {
        return com.paulorchard.islandcraft.weatherofarrakis.StormOverride.rain(level.getRainLevel(partialTick));
    }

    public static float thunder(Level level, float partialTick) {
        return com.paulorchard.islandcraft.weatherofarrakis.StormOverride.thunder(level.getThunderLevel(partialTick));
    }
}
