package com.paulorchard.islandcraft.weatherofarrakis.client;

import com.paulorchard.islandcraft.weatherofarrakis.net.WeatherPayload;

/**
 * What the client was told, and the look it blends to. Free of client-only classes so the network handler can reference it anywhere.
 * Values are fractions "from a clear day (0) to the storm palette (1)", except fogBlocks (distance) and sun (1 = full sunlight).
 */
public final class ClientWeather {
    /** One look. */
    public record Look(float sky, float horizon, float sun, float fogBlocks, float particles, float overlay, float wind) {
        public static final float NO_FOG = 400f;
        public static final Look CLEAR = new Look(0, 0, 1, NO_FOG, 0, 0, 0);

        static Look lerp(Look a, Look b, float t) {
            return new Look(l(a.sky, b.sky, t), l(a.horizon, b.horizon, t), l(a.sun, b.sun, t), l(a.fogBlocks, b.fogBlocks, t),
                    l(a.particles, b.particles, t), l(a.overlay, b.overlay, t), l(a.wind, b.wind, t));
        }

        private static float l(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }

    /** Hytale's calibration for the six approach stages, adapted. Index 0 = stage 1 (5:00) .. 5 = stage 6 (0:30). */
    private static final Look[] STAGES = {
            new Look(0.05f, 0.35f, 1.00f, 150f, 0.00f, 0.00f, 0.10f),
            new Look(0.25f, 0.55f, 1.00f, 120f, 0.00f, 0.00f, 0.25f),
            new Look(0.50f, 0.75f, 0.95f, 90f, 0.00f, 0.00f, 0.40f),
            new Look(0.75f, 0.90f, 0.90f, 60f, 0.00f, 0.00f, 0.55f),
            new Look(1.00f, 1.00f, 0.75f, 38f, 0.35f, 0.00f, 0.70f),
            new Look(1.00f, 1.00f, 0.55f, 28f, 0.55f, 0.40f, 0.85f),
    };
    public static final Look STORM = new Look(1f, 1f, 0.20f, 20f, 1f, 1f, 1f);
    private static final Look SANDSTORM = new Look(0.6f, 0.8f, 0.75f, 55f, 0.45f, 0f, 0.55f);

    private static Look from = Look.CLEAR;
    private static Look to = Look.CLEAR;
    private static long startMs;
    private static long durMs;
    private static boolean storm;
    private static boolean major;
    private static float windX = 1f, windZ = 0f;
    private static int kind;
    private static boolean tremble, front;

    private ClientWeather() {}

    public static void apply(WeatherPayload p) {
        Look now = current();
        Look target = switch (p.kind()) {
            case WeatherPayload.SANDSTORM -> SANDSTORM;
            case WeatherPayload.APPROACH -> STAGES[Math.max(1, Math.min(6, p.stage())) - 1];
            case WeatherPayload.STORM -> STORM;
            default -> Look.CLEAR;
        };
        from = now;
        to = target;
        startMs = net.minecraft.util.Util.getMillis();
        durMs = (long) (p.blendSeconds() * 1000);
        kind = p.kind();
        storm = p.kind() == WeatherPayload.STORM;
        major = p.major();
        tremble = p.tremble();
        front = p.front();
        windX = p.windX();
        windZ = p.windZ();
    }

    public static void reset() {
        from = to = Look.CLEAR;
        durMs = 0;
        kind = 0;
        storm = false;
    }

    public static Look current() {
        if (durMs <= 0) return to;
        float t = (net.minecraft.util.Util.getMillis() - startMs) / (float) durMs;
        if (t >= 1f) return to;
        t = t * t * (3 - 2 * t); // smoothstep
        return Look.lerp(from, to, t);
    }

    public static boolean isActive() {
        Look l = current();
        return l.horizon > 0.001f || l.wind > 0.001f || l.sky > 0.001f || to != Look.CLEAR;
    }

    public static boolean isStorm() { return storm; }
    public static boolean isMajor() { return major; }
    public static boolean tremble() { return tremble; }
    public static boolean front() { return front; }
    public static int kind() { return kind; }
    public static float windX() { return windX; }
    public static float windZ() { return windZ; }
}
