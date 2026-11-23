package com.paulorchard.islandcraft.weatherofarrakis;

import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Prompt 04: client settings of the copied sandstorm (file weatherofarrakis-storms-client.toml). NeoForge reloads the file when it changes on disk;
 * /storm_tune changes a value and writes the file. Every value is also listed in TUNABLE for /storm_tune.
 */
public final class StormsClientConfig {
    public static final String FILE = "weatherofarrakis-storms-client.toml";
    /** name to value, in definition order (used by /storm_tune). */
    public static final Map<String, ModConfigSpec.ConfigValue<?>> TUNABLE = new LinkedHashMap<>();
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    static {
        B.push("particles");
    }
    public static final ModConfigSpec.DoubleValue PARTICLE_SPEED = d("sandstorm_particle_speed", 6.0, 0.1, 40.0,
            "Multiplier on the original dust velocity (upstream = 1.0, which is 20 blocks/s at spawn, slowing 4 percent per tick). Scaled by storm strength.");
    public static final ModConfigSpec.DoubleValue PARTICLE_SPEED_THUNDER = d("sandstorm_particle_speed_thunder_multiplier", 2.0, 1.0, 10.0,
            "Extra speed factor at full thunder (lerped by the thunder gradient).");
    public static final ModConfigSpec.DoubleValue PARTICLE_DENSITY = d("sandstorm_particle_density", 6.0, 0.1, 10.0, "Multiplier for how many grains spawn (upstream 1.0). Default 6.0 because at speed 6 a grain is inside the 20 block radius for 1/8 of the time, so 1.0 looks empty.");
    public static final ModConfigSpec.DoubleValue PARTICLE_SIZE = d("sandstorm_particle_size", 1.0, 0.01, 4.0, "Multiplier for the grain size (upstream default 1.0).");
    public static final ModConfigSpec.DoubleValue PARTICLE_LIFETIME_SCALE = d("sandstorm_particle_lifetime_scale", 0.3, 0.05, 4.0,
            "Multiplier for how long a grain lives. Upstream 1.0 (5 to 20 s). Fast grains leave the 20 block radius in under a second, so the default 0.3 stops spending thousands of grains outside it.");
    public static final ModConfigSpec.DoubleValue PARTICLE_FRICTION = d("sandstorm_particle_friction", 0.96, 0.80, 1.0,
            "Velocity kept per tick (upstream 0.96: grains slow to a crawl after 2 s). 1.0 = constant wind speed.");

    static {
        B.pop();
        B.push("wind");
    }
    public static final ModConfigSpec.BooleanValue WIND_BED_ENABLED = b("wind_bed_enabled", true, "Continuous wind loop that follows the storm strength.");
    public static final ModConfigSpec.ConfigValue<String> WIND_BED_SOUND = s("wind_bed_sound", "islandcraft_weatherofarrakis:weather.bed_roar", "Sound event id of the main wind loop. Try /storm_sound <id> first. Alternatives: islandcraft_weatherofarrakis:weather.bed_howl, minecraft:item.elytra.flying, minecraft:ambient.warped_forest.loop.");
    public static final ModConfigSpec.DoubleValue WIND_BED_VOLUME = d("wind_bed_volume", 1.0, 0.0, 2.0, "Volume of the wind loop at full storm (the game caps a sound at 1.0 after the sound sliders).");
    public static final ModConfigSpec.DoubleValue WIND_BED_PITCH = d("wind_bed_pitch", 1.0, 0.5, 2.0, "Pitch of the wind loop (lower = deeper). It also falls a little in thunder and indoors.");
    public static final ModConfigSpec.ConfigValue<String> WIND_HISS_SOUND = s("wind_hiss_sound", "islandcraft_weatherofarrakis:weather.bed_hiss", "Second layer (blown sand hiss). Empty = no second layer.");
    public static final ModConfigSpec.DoubleValue WIND_HISS_VOLUME = d("wind_hiss_volume", 0.45, 0.0, 2.0, "Volume of the second layer at full storm.");
    public static final ModConfigSpec.DoubleValue WIND_INDOOR_VOLUME = d("wind_indoor_volume", 0.12, 0.0, 1.0, "Share of the wind volume that stays indoors and in caves (the pitch also drops).");
    public static final ModConfigSpec.BooleanValue WIND_GUST_ENABLED = b("wind_gust_enabled", true, "One-shot gusts around the player (the copied Immersive Storms gust code).");
    public static final ModConfigSpec.ConfigValue<String> WIND_GUST_SOUND = s("wind_gust_sound", "islandcraft_weatherofarrakis:weather.gust", "Sound event id of one gust. Empty = the biome sound of the copied code (the Pixabay strong wind files).");
    public static final ModConfigSpec.DoubleValue WIND_GUST_RATE = d("wind_gust_rate", 0.8, 0.0, 10.0, "Gusts per second at full storm; it falls with the storm strength (a fifth at the weakest).");
    public static final ModConfigSpec.DoubleValue WIND_GUST_VOLUME = d("wind_gust_volume", 1.0, 0.0, 2.0, "Volume of a gust at full storm.");
    public static final ModConfigSpec.DoubleValue WIND_GUST_PITCH = d("wind_gust_pitch", 0.9, 0.5, 2.0, "Pitch of a gust (each gust varies by about 15 percent).");

