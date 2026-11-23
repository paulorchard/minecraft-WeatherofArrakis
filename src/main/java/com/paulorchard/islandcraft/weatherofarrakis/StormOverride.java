package com.paulorchard.islandcraft.weatherofarrakis;

/**
 * Prompt 04 (our code): the Coriolis storm's effective rain and thunder strength, written by the client every tick from the server payload and read by the copied
 * sandstorm code and by vanilla's weather attribute layers (through StormWeatherAccessMixin). 0 means "no override". Common side so the mixin can load on a server;
 * on a dedicated server nothing ever writes it. Volatile because the integrated server thread reads it too.
 */
public final class StormOverride {
    public static volatile float rain, thunder, extra;

    private StormOverride() {}

    public static float rain(float vanilla) {
        return Math.max(vanilla, rain);
    }

    public static float thunder(float vanilla) {
        return Math.max(vanilla, thunder);
    }

    public static void clear() {
        rain = thunder = extra = 0f;
    }
}
