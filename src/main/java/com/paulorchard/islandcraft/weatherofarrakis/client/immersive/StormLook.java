package com.paulorchard.islandcraft.weatherofarrakis.client.immersive;

import net.minecraft.util.Mth;

/**
 * The one small interface of the sandstorm renderer (our own code, not adapted). ClientWeather sets the server's intensity; prompt 03 can drive it
 * from the Coriolis stages too. Intensity is blended over blendSeconds on the client's own clock.
 */
public final class StormLook {
    private static float from, to, windX = 1f, windZ;
    private static long startMs, durMs;

    private StormLook() {}

    /** @param intensity 0..1; wind is the unit vector the wind blows towards. */
    public static void set(float intensity, float windX, float windZ, float blendSeconds) {
        from = intensity();
        to = Mth.clamp(intensity, 0f, 1f);
        StormLook.windX = windX;
        StormLook.windZ = windZ;
        startMs = net.minecraft.util.Util.getMillis();
        durMs = (long) (Math.max(0f, blendSeconds) * 1000);
    }

    public static float intensity() {
        if (durMs <= 0) return to;
        float t = (net.minecraft.util.Util.getMillis() - startMs) / (float) durMs;
        if (t >= 1f) return to;
        t = t * t * (3 - 2 * t);
        return from + (to - from) * t;
    }

    public static float windX() { return windX; }
    public static float windZ() { return windZ; }

    public static void reset() { from = to = 0f; durMs = 0; }
}
