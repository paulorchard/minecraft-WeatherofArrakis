package com.paulorchard.islandcraft.weatherofarrakis.client;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import com.paulorchard.islandcraft.weatherofarrakis.net.WeatherPayload;
import com.paulorchard.islandcraft.weatherofarrakis.storms.registry.ISEnvironmentAttributes;
import net.minecraft.client.Minecraft;

/** Counters for the diagnosis log and /storm_debug. Plain statics, client thread only. */
public final class ClientDebug {
    public static int payloads;
    public static WeatherPayload last;
    public static long lastGameTime = -1;
    public static int fogEvents, fogApplied, colorEvents, colorApplied, overlayDraws, overlayDrawn, particles, particleCalls, soundsPlayed;
    public static boolean windPlaying;
    public static float outdoor, fogEnd = -1, overlayAlpha, windVolume, particlesPerSec;
    /** Prompt 03: state of the copied Immersive Storms code, read from outside (the copied files are not instrumented). */
    public static float rainGradient, thunderGradient, fogEndSeen = -1;
    public static String archetype = "?", dustParticles = "?";
    public static String note = "";

    /** Dev only (-Pstageshots=name): a screenshot name_stageN / name_storm half a second after each Coriolis blend finishes. */
    private static final String STAGE_SHOTS = System.getProperty("islandcraft.weather.stageshots");

    private static int ticks;

    private ClientDebug() {}

    public static void onPayload(WeatherPayload p, long gameTime) {
        payloads++;
        last = p;
        lastGameTime = gameTime;
        if (STAGE_SHOTS != null && (p.kind() == WeatherPayload.APPROACH || p.kind() == WeatherPayload.STORM)) {
            StormShot.request(STAGE_SHOTS + (p.kind() == WeatherPayload.STORM ? "_storm" : "_stage" + p.stage()), p.blendSeconds() + 0.5f);
        }
        WeatherOfArrakis.LOGGER.info("[storm-debug] payload received: kind={} stage={} intensity={} wind=({},{}) blend={}s major={}", p.kind(), p.stage(), p.intensity(), p.windX(), p.windZ(), p.blendSeconds(), p.major());
    }

    /** Reads the rain gradient every tick (used to decide whether a sandstorm is showing and to time the screenshot). */
    public static void sample() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        rainGradient = StormInput.rain(mc.level, 1f);
        thunderGradient = StormInput.thunder(mc.level, 1f);
    }

    private static String skyLightFactor(Minecraft mc) {
        try {
            return String.format(java.util.Locale.ROOT, "%.3f", mc.gameRenderer.getMainCamera().attributeProbe().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SKY_LIGHT_FACTOR, 1f));
        } catch (RuntimeException e) {
            return "?";
        }
    }

    private static String skyColor(Minecraft mc) {
        try {
            return String.format("%06X", mc.gameRenderer.getMainCamera().attributeProbe().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR, 1f) & 0xFFFFFF);
        } catch (RuntimeException e) {
            return "?";
        }
    }

    /** Once per second while a weather is active (called every client tick). */
    public static void tick(boolean active) {
        if (++ticks < 20) return;
        ticks = 0;
        if (active) {
            Minecraft mc = Minecraft.getInstance();
            if (net.neoforged.fml.ModList.get().isLoaded("immersive_storms")) {
                WeatherOfArrakis.LOGGER.info("[storm-debug] the real Immersive Storms is loaded (our copy is off): rain={} thunder={}", rainGradient, thunderGradient);
            } else try {
                archetype = String.valueOf(mc.gameRenderer.getMainCamera().attributeProbe().getValue(ISEnvironmentAttributes.WEATHER_ARCHETYPE, 1f));
                dustParticles = mc.particleEngine.countParticles();
            } catch (RuntimeException e) {
                archetype = "error " + e;
            }
            if (!net.neoforged.fml.ModList.get().isLoaded("immersive_storms")) WeatherOfArrakis.LOGGER.info("[storm-debug] copied Immersive Storms: rain={} thunder={} archetype={} fogEnd(after mixin)={} particleEngine={} weatherSoundsPlayed(total)={} | ours: payloads={} renderFog={} outdoor={}",
                    rainGradient, thunderGradient, archetype, fogEndSeen, dustParticles, soundsPlayed, payloads, fogEvents, outdoor);
            WeatherOfArrakis.LOGGER.info("[storm-debug] dust: speedSetting={} thunderMult={} factorNow={} density={} size={} friction={} lifetimeScale={} | {}",
                    com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.PARTICLE_SPEED.get(), com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.PARTICLE_SPEED_THUNDER.get(),
                    mc.level == null ? 0 : StormSpeed.factor(mc.level), com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.PARTICLE_DENSITY.get(), com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.PARTICLE_SIZE.get(),
                    com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.PARTICLE_FRICTION.get(), com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.PARTICLE_LIFETIME_SCALE.get(), StormSpeed.report());
            var eff = ClientWeather.current();
            WeatherOfArrakis.LOGGER.info("[storm-debug] coriolis: kind={} stage={} effRain={} effThunder={} extra={} skyLightFactor={} skyColor={} | no old fog/overlay/particles/wind code runs", ClientWeather.kind(), ClientWeather.stage(), eff.rain(), eff.thunder(), eff.extra(), skyLightFactor(mc), skyColor(mc));
            WeatherOfArrakis.LOGGER.info("[storm-debug] {} gustsPlayed={}", StormWindBed.report(), com.paulorchard.islandcraft.weatherofarrakis.storms.client.world.SandstormSounds.gustsPlayed);
        }
        particlesPerSec = particles;
        particles = 0;
        fogEvents = fogApplied = colorEvents = colorApplied = overlayDraws = overlayDrawn = 0;
    }
}
