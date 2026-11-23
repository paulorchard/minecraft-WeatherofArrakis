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

    public static final ModConfigSpec SPEC;

    static {
        B.pop();
        SPEC = B.build();
    }

    private StormsClientConfig() {}

    private static ModConfigSpec.DoubleValue d(String name, double def, double min, double max, String comment) {
        ModConfigSpec.DoubleValue v = B.comment(comment).defineInRange(name, def, min, max);
        TUNABLE.put(name, v);
        return v;
    }
}