    static {
        B.pop();
        B.push("coriolis");
    }
    public static final ModConfigSpec.DoubleValue CORIOLIS_RAIN_1 = d("coriolis_rain_1", 0.10, 0.0, 1.0, "Rain-equivalent strength of approach stage 1 (the copied sandstorm: 1.0 = a full sandstorm).");
    public static final ModConfigSpec.DoubleValue CORIOLIS_RAIN_2 = d("coriolis_rain_2", 0.25, 0.0, 1.0, "Stage 2.");
    public static final ModConfigSpec.DoubleValue CORIOLIS_RAIN_3 = d("coriolis_rain_3", 0.40, 0.0, 1.0, "Stage 3.");
    public static final ModConfigSpec.DoubleValue CORIOLIS_RAIN_4 = d("coriolis_rain_4", 0.60, 0.0, 1.0, "Stage 4.");
    public static final ModConfigSpec.DoubleValue CORIOLIS_RAIN_5 = d("coriolis_rain_5", 0.80, 0.0, 1.0, "Stage 5.");
    public static final ModConfigSpec.DoubleValue CORIOLIS_RAIN_6 = d("coriolis_rain_6", 1.00, 0.0, 1.0, "Stage 6.");
    public static final ModConfigSpec.DoubleValue CORIOLIS_THUNDER_5 = d("coriolis_thunder_5", 0.3, 0.0, 1.0, "Thunder-equivalent strength of stage 5 (thicker fog, darker, faster dust, deeper wind). Stages 1 to 4 have none.");
    public static final ModConfigSpec.DoubleValue CORIOLIS_THUNDER_6 = d("coriolis_thunder_6", 0.7, 0.0, 1.0, "Thunder-equivalent strength of stage 6.");
    public static final ModConfigSpec.DoubleValue CORIOLIS_STORM_FOG = d("coriolis_storm_fog_multiplier", 0.4, 0.05, 1.0, "Multiplies the thunder fog distance in the storm itself (0.4 gives about 22 blocks).");
    public static final ModConfigSpec.DoubleValue CORIOLIS_STORM_DENSITY = d("coriolis_storm_density_multiplier", 2.0, 1.0, 5.0, "Dust density factor in the storm itself.");
    public static final ModConfigSpec.DoubleValue CORIOLIS_STORM_SPEED = d("coriolis_storm_speed_multiplier", 1.5, 1.0, 5.0, "Dust speed factor in the storm itself.");
    public static final ModConfigSpec.DoubleValue CORIOLIS_STORM_WARM = d("coriolis_storm_warm_cast", 0.35, 0.0, 1.0, "How far the fog colour is pushed toward a warm red-brown in the storm itself.");
    public static final ModConfigSpec.BooleanValue CORIOLIS_LOOK_ENABLED = b("coriolis_look_enabled", true, "false = the Coriolis storm uses no sandstorm effects at all (for comparing with plain gameplay).");

    public static final ModConfigSpec SPEC;

    static {
        B.pop();
        SPEC = B.build();
    }

    private StormsClientConfig() {}

    /** Fog distance factor of the Coriolis storm (1.0 outside it); read by the copied StormFogModifier. */
    public static float coriolisFogFactor() {
        float extra = StormOverride.extra;
        return extra <= 0f ? 1f : (float) (1.0 + (CORIOLIS_STORM_FOG.get() - 1.0) * extra);
    }

    private static ModConfigSpec.BooleanValue b(String name, boolean def, String comment) {
        ModConfigSpec.BooleanValue v = B.comment(comment).define(name, def);
        TUNABLE.put(name, v);
        return v;
    }

    private static ModConfigSpec.ConfigValue<String> s(String name, String def, String comment) {
        ModConfigSpec.ConfigValue<String> v = B.comment(comment).define(name, def);
        TUNABLE.put(name, v);
        return v;
    }

    private static ModConfigSpec.DoubleValue d(String name, double def, double min, double max, String comment) {
        ModConfigSpec.DoubleValue v = B.comment(comment).defineInRange(name, def, min, max);
        TUNABLE.put(name, v);
        return v;
    }
}
