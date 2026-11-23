package com.paulorchard.islandcraft.weatherofarrakis.client;

import com.paulorchard.islandcraft.weatherofarrakis.StormOverride;
import com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig;
import com.paulorchard.islandcraft.weatherofarrakis.net.WeatherPayload;

/**
 * What the client was told, and the effective strength it blends to. Prompt 04: the Coriolis stages and the storm are expressed as an effective rain and thunder
 * gradient (plus an "extra" factor for the storm itself) that the copied Immersive Storms sandstorm effects read through StormOverride; the old staged fog, overlay,
 * particles and wind of prompt 01 are gone. Free of client-only classes so the network handler can reference it anywhere.
 */
public final class ClientWeather {
    /** Effective strength: rain and thunder like vanilla gradients (0..1), extra = 0 in the approach, 1 in the storm itself. */
    public record Eff(float rain, float thunder, float extra) {
        public static final Eff CLEAR = new Eff(0, 0, 0);

        static Eff lerp(Eff a, Eff b, float t) {
            return new Eff(l(a.rain, b.rain, t), l(a.thunder, b.thunder, t), l(a.extra, b.extra, t));
        }

        private static float l(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }

    private static Eff from = Eff.CLEAR;
    private static Eff to = Eff.CLEAR;
    private static long startMs;
    private static long durMs;
    private static boolean storm;
    private static boolean major;
    private static float windX = 1f, windZ = 0f;
    private static int kind, stage;
    private static boolean tremble, front;

    private ClientWeather() {}

    /** Approach stage 1..6 to an effective strength, read from the client config when the payload arrives (so /storm_tune works live). */
    private static Eff stageEff(int stage) {
        float rain = switch (Math.max(1, Math.min(6, stage))) {
            case 1 -> StormsClientConfig.CORIOLIS_RAIN_1.get().floatValue();
            case 2 -> StormsClientConfig.CORIOLIS_RAIN_2.get().floatValue();
            case 3 -> StormsClientConfig.CORIOLIS_RAIN_3.get().floatValue();
            case 4 -> StormsClientConfig.CORIOLIS_RAIN_4.get().floatValue();
            case 5 -> StormsClientConfig.CORIOLIS_RAIN_5.get().floatValue();
            default -> StormsClientConfig.CORIOLIS_RAIN_6.get().floatValue();
        };
        float thunder = stage == 5 ? StormsClientConfig.CORIOLIS_THUNDER_5.get().floatValue() : stage >= 6 ? StormsClientConfig.CORIOLIS_THUNDER_6.get().floatValue() : 0f;
        return new Eff(rain, thunder, 0f);
    }

    public static void apply(WeatherPayload p) {
        ClientDebug.onPayload(p, net.minecraft.client.Minecraft.getInstance().level == null ? -1 : net.minecraft.client.Minecraft.getInstance().level.getGameTime());
        Eff now = current();
        Eff target = switch (p.kind()) {
            case WeatherPayload.APPROACH -> stageEff(p.stage());
            case WeatherPayload.STORM -> new Eff(1f, 1f, 1f);
            default -> Eff.CLEAR;
        };
        if (!StormsClientConfig.CORIOLIS_LOOK_ENABLED.get()) target = Eff.CLEAR;
        from = now;
        to = target;
        startMs = net.minecraft.util.Util.getMillis();
        durMs = (long) (p.blendSeconds() * 1000);
        kind = p.kind();
        stage = p.stage();
        storm = p.kind() == WeatherPayload.STORM;
        major = p.major();
        tremble = p.tremble();
        front = p.front();
        windX = p.windX();
        windZ = p.windZ();
    }

    public static void reset() {
        from = to = Eff.CLEAR;
        durMs = 0;
        kind = 0;
        stage = 0;
        storm = false;
        StormOverride.clear();
    }

    public static Eff current() {
        if (durMs <= 0) return to;
        float t = (net.minecraft.util.Util.getMillis() - startMs) / (float) durMs;
        if (t >= 1f) return to;
        t = t * t * (3 - 2 * t); // smoothstep
        return Eff.lerp(from, to, t);
    }

    /** Called every client tick: publishes the blended strength to the copied sandstorm effects. */
    public static void pushOverride() {
        Eff e = current();
        StormOverride.rain = e.rain();
        StormOverride.thunder = e.thunder();
        StormOverride.extra = e.extra();
    }

    public static boolean isActive() {
        Eff e = current();
        return e.rain > 0.001f || to != Eff.CLEAR;
    }

    public static boolean isStorm() { return storm; }
    public static boolean isMajor() { return major; }
    public static boolean tremble() { return tremble; }
    public static boolean front() { return front; }
    public static int kind() { return kind; }
    public static int stage() { return stage; }
    public static float windX() { return windX; }
    public static float windZ() { return windZ; }
}
