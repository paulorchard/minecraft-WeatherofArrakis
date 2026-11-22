package com.paulorchard.islandcraft.weatherofarrakis.client;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import com.paulorchard.islandcraft.weatherofarrakis.net.WeatherPayload;

/** Counters for the diagnosis log and /storm_debug. Plain statics, client thread only (render and tick are the same thread). */
public final class ClientDebug {
    public static int payloads;
    public static WeatherPayload last;
    public static long lastGameTime = -1;
    public static int fogEvents, fogApplied, colorEvents, colorApplied, overlayDraws, overlayDrawn, particles, particleCalls;
    public static boolean windPlaying;
    public static float outdoor, fogEnd = -1, overlayAlpha, windVolume, particlesPerSec;
    public static String note = "";

    private static int ticks;

    private ClientDebug() {}

    public static void onPayload(WeatherPayload p, long gameTime) {
        payloads++;
        last = p;
        lastGameTime = gameTime;
        WeatherOfArrakis.LOGGER.info("[storm-debug] payload received: kind={} stage={} intensity={} wind=({},{}) blend={}s major={}", p.kind(), p.stage(), p.intensity(), p.windX(), p.windZ(), p.blendSeconds(), p.major());
    }

    /** Once per second while a weather is active (called every client tick). */
    public static void tick(boolean active) {
        if (++ticks < 20) return;
        ticks = 0;
        if (active) {
            WeatherOfArrakis.LOGGER.info("[storm-debug] payloads={} renderFog={} (applied {}, end={}) fogColor={} (applied {}) particles={}/s overlay drawn={} (alpha {}) wind playing={} vol={} outdoor={}",
                    payloads, fogEvents, fogApplied, fogEnd, colorEvents, colorApplied, particles, overlayDrawn, overlayAlpha, windPlaying, windVolume, outdoor);
        }
        particlesPerSec = particles;
        particles = 0;
        fogEvents = fogApplied = colorEvents = colorApplied = overlayDraws = overlayDrawn = 0;
    }
}
