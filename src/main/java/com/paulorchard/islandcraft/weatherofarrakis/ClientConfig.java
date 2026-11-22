package com.paulorchard.islandcraft.weatherofarrakis;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client options (this player's own file). */
public final class ClientConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue SHAKE_ENABLED = B.comment("Turn all camera shake off with false.").define("shake_enabled", true);
    public static final ModConfigSpec.DoubleValue SHAKE_SCALE = B.comment("Strength multiplier for all shake.").defineInRange("shake_scale", 1.0, 0.0, 3.0);
    public static final ModConfigSpec.DoubleValue SHAKE_ROLL_DEGREES = B.comment("Largest camera roll in degrees at full shake. 0 = no roll (kinder to motion sickness).").defineInRange("shake_roll_degrees", 0.0, 0.0, 5.0);
    public static final ModConfigSpec SPEC = B.build();

    private ClientConfig() {}
}
