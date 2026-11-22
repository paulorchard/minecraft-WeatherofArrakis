package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import com.paulorchard.islandcraft.weatherofarrakis.core.LevelWeather;
import com.paulorchard.islandcraft.weatherofarrakis.core.Phase;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Everything the mod saves with the world, stored once with the overworld's data. Per-dimension weather state; Part 6 adds chunk records. */
public class WeatherData extends SavedData {
    private static final Codec<Phase> PHASE = Codec.STRING.xmap(s -> {
        try {
            return Phase.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Phase.CALM;
        }
    }, Phase::name);

    private static final Codec<LevelWeather> LEVEL = RecordCodecBuilder.create(i -> i.group(
            PHASE.optionalFieldOf("phase", Phase.CALM).forGetter(w -> w.phase),
            Codec.LONG.optionalFieldOf("scheduled_at", -1L).forGetter(w -> w.scheduledAt),
            Codec.INT.optionalFieldOf("elapsed_ticks", 0).forGetter(w -> w.elapsedTicks),
            Codec.INT.optionalFieldOf("approach_ticks", 0).forGetter(w -> w.approachTicks),
            Codec.INT.optionalFieldOf("storm_ticks", 0).forGetter(w -> w.stormTicks),
            Codec.INT.optionalFieldOf("clearing_ticks", 0).forGetter(w -> w.clearingTicks),
            Codec.BOOL.optionalFieldOf("major", false).forGetter(w -> w.major),
            Codec.BOOL.optionalFieldOf("sand_active", false).forGetter(w -> w.sandActive),
            Codec.INT.optionalFieldOf("sand_ticks_left", 0).forGetter(w -> w.sandTicksLeft),
            Codec.INT.optionalFieldOf("sand_total_ticks", 0).forGetter(w -> w.sandTotalTicks),
            Codec.INT.optionalFieldOf("sand_next_ticks", -1).forGetter(w -> w.sandNextTicks)
    ).apply(i, LevelWeather::new));

    public static final Codec<WeatherData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, LEVEL).optionalFieldOf("levels", Map.of()).forGetter(d -> d.levels)
    ).apply(i, WeatherData::new));

    public static final SavedDataType<WeatherData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(WeatherOfArrakis.MOD_ID, "weather"), WeatherData::new, CODEC);

    private final Map<String, LevelWeather> levels = new HashMap<>();

    public WeatherData() {}

    private WeatherData(Map<String, LevelWeather> loaded) {
        this.levels.putAll(loaded);
        for (Map.Entry<String, LevelWeather> e : this.levels.entrySet()) {
            if (e.getValue().sanitizeAfterLoad()) {
                WeatherOfArrakis.LOGGER.warn("Saved Major storm phase found for {} at start-up; not resumed, the level is CALM", e.getKey());
            }
        }
    }

    public static WeatherData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public LevelWeather of(String dimensionId) {
        return levels.computeIfAbsent(dimensionId, k -> {
            setDirty();
            return new LevelWeather();
        });
    }
}
