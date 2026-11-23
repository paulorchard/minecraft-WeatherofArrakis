package com.paulorchard.islandcraft.weatherofarrakis;

import com.paulorchard.islandcraft.weatherofarrakis.core.Params;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config (per world, TOML). Every number of the design is here. */
public final class WeatherConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // ---- general ----
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS = B
            .comment("Dimensions where the weather runs. Everywhere else the mod is inert (no state, no sync, start refuses).")
            .defineListAllowEmpty("dimensions", List.of("minecraft:overworld", "islandcraft_dunesofarrakis:arrakis"), () -> "minecraft:overworld", o -> o instanceof String);
    public static final ModConfigSpec.ConfigValue<String> WIND_FROM = B
            .comment("Side the wind comes from: west, east, north or south.")
            .define("wind_from", "west");

    // ---- sandstorms (Part 2) ----
    public static final ModConfigSpec.BooleanValue SANDSTORM_ENABLED = B.comment("Random sandstorm scheduler (old, prompt 01). OFF since prompt 03: sandstorms now come from vanilla rain through /sandstorm start.").define("sandstorm_enabled", false);
    public static final ModConfigSpec.DoubleValue SAND_GAP_MIN = B.comment("Gap between sandstorms, real minutes (server running), minimum.").defineInRange("sandstorm_gap_minutes_min", 15.0, 0.0, 100000.0);
    public static final ModConfigSpec.DoubleValue SAND_GAP_MAX = B.comment("Gap between sandstorms, real minutes, maximum.").defineInRange("sandstorm_gap_minutes_max", 45.0, 0.0, 100000.0);
    public static final ModConfigSpec.DoubleValue SAND_DUR_MIN = B.comment("Sandstorm length, real minutes, minimum.").defineInRange("sandstorm_duration_minutes_min", 3.0, 0.05, 100000.0);
    public static final ModConfigSpec.DoubleValue SAND_DUR_MAX = B.comment("Sandstorm length, real minutes, maximum.").defineInRange("sandstorm_duration_minutes_max", 8.0, 0.05, 100000.0);
    public static final ModConfigSpec.IntValue SAND_RAMP = B.comment("Seconds a sandstorm takes to fade in and to fade out.").defineInRange("sandstorm_ramp_seconds", 30, 0, 3600);

    // ---- Coriolis cycle (Part 3) ----
    public static final ModConfigSpec.BooleanValue CORIOLIS_ENABLED = B.comment("Coriolis storms: rare, scheduled, deadly.").define("coriolis_enabled", true);
    public static final ModConfigSpec.DoubleValue GAP_DAYS_MIN = B.comment("Game days between storms (uniform, not whole days), minimum.").defineInRange("coriolis_gap_days_min", 3.0, 0.01, 100000.0);
    public static final ModConfigSpec.DoubleValue GAP_DAYS_MAX = B.comment("Game days between storms, maximum.").defineInRange("coriolis_gap_days_max", 6.0, 0.01, 100000.0);
    public static final ModConfigSpec.IntValue APPROACH_SECONDS = B.comment("Approach length in real seconds (staged sky and chat warnings).").defineInRange("coriolis_approach_seconds", 300, 1, 36000);
    public static final ModConfigSpec.IntValue STORM_SECONDS_MIN = B.comment("Storm length, real seconds, minimum.").defineInRange("coriolis_storm_seconds_min", 40, 1, 36000);
    public static final ModConfigSpec.IntValue STORM_SECONDS_MAX = B.comment("Storm length, real seconds, maximum.").defineInRange("coriolis_storm_seconds_max", 120, 1, 36000);
    public static final ModConfigSpec.IntValue CLEARING_SECONDS = B.comment("Clearing length in real seconds (the look fades back).").defineInRange("coriolis_clearing_seconds", 30, 1, 3600);
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> WARNING_SECONDS = B
            .comment("Chat warnings at these seconds before the storm. Entries at or above the approach length are folded into the opening message.")
            .defineListAllowEmpty("warning_seconds", List.of(300, 240, 180, 120, 60, 30), () -> 60, o -> o instanceof Integer);
    public static final ModConfigSpec.DoubleValue STAGE_BLEND_FRACTION = B.comment("The client blends between approach stages over this fraction of a stage's length.").defineInRange("stage_blend_fraction", 0.9, 0.0, 1.0);

    // ---- exposure (Part 5) ----
    public static final ModConfigSpec.BooleanValue EXPOSURE_ENABLED = B.comment("Gear loss, health loss and bones during a storm.").define("exposure_enabled", true);
    public static final ModConfigSpec.IntValue DEEP_COVER_SKY_LIGHT = B.comment("Sky light at the head at or below this counts as sheltered.").defineInRange("deep_cover_sky_light", 2, 0, 15);
    public static final ModConfigSpec.IntValue ROOF_BLOCKS = B.comment("A solid block within this many blocks above the head counts as a roof.").defineInRange("roof_blocks", 3, 1, 16);
    public static final ModConfigSpec.IntValue WINDBREAK_BLOCKS = B.comment("A solid block within this many blocks upwind counts as a windbreak (with a roof).").defineInRange("windbreak_blocks", 2, 1, 16);
    public static final ModConfigSpec.IntValue WINDBREAK_UNDER_ROOF = B.comment("The windbreak search carries on up to this many blocks upwind while the roof continues.").defineInRange("windbreak_under_roof_blocks", 16, 1, 32);
    public static final ModConfigSpec.IntValue EXPOSURE_GRACE = B.comment("Exposed seconds that only warn the player before damage starts.").defineInRange("exposure_grace_checks", 2, 0, 600);
    public static final ModConfigSpec.IntValue DURABILITY_LOSS = B.comment("Durability lost per second by armour and the held item once exposed; an item at 0 is torn away.").defineInRange("durability_loss_per_second", 4, 0, 10000);
    public static final ModConfigSpec.DoubleValue HEALTH_LOSS_ARMOURED = B.comment("Health lost per second while any armour remains.").defineInRange("health_loss_armoured", 2.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue HEALTH_LOSS_STRIPPED = B.comment("Health lost per second once no armour remains.").defineInRange("health_loss_stripped", 10.0, 0.0, 1000.0);
    public static final ModConfigSpec.ConfigValue<String> BONE_MARKER_STYLE = B.comment("Marker left where a player dies in a storm: skull (a skeleton skull on the floor, random angle), bone_block (the old marker; bone blocks already in a world stay), none (nothing, only a log line).").define("bone_marker_style", "skull", o -> o instanceof String v && (v.equals("skull") || v.equals("bone_block") || v.equals("none")));

    // ---- lightning and shake (Part 4) ----
    public static final ModConfigSpec.BooleanValue LIGHTNING_ENABLED = B.comment("Lightning during the storm (visual; no fire, no block damage, no conversions).").define("lightning_enabled", true);
    public static final ModConfigSpec.DoubleValue LIGHTNING_INTERVAL_MIN = B.comment("Seconds between strikes, minimum.").defineInRange("lightning_interval_seconds_min", 2.0, 0.1, 600.0);
    public static final ModConfigSpec.DoubleValue LIGHTNING_INTERVAL_MAX = B.comment("Seconds between strikes, maximum.").defineInRange("lightning_interval_seconds_max", 6.0, 0.1, 600.0);
    public static final ModConfigSpec.IntValue LIGHTNING_MIN_DIST = B.comment("A strike lands at least this far from the chosen player.").defineInRange("lightning_distance_min", 12, 0, 256);
    public static final ModConfigSpec.IntValue LIGHTNING_MAX_DIST = B.comment("A strike lands at most this far from the chosen player.").defineInRange("lightning_distance_max", 60, 1, 256);
    public static final ModConfigSpec.DoubleValue LIGHTNING_RADIUS = B.comment("Exposed players within this many blocks of a strike are hurt. 0 turns the damage off.").defineInRange("lightning_damage_radius", 3.0, 0.0, 64.0);
    public static final ModConfigSpec.DoubleValue LIGHTNING_DAMAGE = B.comment("Damage of a strike to an exposed player in range.").defineInRange("lightning_damage", 25.0, 0.0, 1000.0);
    public static final ModConfigSpec.BooleanValue SHAKE_START = B.comment("Strong shake of about 2 s at storm start.").define("shake_at_storm_start", true);
    public static final ModConfigSpec.BooleanValue SHAKE_TREMBLE = B.comment("Faint continuous tremble while exposed in the storm.").define("shake_tremble_enabled", true);
    public static final ModConfigSpec.BooleanValue STORM_FRONT = B.comment("Experimental wall of sand on the horizon during the approach. Off until the owner has seen it.").define("storm_front", false);

    // ---- Major storm and reset (Part 6) ----
    public static final ModConfigSpec.IntValue MAJOR_STORM_SECONDS = B.comment("Length of a Major storm's rage before everyone is disconnected.").defineInRange("major_storm_seconds", 120, 1, 36000);
    public static final ModConfigSpec.IntValue MAJOR_RESET_WINDOW = B.comment("Seconds of reset time after the disconnect.").defineInRange("major_reset_window_seconds", 180, 1, 36000);
    public static final ModConfigSpec.IntValue MAJOR_LOGIN_REOPEN = B.comment("Logins reopen this many seconds after the disconnect if no restart came.").defineInRange("major_login_reopen_seconds", 600, 1, 86400);
    public static final ModConfigSpec.BooleanValue MAJOR_DISCONNECTS = B.comment("false keeps players online after a Major storm (test runs).").define("major_storm_disconnects", true);
    public static final ModConfigSpec.IntValue RESET_AFTER_MAJORS = B.comment("An altered chunk is reset after this many Major storms without a visit.").defineInRange("reset_after_major_storms", 3, 1, 1000);
    public static final ModConfigSpec.IntValue CHUNK_VISIT_SECONDS = B.comment("How often altered chunks near players are marked as visited.").defineInRange("chunk_visit_seconds", 5, 1, 3600);
    public static final ModConfigSpec.IntValue CHUNK_VISIT_RADIUS = B.comment("Visit radius in chunks. 0 = the server's simulation distance.").defineInRange("chunk_visit_radius_chunks", 0, 0, 64);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CLAIM_PROVIDERS = B
            .comment("Claim protection providers that are switched on (ids: ftbchunks, openpartiesandclaims). A provider whose mod is absent does nothing.")
            .defineListAllowEmpty("claim_providers", List.of("ftbchunks", "openpartiesandclaims"), () -> "ftbchunks", o -> o instanceof String);

    public static final ModConfigSpec SPEC = B.build();

    private WeatherConfig() {}

    public static boolean isQualifying(Level level) {
        Identifier id = level.dimension().identifier();
        return DIMENSIONS.get().contains(id.toString());
    }

    public static Params params() {
        int[] warn = WARNING_SECONDS.get().stream().mapToInt(Integer::intValue).toArray();
        return new Params(SANDSTORM_ENABLED.get(),
                (int) (SAND_GAP_MIN.get() * 1200), (int) (Math.max(SAND_GAP_MIN.get(), SAND_GAP_MAX.get()) * 1200),
                (int) (SAND_DUR_MIN.get() * 1200), (int) (Math.max(SAND_DUR_MIN.get(), SAND_DUR_MAX.get()) * 1200), SAND_RAMP.get() * 20,
                CORIOLIS_ENABLED.get(), GAP_DAYS_MIN.get(), Math.max(GAP_DAYS_MIN.get(), GAP_DAYS_MAX.get()),
                APPROACH_SECONDS.get() * 20, STORM_SECONDS_MIN.get() * 20, Math.max(STORM_SECONDS_MIN.get(), STORM_SECONDS_MAX.get()) * 20,
                CLEARING_SECONDS.get() * 20, warn);
    }

    /** Direction the wind blows towards, as a unit (x, z) vector: wind_from west means it blows east (+x). */
    public static float[] windVector() {
        return switch (WIND_FROM.get().toLowerCase(java.util.Locale.ROOT)) {
            case "east" -> new float[]{-1, 0};
            case "north" -> new float[]{0, 1};
            case "south" -> new float[]{0, -1};
            default -> new float[]{1, 0};
        };
    }
}
