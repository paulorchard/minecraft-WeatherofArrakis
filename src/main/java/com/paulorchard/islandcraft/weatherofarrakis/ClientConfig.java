package com.paulorchard.islandcraft.weatherofarrakis;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client options (this player's own file). */
public final class ClientConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue SHAKE_ENABLED = B.comment("Turn all camera shake off with false.").define("shake_enabled", true);
    public static final ModConfigSpec.DoubleValue SHAKE_SCALE = B.comment("Strength multiplier for all shake.").defineInRange("shake_scale", 1.0, 0.0, 3.0);
    public static final ModConfigSpec.DoubleValue SHAKE_ROLL_DEGREES = B.comment("Largest camera roll in degrees at full shake. 0 = no roll (kinder to motion sickness).").defineInRange("shake_roll_degrees", 0.0, 0.0, 5.0);
    // ---- sandstorm look (client side: the client cannot read server config) ----
    public static final ModConfigSpec.BooleanValue SANDSTORM_FOG = B.comment("Sandstorm fog (distance and colour). Most shader packs ignore fog.").define("sandstorm_fog", true);
    public static final ModConfigSpec.DoubleValue SANDSTORM_FOG_START = B.comment("Fog start in blocks at full sandstorm.").defineInRange("sandstorm_fog_start", 4.0, 0.0, 512.0);
    public static final ModConfigSpec.DoubleValue SANDSTORM_FOG_END = B.comment("Fog end in blocks at full sandstorm.").defineInRange("sandstorm_fog_end", 55.0, 8.0, 512.0);
    public static final ModConfigSpec.ConfigValue<String> SANDSTORM_FOG_COLOUR = B.comment("Sand colour for fog and particles, RRGGBB hex.").define("sandstorm_fog_colour", "9E5720");
    public static final ModConfigSpec.DoubleValue SANDSTORM_PARTICLE_SCALE = B.comment("Blowing-sand density multiplier (0 = none).").defineInRange("sandstorm_particle_scale", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.IntValue SANDSTORM_PARTICLE_RADIUS = B.comment("Blocks around the camera that blowing sand is spawned in.").defineInRange("sandstorm_particle_radius", 24, 0, 64);
    public static final ModConfigSpec.BooleanValue SANDSTORM_BOOST_WITH_SHADERS = B.comment("Double the particle density while an Iris shader pack is active (fog is usually ignored there).").define("sandstorm_particles_boost_with_shaders", true);
    public static final ModConfigSpec.DoubleValue SANDSTORM_WIND_VOLUME = B.comment("Sandstorm wind volume multiplier.").defineInRange("sandstorm_wind_volume", 1.0, 0.0, 2.0);
    public static final ModConfigSpec.DoubleValue SANDSTORM_OVERLAY = B.comment("Brown screen tint at full sandstorm, 0..0.6. Default 0 (off): not judged yet.").defineInRange("sandstorm_overlay", 0.0, 0.0, 0.6);
    public static final ModConfigSpec.BooleanValue YIELD_TO_IMMERSIVE_STORMS = B.comment("If the real Immersive Storms mod is loaded, do not draw our sandstorm look.").define("yield_to_immersive_storms", true);
    public static final ModConfigSpec.BooleanValue SANDSTORM_BIOME_FILTER = B.comment("Only show the sandstorm look in desert, badlands and savanna biomes (the server decides when; this is a client filter).").define("sandstorm_biome_filter", false);
    public static final ModConfigSpec SPEC = B.build();

    /** Parsed sand colour; falls back to the default on a bad string. */
    public static int sandColour() {
        try {
            return Integer.parseInt(SANDSTORM_FOG_COLOUR.get().replace("#", "").trim(), 16) & 0xFFFFFF;
        } catch (RuntimeException e) {
            return 0x9E5720;
        }
    }

    private ClientConfig() {}
}
